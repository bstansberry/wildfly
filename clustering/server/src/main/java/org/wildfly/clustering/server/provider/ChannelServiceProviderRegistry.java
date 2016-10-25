/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.server.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.clustering.provider.ServiceProviderRegistry;

/**
 * A {@link ServiceProviderRegistry} backed by a {@link CommandDispatcher}.
 * @author Paul Ferraro
 */
public class ChannelServiceProviderRegistry<T> implements ServiceProviderRegistry<T>, ChannelServiceProviderRegistryContext<T>, Group.Listener, AutoCloseable {

    private final ConcurrentMap<T, Listener> listeners = new ConcurrentHashMap<>();
    private final Group group;
    private final CommandDispatcher<ChannelServiceProviderRegistryContext<T>> dispatcher;
    private final ServiceExecutor executor = new StampedLockServiceExecutor();

    public ChannelServiceProviderRegistry(Object id, CommandDispatcherFactory factory) {
        this.group = factory.getGroup();
        this.dispatcher = factory.createCommandDispatcher(id, this);
        this.group.addListener(this);
    }

    @Override
    public Set<T> getLocalServices() {
        return this.listeners.keySet();
    }

    @Override
    public void notifyListener(T service, Set<Node> providers) {
        Listener listener = this.listeners.get(service);
        if (listener != null) {
            this.executor.execute(() -> {
                try {
                    listener.providersChanged(providers);
                } catch (Throwable e) {
                    ClusteringServerLogger.ROOT_LOGGER.serviceProviderRegistrationListenerFailed(e, this.group.getName(), providers);
                }
            });
        }
    }

    @Override
    public void membershipChanged(List<Node> previousMembers, List<Node> members, boolean merged) {
        if (members.get(0).equals(this.group.getLocalNode())) {
            this.executor.execute(() -> {
                Map<T, Set<Node>> providers = new HashMap<>();
                try {
                    Map<Node, CommandResponse<Collection<T>>> responses = this.dispatcher.executeOnCluster(new ServiceQueryCommand<>());
                    for (Map.Entry<Node, CommandResponse<Collection<T>>> entry : responses.entrySet()) {
                        Node node = entry.getKey();
                        try {
                            Collection<T> services = entry.getValue().get();
                            services.forEach(service -> {
                                Set<Node> nodes = providers.get(service);
                                if (nodes == null) {
                                    nodes = new HashSet<>();
                                    providers.put(service, nodes);
                                }
                                nodes.add(node);
                            });
                        } catch (ExecutionException e) {
                            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    }

                    for (Map.Entry<T, Set<Node>> entry : providers.entrySet()) {
                        this.dispatcher.executeOnCluster(new ServiceProviderNotificationCommand<>(entry.getKey(), entry.getValue()));
                    }
                } catch (CommandDispatcherException e) {
                    ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                }
            });
        }
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public ServiceProviderRegistration<T> register(T service) {
        return this.register(service, null);
    }

    @Override
    public ServiceProviderRegistration<T> register(T service, Listener listener) {
        if (this.listeners.putIfAbsent(service, listener) != null) {
            throw new IllegalArgumentException(service.toString());
        }
        this.notifyListeners(service);
        return new SimpleServiceProviderRegistration<>(service, this, () -> {
            this.listeners.remove(service);
            this.notifyListeners(service);
        });
    }

    private void notifyListeners(T service) {
        Set<Node> providers = this.getProviders(service);
        try {
            this.dispatcher.executeOnCluster(new ServiceProviderNotificationCommand<>(service, providers));
        } catch (CommandDispatcherException e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Set<Node> getProviders(T service) {
        Set<Node> nodes = new HashSet<>(this.group.getNodes().size());
        this.executor.execute(() -> {
            try {
                Map<Node, CommandResponse<Boolean>> responses = this.dispatcher.executeOnCluster(new ServiceLocatorCommand<>(service));
                for (Map.Entry<Node, CommandResponse<Boolean>> entry : responses.entrySet()) {
                    try {
                        if (entry.getValue().get().booleanValue()) {
                            nodes.add(entry.getKey());
                        }
                    } catch (ExecutionException e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e.getCause());
                    }
                }
            } catch (CommandDispatcherException e) {
                ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        });
        return nodes;
    }

    @Override
    public Set<T> getServices() {
        Set<T> services = new HashSet<>(this.listeners.size());
        this.executor.execute(() -> {
            try {
                Map<Node, CommandResponse<Collection<T>>> responses = this.dispatcher.executeOnCluster(new ServiceQueryCommand<>());
                for (Map.Entry<Node, CommandResponse<Collection<T>>> entry : responses.entrySet()) {
                    try {
                        services.addAll(entry.getValue().get());
                    } catch (ExecutionException e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
            } catch (CommandDispatcherException e) {
                ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        });
        return services;
    }

    @Override
    public void close() {
        this.executor.close(() -> {
            this.group.removeListener(this);
            this.dispatcher.close();
        });
    }
}
