/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.database;

import java.util.HashSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

/**
 * Utilities for installing a {@link PoolConfiguration} as a service.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public final class PoolConfigService {

    public static ServiceName getServiceName(String databaseName) {
        return DatabaseConnectionManagerService.BASE_SERVICE_NAME.append(databaseName, "pool-config");
    }

    public static ServiceController<PoolConfiguration> addService(String databaseName, OperationContext context, ModelNode model, ServiceTarget target,
                                                           ServiceListener<? super PoolConfiguration> ... listeners) throws OperationFailedException {
        PoolConfiguration pc = new PoolConfiguration(databaseName, context, model);
        ImmediateValue<PoolConfiguration> value = new ImmediateValue<PoolConfiguration>(pc);
        ValueService<PoolConfiguration> service = new ValueService<PoolConfiguration>(value);
        ServiceBuilder<PoolConfiguration> builder = target.addService(PoolConfigService.getServiceName(databaseName), service);
        if (listeners != null && listeners.length > 0) {
            builder.addListener(listeners);
        }
        return builder.install();
    }

    private PoolConfigService() {
        // no-op
    }



    public static class PoolConfigServiceSet extends HashSet<String> {

        public static final OperationContext.AttachmentKey<PoolConfigServiceSet> ATTACHMENT_KEY =
                OperationContext.AttachmentKey.create(PoolConfigServiceSet.class);

        public PoolConfigServiceSet() {
            super(4);
        }
    }
}
