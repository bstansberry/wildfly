/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CONNECTION_PROPERTIES;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a database connection pool connection configuration property.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ConnectionPropertyResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(CONNECTION_PROPERTIES);

    public static SimpleAttributeDefinition CONNECTION_PROPERTY_VALUE = new SimpleAttributeDefinitionBuilder(VALUE, ModelType.STRING, false)
            .setAllowExpression(true)
            .build();

    ConnectionPropertyResourceDefinition() {
        super(PATH_ELEMENT, ControllerResolver.getResolver("core.management.database-connection.connection-properties"),
                ConnectionPropertyAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        resourceRegistration.registerReadWriteAttribute(CONNECTION_PROPERTY_VALUE, null,
                new ReloadRequiredWriteAttributeHandler(CONNECTION_PROPERTY_VALUE));
    }

}
