/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.SingletonServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalSingletonServiceBuilder<T> implements SingletonServiceBuilder<T>, LocalSingletonServiceContext<T> {

    private final ServiceName name;
    private final Service<T> service;
    private final ValueDependency<Group> groupDependency;

    public LocalSingletonServiceBuilder(LocalSingletonServiceBuilderContext context, ServiceName name, Service<T> service) {
        this.groupDependency = context.getGroupDependency();
        this.name = name;
        this.service = service;
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        // Quorum requirements are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy) {
        // Election policies are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> backupService(Service<T> backupService) {
        // A backup service will never run on a local singleton
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> build(ServiceTarget target) {
        SingletonService<T> service = new LocalSingletonService<>(this);
        SingletonServiceInstaller<T> installer =  new AsynchronousSingletonServiceBuilder<>(this.name, service).build(target);
        this.groupDependency.register(installer);
        return installer;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Service<T> getService() {
        return this.service;
    }

    @Override
    public Value<Group> getGroup() {
        return this.groupDependency;
    }
}
