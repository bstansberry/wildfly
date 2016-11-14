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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceController;

/**
 * @author Paul Ferraro
 */
public class SingletonServiceControllerImpl<S> implements SingletonServiceController<S> {

    private final ServiceController<S> controller;
    private final SingletonService<S> service;

    public SingletonServiceControllerImpl(ServiceController<S> controller, SingletonService<S> service) {
        this.controller = controller;
        this.service = service;
    }

    @Override
    public ServiceController<?> getParent() {
        return this.controller.getParent();
    }

    @Override
    public ServiceContainer getServiceContainer() {
        return this.controller.getServiceContainer();
    }

    @Override
    public Mode getMode() {
        return this.controller.getMode();
    }

    @Override
    public boolean compareAndSetMode(Mode expected, Mode newMode) {
        return this.controller.compareAndSetMode(expected, newMode);
    }

    @Override
    public void setMode(Mode mode) {
        this.controller.setMode(mode);
    }

    @Override
    public State getState() {
        return this.controller.getState();
    }

    @Override
    public Substate getSubstate() {
        return this.controller.getSubstate();
    }

    @Override
    public S getValue() {
        return this.controller.getValue();
    }

    @Override
    public S awaitValue() throws InterruptedException {
        return this.controller.awaitValue();
    }

    @Override
    public S awaitValue(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        return this.controller.awaitValue(time, unit);
    }

    @Override
    public ServiceName getName() {
        return this.controller.getName();
    }

    @Override
    public ServiceName[] getAliases() {
        return this.controller.getAliases();
    }

    @Deprecated
    @Override
    public void addListener(org.jboss.msc.service.ServiceListener<? super S> serviceListener) {
        this.controller.addListener(serviceListener);
    }

    @Deprecated
    @Override
    public void removeListener(org.jboss.msc.service.ServiceListener<? super S> serviceListener) {
        this.controller.removeListener(serviceListener);
    }

    @Override
    public StartException getStartException() {
        return this.controller.getStartException();
    }

    @Override
    public void retry() {
        this.controller.retry();
    }

    @Override
    public Set<ServiceName> getImmediateUnavailableDependencies() {
        return this.controller.getImmediateUnavailableDependencies();
    }

    @Override
    public SingletonService<S> getService() {
        return this.service;
    }
}
