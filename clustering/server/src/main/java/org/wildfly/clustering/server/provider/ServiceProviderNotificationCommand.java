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

package org.wildfly.clustering.server.provider;

import java.util.Set;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public class ServiceProviderNotificationCommand<T> implements Command<Void, ServiceNotifier<T>> {
    private static final long serialVersionUID = -9137685114303497928L;

    private final T service;
    private final Set<Node> providers;

    public ServiceProviderNotificationCommand(T service, Set<Node> providers) {
        this.service = service;
        this.providers = providers;
    }

    @Override
    public Void execute(ServiceNotifier<T> notifier) throws Exception {
        notifier.notifyListener(this.service, this.providers);
        return null;
    }
}
