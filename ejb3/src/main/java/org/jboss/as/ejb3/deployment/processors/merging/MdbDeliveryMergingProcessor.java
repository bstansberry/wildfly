/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.delivery.metadata.EJBBoundMdbDeliveryMetaData;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.DeliveryActive;
import org.jboss.ejb3.annotation.DeliveryGroup;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

/**
 * Handles the {@link org.jboss.ejb3.annotation.DeliveryActive} and {@link org.jboss.ejb3.annotation.DeliveryGroup} annotation merging
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Flavia Rainone
 */
public class MdbDeliveryMergingProcessor extends AbstractMergingProcessor<MessageDrivenComponentDescription> {

    public MdbDeliveryMergingProcessor() {
        super(MessageDrivenComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final MessageDrivenComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {

        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());

        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }

        final ClassAnnotationInformation<DeliveryActive, Boolean> deliveryActive = clazz.getAnnotationInformation(DeliveryActive.class);

        if (deliveryActive != null
                && !deliveryActive.getClassLevelAnnotations().isEmpty()) {
            componentConfiguration.setDeliveryActive(deliveryActive.getClassLevelAnnotations().get(0));
        }

        final ClassAnnotationInformation<DeliveryGroup, String> deliveryGroup = clazz.getAnnotationInformation(DeliveryGroup.class);

        if (deliveryGroup != null) {
            List<String> deliveryGroups = deliveryGroup.getClassLevelAnnotations();
            if (!deliveryGroups.isEmpty()) {
                componentConfiguration.setDeliveryGroup(deliveryGroups.toArray(new String[deliveryGroups.size()]));
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final MessageDrivenComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final String ejbName = componentConfiguration.getEJBName();
        final EjbJarMetaData metaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (metaData == null) {
            return;
        }
        final AssemblyDescriptorMetaData assemblyDescriptor = metaData.getAssemblyDescriptor();
        if (assemblyDescriptor == null) {
            return;
        }
        Boolean deliveryActive = null;
        String[] deliveryGroups = null;
        final List<EJBBoundMdbDeliveryMetaData> deliveryMetaDataList = assemblyDescriptor.getAny(EJBBoundMdbDeliveryMetaData.class);
        if (deliveryMetaDataList != null) {
            for (EJBBoundMdbDeliveryMetaData deliveryMetaData : deliveryMetaDataList) {
                if ("*".equals(deliveryMetaData.getEjbName())) {
                    // do not overwrite if deliveryActive is not null
                    if (deliveryActive == null)
                        deliveryActive = deliveryMetaData.isDeliveryActive();
                    deliveryGroups = mergeDeliveryGroups(deliveryGroups, deliveryMetaData.getDeliveryGroups());

                } else if (ejbName.equals(deliveryMetaData.getEjbName())) {
                    deliveryActive = deliveryMetaData.isDeliveryActive();
                    deliveryGroups = mergeDeliveryGroups(deliveryGroups, deliveryMetaData.getDeliveryGroups());
                }
            }
        }
        // delivery group configuration has precedence over deliveryActive
        if (deliveryGroups != null && deliveryGroups.length > 0) {
            componentConfiguration.setDeliveryGroup(deliveryGroups);
        }
        else if (deliveryActive != null) {
            componentConfiguration.setDeliveryActive(deliveryActive);
        }
    }

    private final String[] mergeDeliveryGroups(String[] deliveryGroups1, String[] deliveryGroups2) {
        if (deliveryGroups1 == null)
            return deliveryGroups2;
        if (deliveryGroups2 == null)
            return deliveryGroups1;
        final List<String> deliveryGroupList = new ArrayList(deliveryGroups1.length + deliveryGroups2.length);
        Collections.addAll(deliveryGroupList, deliveryGroups1);
        Collections.addAll(deliveryGroupList, deliveryGroups2);
        return deliveryGroupList.toArray(new String[deliveryGroupList.size()]);
    }
}
