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

package org.wildfly.clustering.server.singleton;

import java.util.Collection;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceController;
import org.wildfly.clustering.singleton.SingletonServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class SingletonServiceInstallerImpl<T> implements SingletonServiceInstaller<T> {
    private final ServiceBuilder<T> builder;
    private final SingletonService<T> service;

    public SingletonServiceInstallerImpl(ServiceBuilder<T> builder, SingletonService<T> service) {
        this.builder = builder;
        this.service = service;
    }

    @Override
    public SingletonServiceInstaller<T> addAliases(ServiceName... aliases) {
        this.builder.addAliases(aliases);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> setInitialMode(Mode mode) {
        this.builder.setInitialMode(mode);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependencies(ServiceName... dependencies) {
        this.builder.addDependencies(dependencies);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependencies(DependencyType dependencyType, ServiceName... dependencies) {
        this.builder.addDependencies(dependencyType, dependencies);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependencies(Iterable<ServiceName> dependencies) {
        this.builder.addDependencies(dependencies);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependencies(DependencyType dependencyType, Iterable<ServiceName> dependencies) {
        this.builder.addDependencies(dependencyType, dependencies);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependency(ServiceName dependency) {
        this.builder.addDependency(dependency);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency) {
        this.builder.addDependency(dependencyType, dependency);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependency(ServiceName dependency, Injector<Object> target) {
        this.builder.addDependency(dependency, target);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency, Injector<Object> target) {
        this.builder.addDependency(dependencyType, dependency, target);
        return this;
    }

    @Override
    public <I> SingletonServiceInstaller<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
        this.builder.addDependency(dependency, type, target);
        return this;
    }

    @Override
    public <I> SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency, Class<I> type, Injector<I> target) {
        this.builder.addDependency(dependencyType, dependency, type, target);
        return this;
    }

    @Override
    public <I> SingletonServiceInstaller<T> addInjection(Injector<? super I> target, I value) {
        this.builder.addInjection(target, value);
        return this;
    }

    @Override
    public <I> SingletonServiceInstaller<T> addInjectionValue(Injector<? super I> target, Value<I> value) {
        this.builder.addInjectionValue(target, value);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addInjection(Injector<? super T> target) {
        this.builder.addInjection(target);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addMonitor(StabilityMonitor monitor) {
        this.builder.addMonitor(monitor);
        return this;
    }

    @Override
    public SingletonServiceInstaller<T> addMonitors(StabilityMonitor... monitors) {
        this.builder.addMonitors(monitors);
        return this;
    }

    @Override
    public SingletonServiceController<T> install() {
        return new SingletonServiceControllerImpl<>(this.builder.install(), this.service);
    }

    @Deprecated
    @Override
    public ServiceBuilder<T> addListener(org.jboss.msc.service.ServiceListener<? super T> listener) {
        return this.builder.addListener(listener);
    }

    @Deprecated
    @Override
    public ServiceBuilder<T> addListener(@SuppressWarnings("unchecked") org.jboss.msc.service.ServiceListener<? super T>... listeners) {
        return this.builder.addListener(listeners);
    }

    @Deprecated
    @Override
    public ServiceBuilder<T> addListener(Collection<? extends org.jboss.msc.service.ServiceListener<? super T>> listeners) {
        return this.builder.addListener(listeners);
    }
}
