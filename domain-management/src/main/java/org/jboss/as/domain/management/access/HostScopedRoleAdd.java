/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.access.ConfigurableAuthorizer;
import org.jboss.as.controller.access.constraint.HostEffectConstraint;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handles the {@code add} operation for a {@link HostScopedRolesResourceDefinition host scoped role}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class HostScopedRoleAdd extends AbstractAddStepHandler {

    private final Map<String, HostEffectConstraint> constraintMap;
    private final ConfigurableAuthorizer authorizer;

    HostScopedRoleAdd(Map<String, HostEffectConstraint> constraintMap, ConfigurableAuthorizer authorizer) {
        super(HostScopedRolesResourceDefinition.BASE_ROLE, HostScopedRolesResourceDefinition.HOSTS);
        this.constraintMap = constraintMap;
        this.authorizer = authorizer;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        String baseRole = ServerGroupScopedRoleResourceDefinition.BASE_ROLE.resolveModelAttribute(context, model).asString();

        List<ModelNode> nodeList = ServerGroupScopedRoleResourceDefinition.SERVER_GROUPS.resolveModelAttribute(context, model).asList();

        addScopedRole(roleName, baseRole, nodeList, authorizer, constraintMap);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {

        String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        authorizer.removeScopedRole(roleName);
        constraintMap.remove(roleName);
    }

    static void addScopedRole(final String roleName, final String baseRole, final List<ModelNode> hostNodes,
                              final ConfigurableAuthorizer authorizer, final Map<String, HostEffectConstraint> constraintMap) {

        List<String> serverGroups = new ArrayList<String>();
        for (ModelNode group : hostNodes) {
            serverGroups.add(group.asString());
        }
        HostEffectConstraint constraint = new HostEffectConstraint(serverGroups);
        authorizer.addScopedRole(roleName, baseRole, constraint);
        constraintMap.put(roleName, constraint);
    }
}

