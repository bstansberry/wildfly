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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a connection factory for an Database security store.
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 *  @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DatabaseConnectionResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement RESOURCE_PATH = PathElement.pathElement(ModelDescriptionConstants.DATABASE_CONNECTION);

    public static final SimpleAttributeDefinition DATA_SOURCE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DATA_SOURCE, ModelType.STRING, false)
            .setXmlName(Attribute.REF.getLocalName())
            .setAlternatives(ModelDescriptionConstants.CONNECTION_URL, ModelDescriptionConstants.URL_DELIMITER,
                    ModelDescriptionConstants.DRIVER_MODULE_NAME, ModelDescriptionConstants.DRIVER_CLASS_NAME,
                    ModelDescriptionConstants.USERNAME, ModelDescriptionConstants.PASSWORD,
                    ModelDescriptionConstants.MAX_POOL_SIZE, ModelDescriptionConstants.MIN_POOL_SIZE,
                    ModelDescriptionConstants.BACKGROUND_VALIDATION, ModelDescriptionConstants.BACKGROUND_VALIDATION_MILLIS,
                    ModelDescriptionConstants.BLOCKING_TIMEOUT_WAIT_MILLIS, ModelDescriptionConstants.CHECK_VALID_CONNECTION_SQL,
                    ModelDescriptionConstants.IDLE_TIMEOUT_MINUTES, ModelDescriptionConstants.NEW_CONNECTION_SQL,
                    ModelDescriptionConstants.POOL_PREFILL, ModelDescriptionConstants.POOL_USE_STRICT_MIN,
                    ModelDescriptionConstants.VALIDATE_ON_MATCH, ModelDescriptionConstants.VALID_CONNECTION_CHECKER_CLASS_NAME,
                    ModelDescriptionConstants.VALID_CONNECTION_CHECKER_MODULE_NAME, ModelDescriptionConstants.VALID_CONNECTION_CHECKER_PROPERTIES,
                    ModelDescriptionConstants.USE_FAST_FAIL)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CONNECTION_URL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONNECTION_URL, ModelType.STRING, false)
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition DRIVER_MODULE_NAME =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DRIVER_MODULE_NAME, ModelType.STRING)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(false)
            .build();

    public static final SimpleAttributeDefinition DRIVER_CLASS_NAME =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DRIVER_CLASS_NAME, ModelType.STRING)
            .setXmlName(Attribute.CLASS_NAME.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .build();

    public static final SimpleAttributeDefinition USERNAME =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USERNAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Element.USER_NAME.getLocalName())
            .build();

    public static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PASSWORD, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, true))
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MAX_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MAX_POOL_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setDefaultValue(new ModelNode(20))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MIN_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MIN_POOL_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true))
            .setDefaultValue(new ModelNode(0))
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition BACKGROUND_VALIDATION =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BACKGROUND_VALIDATION, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition BACKGROUND_VALIDATION_MILLIS =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BACKGROUND_VALIDATION_MILLIS, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(0))
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
                    .build();

    public static final SimpleAttributeDefinition BLOCKING_TIMEOUT_WAIT_MILLIS =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BLOCKING_TIMEOUT_WAIT_MILLIS, ModelType.LONG, true)
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true))
            .setDefaultValue(new ModelNode(30000))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setXmlName(Attribute.BLOCKING_TIMEOUT_MILLIS.getLocalName())
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .build();

    public static SimpleAttributeDefinition CHECK_VALID_CONNECTION_SQL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CHECK_VALID_CONNECTION_SQL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition IDLE_TIMEOUT_MINUTES =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.IDLE_TIMEOUT_MINUTES, ModelType.LONG, true)
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setDefaultValue(new ModelNode(30))
            .setMeasurementUnit(MeasurementUnit.MINUTES)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .build();

    public static SimpleAttributeDefinition NEW_CONNECTION_SQL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NEW_CONNECTION_SQL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POOL_PREFILL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.POOL_PREFILL, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PREFILL.getLocalName())
            .setDefaultValue(new ModelNode(false))
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition POOL_USE_STRICT_MIN =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.POOL_USE_STRICT_MIN, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.USE_STRICT_MIN.getLocalName())
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static SimpleAttributeDefinition URL_DELIMITER =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL_DELIMITER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static SimpleAttributeDefinition VALIDATE_ON_MATCH =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALIDATE_ON_MATCH, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static SimpleAttributeDefinition VALID_CONNECTION_CHECKER_CLASS_NAME =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALID_CONNECTION_CHECKER_CLASS_NAME, ModelType.STRING, true)
            .setXmlName(Attribute.CLASS_NAME.getLocalName())
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static SimpleAttributeDefinition VALID_CONNECTION_CHECKER_MODULE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALID_CONNECTION_CHECKER_MODULE_NAME, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(true)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static PropertiesAttributeDefinition VALID_CONNECTION_CHECKER_PROPERTIES =
            new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.VALID_CONNECTION_CHECKER_PROPERTIES, true)
            .setXmlName(Element.CONFIG_PROPERTY.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition USE_FAST_FAIL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USE_FAST_FAIL, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setAlternatives(ModelDescriptionConstants.DATA_SOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {DATA_SOURCE, URL_DELIMITER, DRIVER_MODULE_NAME, DRIVER_CLASS_NAME,
            CONNECTION_URL, NEW_CONNECTION_SQL, MIN_POOL_SIZE, MAX_POOL_SIZE, POOL_PREFILL, POOL_USE_STRICT_MIN,
            USERNAME, PASSWORD, VALIDATE_ON_MATCH, BACKGROUND_VALIDATION, BACKGROUND_VALIDATION_MILLIS, USE_FAST_FAIL,
            CHECK_VALID_CONNECTION_SQL, VALID_CONNECTION_CHECKER_MODULE, VALID_CONNECTION_CHECKER_CLASS_NAME,
            VALID_CONNECTION_CHECKER_PROPERTIES, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLE_TIMEOUT_MINUTES};

    public static final DatabaseConnectionResourceDefinition INSTANCE = new DatabaseConnectionResourceDefinition();

    private DatabaseConnectionResourceDefinition() {
        super(RESOURCE_PATH, ControllerResolver.getResolver("core.management.database-connection"),
                DatabaseConnectionAddHandler.INSTANCE, DatabaseConnectionRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        DatabaseConnectionWriteAttributeHandler writeHandler = new DatabaseConnectionWriteAttributeHandler();
        // TODO all this just to use a single ReloadRequiredWriteAttributeHandler while still registering
        // attributes in order. Consider an addAttributeDefinition method on ReloadRequiredWriteAttributeHandler
        List<AttributeDefinition> reloads = new ArrayList<AttributeDefinition>();
        Map<AttributeDefinition, Boolean> reloadMap = new HashMap<AttributeDefinition, Boolean>();
        for (AttributeDefinition attr : DatabaseConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            if (writeHandler.isSupported(attr)) {
                reloadMap.put(attr, Boolean.FALSE);
            } else {
                reloads.add(attr);
                reloadMap.put(attr, Boolean.TRUE);
            }
        }
        OperationStepHandler reloadHandler = new ReloadRequiredWriteAttributeHandler(reloads);
        for (Map.Entry<AttributeDefinition, Boolean> ad : reloadMap.entrySet()) {
            resourceRegistration.registerReadWriteAttribute(ad.getKey(), null, ad.getValue() ? reloadHandler : writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConnectionPropertyResourceDefinition());
    }
}
