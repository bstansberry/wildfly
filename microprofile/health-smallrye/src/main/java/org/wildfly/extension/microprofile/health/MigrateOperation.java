/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.health._private.MicroProfileHealthLogger.LOGGER;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

final class MigrateOperation implements OperationStepHandler {

    private static final String MIGRATE = "migrate";
    private static final String MIGRATION_WARNINGS = "migration-warnings";
    private static final String MIGRATION_ERROR = "migration-error";
    private static final String MIGRATION_OPERATIONS = "migration-operations";
    private static final String DESCRIBE_MIGRATION = "describe-migration";
    private static final PathElement BASE_HEALTH_EXTENSION = PathElement.pathElement(EXTENSION, "org.wildfly.extension.health");
    private static final PathElement BASE_HEALTH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, "health");

    private static final OperationStepHandler MIGRATE_INSTANCE = new MigrateOperation(false);
    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new MigrateOperation(true);


    private static final AttributeDefinition MIGRATION_WARNINGS_ATTR = new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
            .setRequired(false)
            .build();

    private static final AttributeDefinition MIGRATION_ERROR_ATTR = new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
            .setValueType(ModelType.OBJECT)
            .setRequired(false)
            .build();

    private static final AttributeDefinition MIGRATION_OPERATIONS_ATTR = new PrimitiveListAttributeDefinition.Builder(MIGRATION_OPERATIONS, ModelType.OBJECT)
            .setRequired(false)
            .build();

    private final boolean describe;

    MigrateOperation(boolean describe) {
        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry) {
        ResourceDescriptionResolver resourceDescriptionResolver = MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(MIGRATE, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .build(),
                MigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_OPERATIONS_ATTR)
                        .setReadOnly()
                        .build(),
                MigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        // Our subsystem requires base health outside of admin-only. So if we're not admin-only,
        // or we are but the base health subsytem is already present, there's nothing to do
        PathAddress ourAddress = context.getCurrentAddress();
        PathAddress parentAddress = ourAddress.getParent();
        Resource parentResource;
        if (context.getRunningMode() != RunningMode.ADMIN_ONLY
                || (parentResource = context.readResourceFromRoot(parentAddress, false)).hasChild(BASE_HEALTH_SUBSYSTEM) ) {
            if (describe) {
                context.getResult().get(MIGRATION_OPERATIONS).setEmptyList();
            }
            return;
        }

        // preserve the order of insertion of the add operations for the new subsystem.
        final Map<PathAddress, ModelNode> migrationOperations = new LinkedHashMap<>();

        final Resource rootResource = parentAddress.size() == 0 ? parentResource : context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (!rootResource.hasChild(BASE_HEALTH_EXTENSION)) {
            PathAddress baseExtAddr = PathAddress.pathAddress(BASE_HEALTH_EXTENSION);
            migrationOperations.put(baseExtAddr, Util.createAddOperation(baseExtAddr));
        }

        final ModelNode ourModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();

        PathAddress baseSubsystemAddr = parentAddress.append(BASE_HEALTH_SUBSYSTEM);
        ModelNode baseAddOp = Util.createAddOperation(parentAddress.append(BASE_HEALTH_SUBSYSTEM));
        String secAttrName = MicroProfileHealthSubsystemDefinition.SECURITY_ENABLED.getName();
        baseAddOp.get(secAttrName).set(ourModel.get(secAttrName));

        migrationOperations.put(baseSubsystemAddr, baseAddOp);

        if (ourModel.hasDefined(secAttrName)) {
            // Drop our setting; rely on the base
            migrationOperations.put(ourAddress, Util.getWriteAttributeOperation(ourAddress, secAttrName, new ModelNode()));
        }


        if (describe) {
            ModelNode result = new ModelNode();
            result.get(MIGRATION_OPERATIONS).set(migrationOperations.values());
            context.getResult().set(result);
        } else {
            final Map<PathAddress, ModelNode> migrateOpResponses = new LinkedHashMap<>();
            MultistepUtil.recordOperationSteps(context, migrationOperations, migrateOpResponses);

            context.completeStep((resultAction, completeContext, completeOp) -> {
                final ModelNode result = new ModelNode();
                if (resultAction == OperationContext.ResultAction.ROLLBACK) {
                    for (Map.Entry<PathAddress, ModelNode> entry : migrateOpResponses.entrySet()) {
                        if (entry.getValue().hasDefined(FAILURE_DESCRIPTION)) {
                            //we check for failure description, as every node has 'failed', but one
                            //the real error has a failure description
                            //we break when we find the first one, as there will only ever be one failure
                            //as the op stops after the first failure
                            ModelNode desc = new ModelNode();
                            desc.get(OP).set(migrationOperations.get(entry.getKey()));
                            desc.get(RESULT).set(entry.getValue());
                            result.get(MIGRATION_ERROR).set(desc);
                            break;
                        }
                    }
                    completeContext.getFailureDescription().set(LOGGER.migrationFailed());
                }

                completeContext.getResult().set(result);
            });
        }
    }
}
