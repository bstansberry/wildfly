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
package org.wildfly.clustering.spi;

import java.util.Map;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * @author Paul Ferraro
 */
public enum ClusteringDefaultRequirement implements Requirement, ServiceNameFactoryProvider {

    COMMAND_DISPATCHER_FACTORY("org.wildfly.clustering.default-command-dispatcher-factory", CommandDispatcherFactory.class),
    GROUP("org.wildfly.clustering.default-group", Group.class),
    NODE_FACTORY("org.wildfly.clustering.default-node-factory", NodeFactory.class),
    REGISTRY("org.wildfly.clustering.default-registry", Registry.class),
    REGISTRY_ENTRY("org.wildfly.clustering.default-registry-entry", Map.Entry.class),
    REGISTRY_FACTORY("org.wildfly.clustering.default-registry-factory", RegistryFactory.class),
    SERVICE_PROVIDER_REGISTRY("org.wildfly.clustering.default-service-provider-registry", ServiceProviderRegistry.class),
    SINGLETON_SERVICE_BUILDER_FACTORY("org.wildfly.clustering.default-singleton-service-builder-factory", SingletonServiceBuilderFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory = new RequirementServiceNameFactory(this);

    ClusteringDefaultRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public ServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
