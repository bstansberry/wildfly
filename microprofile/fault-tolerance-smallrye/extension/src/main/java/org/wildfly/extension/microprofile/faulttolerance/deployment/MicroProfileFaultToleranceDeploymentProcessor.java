/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.concurrent.atomic.AtomicBoolean;

import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger;

/**
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceDeploymentProcessor implements DeploymentUnitProcessor {

    private static AtomicBoolean hystrixConfigured = new AtomicBoolean();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!hystrixConfigured.getAndSet(true)) {
            // TODO Alternative solution to support hystrix configuration only from the "globally scoped config"
            // as opposed to deployments providing their configuration.
            //ClassLoader extensionClassLoader = MicroProfileFaultToleranceExtension.class.getClassLoader();
            //Config mpConfig = ConfigProvider.getConfig(extensionClassLoader);

            Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            Config mpConfig = ConfigProvider.getConfig(module.getClassLoader());
            AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();

            // We need to iterate over all keys since the key names are dynamic and not known in advance
            for (String key : mpConfig.getPropertyNames()) {
                if (key.startsWith("hystrix.")) {
                    String value = mpConfig.getValue(key, String.class);
                    MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("Configuring Hystrix: %s=%s", key, value);
                    configInstance.setProperty(key, value);
                }
            }
        } else {
            MicroProfileFaultToleranceLogger.ROOT_LOGGER.hystrixAlreadyConfigured(deploymentUnit.getName());
        }


        // Weld Extension
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw MicroProfileFaultToleranceLogger.ROOT_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), WELD_CAPABILITY_NAME);
        }
        if (weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
            weldCapability.registerExtensionInstance(new MicroProfileFaultToleranceCDIExtension(), deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // noop
    }
}
