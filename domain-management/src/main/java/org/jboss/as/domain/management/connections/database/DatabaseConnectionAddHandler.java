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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import javax.sql.DataSource;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.Services;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
/**
 * Handler for adding database management connections.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionAddHandler extends AbstractAddStepHandler {

    public static final DatabaseConnectionAddHandler INSTANCE = new DatabaseConnectionAddHandler();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : DatabaseConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        ModelNode dsName = DatabaseConnectionResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, fullModel);
        boolean useDataSource = dsName.isDefined();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final DatabaseConnectionManagerService connectionManagerService = new DatabaseConnectionManagerService(useDataSource);

        ServiceBuilder<ConnectionManager> serviceBuilder = serviceTarget.addService(DatabaseConnectionManagerService.BASE_SERVICE_NAME.append(name), connectionManagerService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND);

        if (useDataSource) {
            ServiceName datasourceService = ServiceName.JBOSS.append("data-source").append(dsName.asString());
            serviceBuilder.addDependency(datasourceService,DataSource.class,connectionManagerService.getDatasource());
        } else {
            ServiceController<PoolConfiguration> poolConfigSC = PoolConfigService.addService(name, context, fullModel,
                    serviceTarget, verificationHandler);
            if (newControllers != null) {
                newControllers.add(poolConfigSC);
            }

            // Add an attachment to the context so ConnectionPropertyAdd/Remove know we've handled the properties
            if (!context.isBooting()) { // Don't bother if we're booting
                PoolConfigService.PoolConfigServiceSet set = context.getAttachment(PoolConfigService.PoolConfigServiceSet.ATTACHMENT_KEY);
                if (set == null) {
                    set = new PoolConfigService.PoolConfigServiceSet();
                    context.attach(PoolConfigService.PoolConfigServiceSet.ATTACHMENT_KEY, set);
                }
                set.add(name);
            }
            serviceBuilder.addDependency(poolConfigSC.getName(), PoolConfiguration.class, connectionManagerService.getPoolConfig());

            Services.addDomainManagementExecutorServiceDependency(serviceBuilder, connectionManagerService.getExecutorService());
        }

        ServiceController<ConnectionManager> sc = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }
    }


}
