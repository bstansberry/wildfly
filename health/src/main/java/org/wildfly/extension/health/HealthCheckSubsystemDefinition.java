/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.wildfly.extension.health;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelType;

public class HealthCheckSubsystemDefinition extends PersistentResourceDefinition {


    private static final AttributeDefinition ATTR = SimpleAttributeDefinitionBuilder.create("status", ModelType.STRING)
            .setRequired(false)
            .setAllowedValues("UP", "DOWN")
            .build();

    static final String REQUIRED_CAP = "org.wildfly.management.http.extensible";
    private static final RuntimeCapability<Void> CAP = RuntimeCapability.Builder.of(HealthCheckExtension.EXTENSION_NAME)
            .addRequirements(REQUIRED_CAP)
            .build();

    static final HealthCheckSubsystemDefinition INSTANCE = new HealthCheckSubsystemDefinition();

    private HealthCheckSubsystemDefinition() {
        super(new Parameters(HealthCheckExtension.SUBSYSTEM_PATH, HealthCheckExtension.getResolver())
                .setAddHandler(HealthCheckSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ModelOnlyRemoveStepHandler())
                .setCapabilities(CAP)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(ATTR, OSH);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        OperationDefinition OD = SimpleOperationDefinitionBuilder.of("check", HealthCheckExtension.getResolver()).build();
        resourceRegistration.registerOperationHandler(OD, OSH);
    }

    private static final OperationStepHandler OSH = ((operationContext, modelNode) -> {
        String config = System.getProperty("health.status", "UP");
        if ("missing".equalsIgnoreCase(config)) {
            throw new Resource.NoSuchResourceException(PathElement.pathElement("subsystem", HealthCheckExtension.SUBSYSTEM_NAME));
        } else {
            String status = "DOWN".equalsIgnoreCase(config) ? "DOWN" : "UP";
            operationContext.getResult().get("outcome").set(status);
        }
    });

}
