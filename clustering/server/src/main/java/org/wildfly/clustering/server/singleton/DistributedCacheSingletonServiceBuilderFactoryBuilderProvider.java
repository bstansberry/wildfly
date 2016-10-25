/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.singleton;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.DistributedCacheBuilderProvider;

/**
 * Provides the requisite builders for a clustered {@link org.wildfly.clustering.singleton.SingletonServiceBuilderFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(DistributedCacheBuilderProvider.class)
public class DistributedCacheSingletonServiceBuilderFactoryBuilderProvider extends CacheSingletonServiceBuilderFactoryBuilderProvider implements DistributedCacheBuilderProvider {

    public DistributedCacheSingletonServiceBuilderFactoryBuilderProvider() {
        super((name, containerName, cacheName) -> new DistributedSingletonServiceBuilderFactoryBuilder(name, containerName, support -> ClusteringCacheRequirement.SERVICE_PROVIDER_REGISTRY.getServiceName(support, containerName, cacheName)));
    }
}
