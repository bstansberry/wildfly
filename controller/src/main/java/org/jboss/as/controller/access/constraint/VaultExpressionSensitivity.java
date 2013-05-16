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

package org.jboss.as.controller.access.constraint;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.dmr.ModelNode;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class VaultExpressionSensitivity {

    private final TargetAttribute targetAttribute;
    private final VaultExpressionSensitivityConfig config;

    public VaultExpressionSensitivity(final TargetAttribute targetAttribute,
                                      final VaultExpressionSensitivityConfig config) {
        this.targetAttribute = targetAttribute;
        this.config = config;
    }



    public boolean isSensitive(Action.ActionEffect actionEffect) {
        if (config.isSensitive(actionEffect)) {
            if (hasVaultExpression(targetAttribute.getCurrentValue())) {
                return true;
            }
            // TODO new data
            throw new UnsupportedOperationException("implement me");
        }
        return false;
    }

    private boolean hasVaultExpression(ModelNode node) {
        // TODO check for vault expression in the node
        throw new UnsupportedOperationException("implement me");
    }

}
