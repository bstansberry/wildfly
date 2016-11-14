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

import java.util.Optional;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonService;

/**
 * Local singleton service implementation.
 * @author Paul Ferraro
 */
public class LocalSingletonService<T> implements SingletonService<T> {

    private final Service<T> service;
    private final Value<Group> group;

    public LocalSingletonService(LocalSingletonServiceContext<T> context) {
        this.service = context.getService();
        this.group = context.getGroup();
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.service.start(context);
    }

    @Override
    public void stop(StopContext context) {
        this.service.stop(context);
    }

    @Override
    public T getValue() {
        return this.service.getValue();
    }

    @Override
    public boolean isPrimary() {
        // A local singleton is always primary
        return true;
    }

    @Override
    public Optional<Node> getPrimaryProvider() {
        return Optional.of(this.group.getValue().getLocalNode());
    }
}
