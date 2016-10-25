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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.GroupCapabilityServiceBuilderFactory;
import org.wildfly.clustering.server.GroupJndiNameFactory;
import org.wildfly.clustering.server.GroupRequirementBuilderProvider;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class GroupRegistryFactoryBuilderProvider extends GroupRequirementBuilderProvider<RegistryFactory<Object, Object>> {

    protected GroupRegistryFactoryBuilderProvider(GroupCapabilityServiceBuilderFactory<RegistryFactory<Object, Object>> factory) {
        super(ClusteringRequirement.REGISTRY_FACTORY, factory, GroupJndiNameFactory.REGISTRY_FACTORY);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringRequirement> registry, String group) {
        Collection<CapabilityServiceBuilder<?>> builders = super.getBuilders(registry, group);
        List<CapabilityServiceBuilder<?>> result = new ArrayList<>(builders.size() + 1);
        result.addAll(builders);
        result.add(new RegistryBuilder<>(registry.getServiceName(ClusteringRequirement.REGISTRY), support -> ClusteringRequirement.REGISTRY_FACTORY.getServiceName(support, group), support -> ClusteringRequirement.REGISTRY_ENTRY.getServiceName(support, group)));
        return result;
    }
}
