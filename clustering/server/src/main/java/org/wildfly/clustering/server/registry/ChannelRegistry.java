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

package org.wildfly.clustering.server.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * @author Paul Ferraro
 */
public class ChannelRegistry<K, V> implements Registry<K, V>, RegistryEntryProvider<K, V> {

    private final CommandDispatcher<RegistryEntryProvider<K, V>> dispatcher;
    private final Group group;
    private final Map.Entry<K, V> entry;
    private final Runnable closeTask;

    public ChannelRegistry(ChannelRegistryConfiguration config, Map.Entry<K, V> entry, Runnable closeTask) {
        CommandDispatcherFactory factory = config.getCommandDispatcherFactory();
        this.group = factory.getGroup();
        this.dispatcher = factory.createCommandDispatcher(config.getServiceName(), this);
        this.entry = entry;
        this.closeTask = closeTask;
    }

    @Override
    public Map.Entry<K, V> getLocalEntry() {
        return this.entry;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public void addListener(Registry.Listener<K, V> listener) {
        // Listeners not supported
    }

    @Override
    public void removeListener(Registry.Listener<K, V> listener) {
        // Listeners not supported
    }

    @Override
    public Map<K, V> getEntries() {
        Map<K, V> result = new HashMap<>(this.group.getNodes().size());
        try {
            Map<Node, CommandResponse<Map.Entry<K, V>>> responses = this.dispatcher.executeOnCluster(new LocalEntryCommand<>());
            for (Map.Entry<Node, CommandResponse<Map.Entry<K, V>>> response : responses.entrySet()) {
                try {
                    Map.Entry<K, V> entry = response.getValue().get();
                    if (entry != null) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                } catch (ExecutionException e) {
                    ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
            return result;
        } catch (CommandDispatcherException e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return Collections.singletonMap(this.entry.getKey(), this.entry.getValue());
        }
    }

    @Override
    public Map.Entry<K, V> getEntry(Node node) {
        if (this.group.getLocalNode().equals(node)) return this.entry;
        try {
            return this.dispatcher.executeOnNode(new LocalEntryCommand<>(), node).get();
        } catch (ExecutionException e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return null;
        } catch (CommandDispatcherException e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return null;
        }
    }

    @Override
    public void close() {
        this.dispatcher.close();
        this.closeTask.run();
    }
}
