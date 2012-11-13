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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for updating attributes of database management connections.
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public DatabaseConnectionWriteAttributeHandler() {
        super(DatabaseConnectionResourceDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS, DatabaseConnectionResourceDefinition.IDLE_TIMEOUT_MINUTES,
              DatabaseConnectionResourceDefinition.MIN_POOL_SIZE, DatabaseConnectionResourceDefinition.POOL_USE_STRICT_MIN,
              DatabaseConnectionResourceDefinition.USE_FAST_FAIL);
    }

    boolean isSupported(AttributeDefinition attribute) {
        return getAttributeDefinition(attribute.getName()) != null;
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName,
                                           final ModelNode resolvedValue, final ModelNode currentValue,
                                           final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDatabaseConnectionService(attributeName, context, operation, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName,
                                         final ModelNode valueToRestore, final ModelNode valueToRevert,
                                         final Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDatabaseConnectionService(attributeName, context, operation, restored);
    }

    private void updateDatabaseConnectionService(final String attributeName, final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        PoolConfiguration poolConfiguration = getPoolConfig(context, operation);

        AttributeDefinition ad = getAttributeDefinition(attributeName);
        ModelNode resolved = getAttributeDefinition(attributeName).resolveModelAttribute(context, model);
        if (ad == DatabaseConnectionResourceDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS) {
            poolConfiguration.setBlockingTimeout(resolved.asLong());
        } else if (ad == DatabaseConnectionResourceDefinition.IDLE_TIMEOUT_MINUTES) {
            poolConfiguration.setConnectionIdleTimeout(resolved.asLong());
        } else if (ad == DatabaseConnectionResourceDefinition.MIN_POOL_SIZE) {
            poolConfiguration.setMinSize(resolved.asInt());
        } else if (ad == DatabaseConnectionResourceDefinition.POOL_USE_STRICT_MIN) {
            poolConfiguration.setStrictMin(resolved.asBoolean());
        }else if (ad == DatabaseConnectionResourceDefinition.USE_FAST_FAIL) {
            poolConfiguration.setUseFastFail(resolved.asBoolean());
        } else {
            // Coding error
            throw new IllegalStateException();
        }
    }

    private PoolConfiguration getPoolConfig(OperationContext context, ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ServiceName svcName = PoolConfigService.getServiceName(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> controller = registry.getService(svcName);
        return (PoolConfiguration) controller.getValue();
    }
}
