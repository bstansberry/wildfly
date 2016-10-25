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

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * @author Paul Ferraro
 */
public class ChannelRegistryFactoryBuilder<K, V> implements CapabilityServiceBuilder<RegistryFactory<K, V>>, ChannelRegistryConfiguration {

    private final ServiceName name;
    private final String group;

    private volatile ValueDependency<CommandDispatcherFactory> dispatcherFactory;

    public ChannelRegistryFactoryBuilder(ServiceName name, String group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<RegistryFactory<K, V>> configure(CapabilityServiceSupport support) {
        this.dispatcherFactory = new InjectedValueDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.group), CommandDispatcherFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<RegistryFactory<K, V>> build(ServiceTarget target) {
        Value<RegistryFactory<K, V>> value = () -> new FunctionalRegistryFactory<>((entry, closeTask) -> new ChannelRegistry<>(this, entry, closeTask));
        ServiceBuilder<RegistryFactory<K, V>> builder = target.addService(this.name, new ValueService<>(value)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return this.dispatcherFactory.register(builder);
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.getValue();
    }
}
