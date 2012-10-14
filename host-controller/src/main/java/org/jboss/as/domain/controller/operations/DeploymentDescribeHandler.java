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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Outputs the deployment's subsystem-specific configuration as a series of operations needed to construct the configuration.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 *
 */
public class DeploymentDescribeHandler implements OperationStepHandler {

    public static final OperationStepHandler INSTANCE = new DeploymentDescribeHandler();

    private DeploymentDescribeHandler() {
        // no-op
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String opName = operation.require(OP).asString();
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));

        final ModelNode result = new ModelNode();
        final ModelNode profile = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        result.setEmptyList();

        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        final AtomicReference<ModelNode> failureRef = new AtomicReference<ModelNode>();

        final ModelNode subsystemResults = new ModelNode().setEmptyList();
        final Map<String, ModelNode> includeResults = new HashMap<String, ModelNode>();

        // Add a step at end to assemble all the data
        // Add steps in the reverse of expected order, as Stage.IMMEDIATE adds to the top of the list
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                boolean failed = false;
                if (failureRef.get() != null) {
                    // One of our subsystems failed
                    context.getFailureDescription().set(failureRef.get());
                    failed = true;
                } else {
                    for (ModelNode includeRsp : includeResults.values()) {
                        if (includeRsp.hasDefined(FAILURE_DESCRIPTION)) {
                            context.getFailureDescription().set(includeRsp.get(FAILURE_DESCRIPTION));
                            failed = true;
                            break;
                        }
                        ModelNode includeResult = includeRsp.get(RESULT);
                        if (includeResult.isDefined()) {
                            for (ModelNode op : includeResult.asList()) {
                                result.add(op);
                            }
                        }
                    }
                }
                if (!failed) {
                    for (ModelNode subsysRsp : subsystemResults.asList()) {
                        result.add(subsysRsp);
                    }
                    context.getResult().set(result);
                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.IMMEDIATE);

        if (profile.hasDefined(SUBSYSTEM)) {
            for (final String subsystemName : profile.get(SUBSYSTEM).keys()) {
                final ModelNode subsystemRsp = new ModelNode();
                PathElement pe = PathElement.pathElement(SUBSYSTEM, subsystemName);
                PathAddress fullAddress = address.append(pe);
                final ModelNode subsystemAddress = fullAddress.toModelNode();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(subsystemAddress);
                PathAddress relativeAddress = PathAddress.pathAddress(pe);
                OperationStepHandler subsysHandler = registry.getOperationHandler(relativeAddress, opName);
                if (subsysHandler == null) {
                    // data must be runtime stuff, not config
                    continue;
                }

                // Step to store subsystem ops in overall list
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        if (failureRef.get() == null) {
                            if (subsystemRsp.hasDefined(FAILURE_DESCRIPTION)) {
                                failureRef.set(subsystemRsp.get(FAILURE_DESCRIPTION));
                            } else if (subsystemRsp.hasDefined(RESULT)) {
                                for (ModelNode op : subsystemRsp.require(RESULT).asList()) {
                                    subsystemResults.add(op);
                                }
                            }
                        }
                        context.stepCompleted();
                    }
                }, OperationContext.Stage.IMMEDIATE);

                // Step to determine subsystem ops
                context.addStep(subsystemRsp, newOp, subsysHandler, OperationContext.Stage.IMMEDIATE);
            }
        }

        context.stepCompleted();
    }
}
