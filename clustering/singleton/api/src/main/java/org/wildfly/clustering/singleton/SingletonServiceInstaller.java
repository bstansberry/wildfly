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

package org.wildfly.clustering.singleton;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.Value;

/**
 * {@link SingletonServiceInstaller} for singleton services.
 * @author Paul Ferraro
 */
public interface SingletonServiceInstaller<T> extends ServiceBuilder<T> {

    @Override
    SingletonServiceInstaller<T> addAliases(ServiceName... aliases);

    @Override
    SingletonServiceInstaller<T> setInitialMode(Mode mode);

    @Override
    SingletonServiceInstaller<T> addDependencies(ServiceName... dependencies);

    @Override
    SingletonServiceInstaller<T> addDependencies(DependencyType dependencyType, ServiceName... dependencies);

    @Override
    SingletonServiceInstaller<T> addDependencies(Iterable<ServiceName> dependencies);

    @Override
    SingletonServiceInstaller<T> addDependencies(DependencyType dependencyType, Iterable<ServiceName> dependencies);

    @Override
    SingletonServiceInstaller<T> addDependency(ServiceName dependency);

    @Override
    SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency);

    @Override
    SingletonServiceInstaller<T> addDependency(ServiceName dependency, Injector<Object> target);

    @Override
    SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency, Injector<Object> target);

    @Override
    <I> SingletonServiceInstaller<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target);

    @Override
    <I> SingletonServiceInstaller<T> addDependency(DependencyType dependencyType, ServiceName dependency, Class<I> type, Injector<I> target);

    @Override
    <I> SingletonServiceInstaller<T> addInjection(Injector<? super I> target, I value);

    @Override
    <I> SingletonServiceInstaller<T> addInjectionValue(Injector<? super I> target, Value<I> value);

    @Override
    SingletonServiceInstaller<T> addInjection(Injector<? super T> target);

    @Override
    SingletonServiceInstaller<T> addMonitor(StabilityMonitor monitor);

    @Override
    SingletonServiceInstaller<T> addMonitors(StabilityMonitor... monitors);

    @Override
    SingletonServiceController<T> install();
}
