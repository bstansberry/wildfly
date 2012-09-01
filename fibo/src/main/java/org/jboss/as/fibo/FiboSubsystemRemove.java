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

package org.jboss.as.fibo;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * The "fibo" subsystem remove update handler.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class FiboSubsystemRemove extends AbstractRemoveStepHandler {

    static final FiboSubsystemRemove INSTANCE = new FiboSubsystemRemove();

    private FiboSubsystemRemove() {
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // Add a step to remove the fibo.ear deployment
        if (requiresRuntime(context)) {  // only add the step if we are going to actually undeploy the ear

            PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "fibo.ear"));
            ModelNode op = Util.createOperation(REMOVE, deploymentAddress);

            ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
            OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, REMOVE);

            context.addStep(op, handler, OperationContext.Stage.MODEL);
        }

        super.performRemove(context, operation, model);

    }
}
