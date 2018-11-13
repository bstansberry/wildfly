/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;

/**
 * @author Stuart Douglas
 */
final class UUIDKeyGeneratorResourceDefinition extends AbstractKeyGeneratorResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTES = { JNDI_NAME };

    UUIDKeyGeneratorResourceDefinition() {
        super(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH,
                CmpExtension.getResolver(CmpSubsystemModel.UUID_KEY_GENERATOR),
                new ModelOnlyAddStepHandler(JNDI_NAME), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(CmpExtension.DEPRECATED_SINCE);
    }

    @Override
    AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }
}
