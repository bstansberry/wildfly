/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.connections.database.ConnectionPropertyResourceDefinition.CONNECTION_PROPERTY_VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Adds a JDBC connection configuration property.
 */
public class ConnectionPropertyAdd extends AbstractAddStepHandler {

    public static final ConnectionPropertyAdd INSTANCE = new ConnectionPropertyAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode modelNode) throws OperationFailedException {
        CONNECTION_PROPERTY_VALUE.validateAndSet(operation, modelNode);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return super.requiresRuntime(context) && !context.isBooting();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        // If DatabaseConnectionAddHandler attached the key, it's read the value we set in Stage.MODEL
        // and incorporated it; otherwise this op was called separately and a reload is needed
        if (!hasPoolConfigAttachment(context, operation)) {
            context.reloadRequired();
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {

        // If DatabaseConnectionAddHandler attached the key, it's read the value we set in Stage.MODEL
        // and incorporated it; otherwise this op was called separately and we should revert the reload we set
        if (!hasPoolConfigAttachment(context, operation)) {
            context.revertReloadRequired();
        }
    }

    static boolean hasPoolConfigAttachment(OperationContext context, ModelNode operation) {

        PathAddress pa = PathAddress.pathAddress(operation.require(OP_ADDR));
        PathElement pe = pa.getElement(pa.size() - 2);
        PoolConfigService.PoolConfigServiceSet set = context.getAttachment(PoolConfigService.PoolConfigServiceSet.ATTACHMENT_KEY);

        return set != null && set.contains(pe.getValue());
    }

}
