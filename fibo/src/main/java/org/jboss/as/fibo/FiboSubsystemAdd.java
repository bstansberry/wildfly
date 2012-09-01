/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.net.URL;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;

/**
 * The "fibo" subsystem add update handler.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class FiboSubsystemAdd extends AbstractAddStepHandler {

    static final FiboSubsystemAdd INSTANCE = new FiboSubsystemAdd();

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        resource.getModel().setEmptyObject();

        // Add a step to install the fibo.ear deployment
        if (requiresRuntime(context)) {  // only add the step if we are going to actually deploy the ear

            PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "fibo.ear"));
            ModelNode op = Util.createOperation(ADD, deploymentAddress);
            op.get(ENABLED).set(true);
            op.get(PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

            Module module = Module.forClass(getClass());
            URL url = module.getExportedResource("fibo.ear");
            String urlString = url.toExternalForm();
            System.out.println(url);

            ModelNode contentItem = new ModelNode();
            contentItem.get(URL).set(urlString);

            op.get(CONTENT).add(contentItem);

            ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
            OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, ADD);

            context.addStep(op, handler, OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // We overrode the code that calls this method
        throw new UnsupportedOperationException();
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
