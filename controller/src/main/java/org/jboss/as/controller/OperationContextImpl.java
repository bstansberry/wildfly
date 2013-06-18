/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CALLER_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.DelegatingImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchServiceTarget;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * Operation context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class OperationContextImpl extends AbstractOperationContext {

    private static final Object NULL = new Object();

    private static final Set<Action.ActionEffect> ACCESS = EnumSet.of(Action.ActionEffect.ACCESS);
    private static final Set<Action.ActionEffect> READ_CONFIG = EnumSet.of(Action.ActionEffect.READ_CONFIG);
    private static final Set<Action.ActionEffect> READ_RUNTIME = EnumSet.of(Action.ActionEffect.READ_RUNTIME);
    private static final Set<Action.ActionEffect> READ_WRITE_CONFIG = EnumSet.of(Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_CONFIG);
    private static final Set<Action.ActionEffect> READ_WRITE_RUNTIME = EnumSet.of(Action.ActionEffect.READ_RUNTIME, Action.ActionEffect.WRITE_RUNTIME);
    private static final Set<Action.ActionEffect> WRITE_CONFIG = EnumSet.of(Action.ActionEffect.WRITE_CONFIG);
    private static final Set<Action.ActionEffect> WRITE_RUNTIME = EnumSet.of(Action.ActionEffect.WRITE_RUNTIME);

    private final ModelControllerImpl modelController;
    private final EnumSet<ContextFlag> contextFlags;
    private final OperationMessageHandler messageHandler;
    private final ServiceTarget serviceTarget;
    private final Map<ServiceName, ServiceController<?>> realRemovingControllers = new HashMap<ServiceName, ServiceController<?>>();
    // protected by "realRemovingControllers"
    private final Map<ServiceName, Step> removalSteps = new HashMap<ServiceName, Step>();
    private final OperationAttachments attachments;
    /** Tracks whether any steps have gotten write access to the model */
    private final Map<PathAddress, Object> affectsModel;
    /** Resources that have had their services restarted, used by ALLOW_RESOURCE_SERVICE_RESTART This should be confined to a thread, so no sync needed */
    private Map<PathAddress, Object> restartedResources = Collections.emptyMap();
    /** A concurrent map for the attachments. **/
    private final ConcurrentMap<AttachmentKey<?>, Object> valueAttachments = new ConcurrentHashMap<AttachmentKey<?>, Object>();
    private final Map<OperationId, AuthorizationResponse> authorizations =
            new ConcurrentHashMap<OperationId, AuthorizationResponse>();
    private final Environment callEnvironment;
    /** Tracks whether any steps have gotten write access to the management resource registration*/
    private volatile boolean affectsResourceRegistration;

    private volatile Resource model;

    private volatile Resource originalModel;

    /** Tracks whether any steps have gotten write access to the runtime */
    private volatile boolean affectsRuntime;
    /** The step that acquired the write lock */
    private Step lockStep;
    /** The step that acquired the container monitor  */
    private Step containerMonitorStep;
    private volatile Boolean requiresModelUpdateAuthorization;

    private final Integer operationId;

    OperationContextImpl(final ModelControllerImpl modelController, final ProcessType processType,
                         final RunningMode runningMode, final EnumSet<ContextFlag> contextFlags,
                            final OperationMessageHandler messageHandler, final OperationAttachments attachments,
                            final Resource model, final ModelController.OperationTransactionControl transactionControl,
                            final ControlledProcessState processState, final boolean booting, final Integer operationId) {
        super(processType, runningMode, transactionControl, processState, booting);
        this.model = model;
        this.originalModel = model;
        this.modelController = modelController;
        this.messageHandler = messageHandler;
        this.attachments = attachments;
        this.affectsModel = booting ? new ConcurrentHashMap<PathAddress, Object>(16 * 16) : new HashMap<PathAddress, Object>(1);
        this.contextFlags = contextFlags;
        this.serviceTarget = new ContextServiceTarget(modelController);
        this.callEnvironment = new Environment(processState, processType);
        this.operationId = operationId;
    }

    public InputStream getAttachmentStream(final int index) {
        if (attachments == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return attachments.getInputStreams().get(index);
    }

    public int getAttachmentStreamCount() {
        return attachments == null ? 0 : attachments.getInputStreams().size();
    }

    @Override
    void awaitModelControllerContainerMonitor() throws InterruptedException {
        if (affectsRuntime) {
            MGMT_OP_LOGGER.debugf("Entered VERIFY stage; waiting for service container to settle");
            // First wait until any removals we've initiated have begun processing, otherwise
            // the ContainerStateMonitor may not have gotten the notification causing it to untick
            waitForRemovals();
            ContainerStateMonitor.ContainerStateChangeReport changeReport = modelController.awaitContainerStateChangeReport();
            // If any services are missing, add a verification handler to see if we caused it
            if (changeReport != null && !changeReport.getMissingServices().isEmpty()) {
                ServiceRemovalVerificationHandler removalVerificationHandler = new ServiceRemovalVerificationHandler(changeReport);
                addStep(new ModelNode(), new ModelNode(), PathAddress.EMPTY_ADDRESS, removalVerificationHandler, Stage.VERIFY);
            }
        }
    }

    @Override
    protected void waitForRemovals() throws InterruptedException {
        if (affectsRuntime && !cancelled) {
            final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
            synchronized (map) {
                while (!map.isEmpty() && !cancelled) {
                    map.wait();
                }
            }
        }
    }

    @Override
    ConfigurationPersister.PersistenceResource createPersistenceResource() throws ConfigurationPersistenceException {
        return modelController.writeModel(model, affectsModel.keySet());
    }

    @Override
    public boolean isRollbackOnRuntimeFailure() {
        return contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL);
    }

    @Override
    public boolean isResourceServiceRestartAllowed() {
        return contextFlags.contains(ContextFlag.ALLOW_RESOURCE_SERVICE_RESTART);
    }


    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        final PathAddress address = activeStep.address;
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        //if (currentStage != Stage.MODEL) {
        //    throw MESSAGES.stageAlreadyComplete(Stage.MODEL);
        //}
        authorize(false, READ_WRITE_CONFIG);
        if (!affectsResourceRegistration) {
            takeWriteLock();
            affectsResourceRegistration = true;
        }
        return modelController.getRootRegistration().getSubModel(address);

    }

    @Override
    public ImmutableManagementResourceRegistration getResourceRegistration() {
        final PathAddress address = activeStep.address;
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null || currentStage == Stage.DONE) {
            throw MESSAGES.operationAlreadyComplete();
        }
        authorize(false, READ_CONFIG);
        ImmutableManagementResourceRegistration delegate = modelController.getRootRegistration().getSubModel(address);
        return delegate == null ? null : new DelegatingImmutableManagementResourceRegistration(delegate);
    }

    @Override
    public ImmutableManagementResourceRegistration getRootResourceRegistration() {
        authorize(false, READ_WRITE_CONFIG);
        ImmutableManagementResourceRegistration delegate = modelController.getRootRegistration();
        return delegate == null ? null : new DelegatingImmutableManagementResourceRegistration(delegate);
    }

    public ServiceRegistry getServiceRegistry(final boolean modify) throws UnsupportedOperationException {
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (! (!modify || currentStage == Stage.RUNTIME || currentStage == Stage.MODEL || currentStage == Stage.VERIFY || isRollingBack())) {
            throw MESSAGES.serviceRegistryRuntimeOperationsOnly();
        }
        authorize(false, modify ? READ_RUNTIME : READ_WRITE_RUNTIME);
        if (modify && !affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        return new OperationContextServiceRegistry(modelController.getServiceRegistry());
    }

    public ServiceController<?> removeService(final ServiceName name) throws UnsupportedOperationException {
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw MESSAGES.serviceRemovalRuntimeOperationsOnly();
        }
        authorize(true, WRITE_RUNTIME);
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        ServiceController<?> controller = modelController.getServiceRegistry().getService(name);
        if (controller != null) {
            doRemove(controller);
        }
        return controller;
    }

    @Override
    public boolean markResourceRestarted(PathAddress resource, Object owner) {
        if (restartedResources.containsKey(resource) ) {
            return false;
        }

        if (restartedResources == Collections.EMPTY_MAP) {
            restartedResources = new HashMap<PathAddress, Object>();
        }

        restartedResources.put(resource, owner);

        return true;
    }

    @Override
    public boolean revertResourceRestarted(PathAddress resource, Object owner) {
        if (restartedResources.get(resource) == owner) {
            restartedResources.remove(resource);
            return true;
        }

        return false;
    }

    public void removeService(final ServiceController<?> controller) throws UnsupportedOperationException {
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw MESSAGES.serviceRemovalRuntimeOperationsOnly();
        }
        authorize(true, WRITE_RUNTIME);
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        if (controller != null) {
            doRemove(controller);
        }
    }

    private void doRemove(final ServiceController<?> controller) {
        final Step removalStep = activeStep;
        controller.addListener(new AbstractServiceListener<Object>() {
            public void listenerAdded(final ServiceController<?> controller) {
                final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                synchronized (map) {
                    map.put(controller.getName(), controller);
                    controller.setMode(ServiceController.Mode.REMOVE);
                }
            }

            public void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case REMOVING_to_REMOVED:
                    case REMOVING_to_DOWN: {
                        final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                        synchronized (map) {
                            ServiceName name = controller.getName();
                            if (map.get(name) == controller) {
                                map.remove(name);
                                removalSteps.put(name, removalStep);
                                map.notifyAll();
                            }
                        }
                        break;
                    }
                }
            }
        });
    }

    public ServiceTarget getServiceTarget() throws UnsupportedOperationException {
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw MESSAGES.serviceTargetRuntimeOperationsOnly();
        }
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        return serviceTarget;
    }

    private void takeWriteLock() {
        if (lockStep == null) {
            if (currentStage == Stage.DONE) {
                throw MESSAGES.invalidModificationAfterCompletedStep();
            }
            try {
                modelController.acquireLock(operationId, respectInterruption, this);
                lockStep = activeStep;
            } catch (InterruptedException e) {
                cancelled = true;
                Thread.currentThread().interrupt();
                throw MESSAGES.operationCancelledAsynchronously();
            }
        }
    }

    private void acquireContainerMonitor() {
        if (containerMonitorStep == null) {
            if (currentStage == Stage.DONE) {
                throw MESSAGES.invalidModificationAfterCompletedStep();
            }
            modelController.acquireContainerMonitor();
            containerMonitorStep = activeStep;
        }
    }

    private void awaitContainerMonitor() {
        try {
            modelController.awaitContainerMonitor(respectInterruption);
        } catch (InterruptedException e) {
            if (currentStage != Stage.DONE && resultAction != ResultAction.ROLLBACK) {
                // We're not on the way out, so we've been cancelled on the way in
                cancelled = true;
            }
            Thread.currentThread().interrupt();
            throw MESSAGES.operationCancelledAsynchronously();
        }
    }

    public ModelNode readModel(final PathAddress requestAddress) {
        authorize(true, READ_CONFIG);
        final PathAddress address = activeStep.address.append(requestAddress);
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        Resource model = this.model;
        for (final PathElement element : address) {
            model = requireChild(model, element, address);
        }
        // recursively read the model
        return Resource.Tools.readModel(model);
    }

    public ModelNode readModelForUpdate(final PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.MODEL) {
            throw MESSAGES.stageAlreadyComplete(Stage.MODEL);
        }
        rejectUserDomainServerUpdates();
        authorize(true, READ_WRITE_CONFIG);
        if (!isModelAffected()) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.put(address, NULL);
        Resource model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw MESSAGES.cannotWriteTo("*");
            }
            if (! i.hasNext()) {
                final String key = element.getKey();
                if(! model.hasChild(element)) {
                    final PathAddress parent = address.subAddress(0, address.size() -1);
                    final Set<String> childrenNames = modelController.getRootRegistration().getChildNames(parent);
                    if(!childrenNames.contains(key)) {
                        throw MESSAGES.noChildType(key);
                    }
                    final Resource newModel = Resource.Factory.create();
                    model.registerChild(element, newModel);
                    model = newModel;
                } else {
                    model = requireChild(model, element, address);
                }
            } else {
                model = requireChild(model, element, address);
            }
        }
        if(model == null) {
            throw new IllegalStateException();
        }
        return model.getModel();
    }

    public Resource readResource(final PathAddress requestAddress) {
        return readResource(requestAddress, true);
    }

    public Resource readResource(final PathAddress requestAddress, final boolean recursive) {
        final PathAddress address = activeStep.address.append(requestAddress);
        return readResourceFromRoot(address, recursive);
    }

    public Resource readResourceFromRoot(final PathAddress address) {
        return readResourceFromRoot(address, true);
    }

    public Resource readResourceFromRoot(final PathAddress address, final boolean recursive) {
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        authorize(true, READ_CONFIG);
        Resource model = this.model;
        final Iterator<PathElement> iterator = address.iterator();
        while(iterator.hasNext()) {
            final PathElement element = iterator.next();
            // Allow wildcard navigation for the last element
            if(element.isWildcard() && ! iterator.hasNext()) {
                final Set<Resource.ResourceEntry> children = model.getChildren(element.getKey());
                if(children.isEmpty()) {
                    final PathAddress parent = address.subAddress(0, address.size() -1);
                    final Set<String> childrenTypes = modelController.getRootRegistration().getChildNames(parent);
                    if(! childrenTypes.contains(element.getKey())) {
                        throw ControllerMessages.MESSAGES.managementResourceNotFound(address);
                    }
                    // Return an empty model
                    return Resource.Factory.create();
                }
                model = Resource.Factory.create();
                for(final Resource.ResourceEntry entry : children) {
                    model.registerChild(entry.getPathElement(), entry);
                }
            } else {
                model = requireChild(model, element, address);
            }
        }
        if(recursive) {
            return model.clone();
        } else {
            final Resource copy = Resource.Factory.create();
            copy.writeModel(model.getModel());
            for(final String childType : model.getChildTypes()) {
                for(final Resource.ResourceEntry child : model.getChildren(childType)) {
                    copy.registerChild(child.getPathElement(), PlaceholderResource.INSTANCE);
                }
            }
            return copy;
        }
    }

    public Resource readResourceForUpdate(PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.MODEL) {
            throw MESSAGES.stageAlreadyComplete(Stage.MODEL);
        }
        rejectUserDomainServerUpdates();
        if (!isModelAffected()) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.put(address, NULL);
        Resource resource = this.model;
        for (PathElement element : address) {
            if (element.isMultiTarget()) {
                throw MESSAGES.cannotWriteTo("*");
            }
            resource = requireChild(resource, element, address);
        }
        return resource;
    }

    @Override
    public Resource getOriginalRootResource() {
        // TODO restrict
        return originalModel.clone();
    }

    public Resource createResource(PathAddress relativeAddress) {
        final Resource toAdd = Resource.Factory.create();
        addResource(relativeAddress, toAdd);
        return toAdd;
    }

    public void addResource(PathAddress relativeAddress, Resource toAdd) {
        final PathAddress absoluteAddress = activeStep.address.append(relativeAddress);
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.MODEL) {
            throw MESSAGES.stageAlreadyComplete(Stage.MODEL);
        }
        if (absoluteAddress.size() == 0) {
            throw MESSAGES.duplicateResourceAddress(absoluteAddress);
        }
        // Check for user updates to a domain server model
        rejectUserDomainServerUpdates();
        authorize(true, WRITE_CONFIG);
        if (!isModelAffected()) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.put(absoluteAddress, NULL);
        Resource model = this.model;
        final Iterator<PathElement> i = absoluteAddress.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw MESSAGES.cannotWriteTo("*");
            }
            if (! i.hasNext()) {
                final String key = element.getKey();
                if(model.hasChild(element)) {
                    throw MESSAGES.duplicateResourceAddress(absoluteAddress);
                } else {
                    final PathAddress parent = absoluteAddress.subAddress(0, absoluteAddress.size() -1);
                    final Set<String> childrenNames = modelController.getRootRegistration().getChildNames(parent);
                    if(!childrenNames.contains(key)) {
                        throw MESSAGES.noChildType(key);
                    }
                    model.registerChild(element, toAdd);
                    model = toAdd;
                }
            } else {
                model = model.getChild(element);
                if (model == null) {
                    PathAddress ancestor = PathAddress.EMPTY_ADDRESS;
                    for (PathElement pe : absoluteAddress) {
                        ancestor = ancestor.append(pe);
                        if (element.equals(pe)) {
                            break;
                        }
                    }
                    throw MESSAGES.resourceNotFound(ancestor, absoluteAddress);
                }
            }
        }
    }

    public Resource removeResource(final PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (currentStage != Stage.MODEL) {
            throw MESSAGES.stageAlreadyComplete(Stage.MODEL);
        }
        rejectUserDomainServerUpdates();
        authorize(true, READ_WRITE_CONFIG);
        if (!isModelAffected()) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.put(address, NULL);
        Resource model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw MESSAGES.cannotRemove("*");
            }
            if (! i.hasNext()) {
                model = model.removeChild(element);
            } else {
                model = requireChild(model, element, address);
            }
        }
        return model;
    }

    public void acquireControllerLock() {
        takeWriteLock();
    }

    public Resource getRootResource() {
        // TODO limit children
        authorize(false, READ_CONFIG);
        final Resource readOnlyModel = this.model;
        return readOnlyModel.clone();
    }

    public boolean isModelAffected() {
        return affectsModel.size() > 0;
    }

    public boolean isRuntimeAffected() {
        return affectsRuntime;
    }

    public boolean isResourceRegistryAffected() {
        return affectsResourceRegistration;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void report(final MessageSeverity severity, final String message) {
        try {
            if(messageHandler != null) {
                messageHandler.handleReport(severity, message);
            }
        } catch (Throwable t) {
            // ignored
        }
    }

    @Override
    void releaseStepLocks(AbstractOperationContext.Step step) {
        try {
            if (this.lockStep == step) {
                modelController.releaseLock(operationId);
                lockStep = null;
            }
            if (this.containerMonitorStep == step) {
                // Note: If we allow this thread to be interrupted, an op that has been cancelled
                // because of minor user impatience can release the controller lock while the
                // container is unsettled. OTOH, if we don't allow interruption, if the
                // container can't settle (e.g. a broken service is blocking in start()), the operation
                // will not be cancellable. I (BES 2012/01/24) chose the former as the lesser evil.
                // Any subsequent step that calls getServiceRegistry/getServiceTarget/removeService
                // is going to have to await the monitor uninterruptibly anyway before proceeding.
                try {
                    modelController.awaitContainerMonitor(true);
                }  catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    MGMT_OP_LOGGER.interruptedWaitingStability();
                }
            }
        } finally {
            if (this.containerMonitorStep == step) {
                modelController.releaseContainerMonitor();
                containerMonitorStep = null;
            }
        }
    }

    private static Resource requireChild(final Resource resource, final PathElement childPath, final PathAddress fullAddress) {
        if (resource.hasChild(childPath)) {
            return resource.requireChild(childPath);
        } else {
            PathAddress missing = PathAddress.EMPTY_ADDRESS;
            for (PathElement search : fullAddress) {
                missing = missing.append(search);
                if (search.equals(childPath)) {
                    break;
                }
            }
            throw ControllerMessages.MESSAGES.managementResourceNotFound(missing);
        }
    }

    @Override
    public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
        return modelController.resolveExpressions(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getAttachment(final AttachmentKey<V> key) {
        if (key == null) {
            throw MESSAGES.nullVar("key");
        }
        return key.cast(valueAttachments.get(key));
    }

    @Override
    public <V> V attach(final AttachmentKey<V> key, final V value) {
        if (key == null) {
            throw MESSAGES.nullVar("key");
        }
        return key.cast(valueAttachments.put(key, value));
    }

    @Override
    public <V> V attachIfAbsent(final AttachmentKey<V> key, final V value) {
        if (key == null) {
            throw MESSAGES.nullVar("key");
        }
        return key.cast(valueAttachments.putIfAbsent(key, value));
    }

    @Override
    public <V> V detach(final AttachmentKey<V> key) {
        if (key == null) {
            throw MESSAGES.nullVar("key");
        }
        return key.cast(valueAttachments.remove(key));
    }

    private void rejectUserDomainServerUpdates() {
        if (isModelUpdateRejectionRequired()) {
            ModelNode op = activeStep.operation;
            if (op.hasDefined(OPERATION_HEADERS) && op.get(OPERATION_HEADERS).hasDefined(CALLER_TYPE) && USER.equals(op.get(OPERATION_HEADERS, CALLER_TYPE).asString())) {
                throw ControllerMessages.MESSAGES.modelUpdateNotAuthorized(op.require(OP).asString(), PathAddress.pathAddress(op.get(OP_ADDR)));
            }
        }
    }

    private boolean isModelUpdateRejectionRequired() {
        if (requiresModelUpdateAuthorization == null) {
            requiresModelUpdateAuthorization = !isBooting() && getProcessType() == ProcessType.DOMAIN_SERVER;
        }
        return requiresModelUpdateAuthorization.booleanValue();
    }

    private void authorize(boolean allAttributes, Set<Action.ActionEffect> actionEffects) {
        AuthorizationResult accessResult = authorize(activeStep, false, ACCESS);
        if (accessResult.getDecision() == AuthorizationResult.Decision.DENY) {
            throw ControllerMessages.MESSAGES.managementResourceNotFound(activeStep.address);
        }
        AuthorizationResult authResult = authorize(activeStep, allAttributes, actionEffects);
        if (authResult.getDecision() == AuthorizationResult.Decision.DENY) {
            throw ControllerMessages.MESSAGES.managementResourceNotFound(activeStep.address);
        }
    }

    private AuthorizationResult authorize(Step step, boolean allAttributes, Set<Action.ActionEffect> actionEffects) {
        if (isBooting()) {
            return AuthorizationResult.PERMITTED;
        } else {
            AuthorizationResponse authResp = authorizations.get(step.operationId);
            if (authResp == null) {
                authResp = getBasicAuthorizationResponse(step);
            }
            if (authResp == null) {
                // Non-existent resource type or operation. This is permitted but will fail
                // later for reasons unrelated to authz
                return AuthorizationResult.PERMITTED;
            }
            for (Action.ActionEffect requiredEffect : actionEffects) {
                AuthorizationResult effectResult = authResp.getResourceResult(requiredEffect);
                if (effectResult == null) {
                    Action action = authResp.standardAction.limitAction(requiredEffect);
                    effectResult = modelController.getAuthorizer().authorize(getCaller(), callEnvironment, action, authResp.targetResource);
                    authResp.addResourceResult(requiredEffect, effectResult);
                }
                if (effectResult.getDecision() == AuthorizationResult.Decision.DENY) {
                    return effectResult;
                }
            }
            if (allAttributes) {
                ImmutableManagementResourceRegistration mrr = authResp.targetResource.getResourceRegistration();
                for (String attr : mrr.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
                    AuthorizationResult attrResult = authorize(step, attr, actionEffects);
                    if (attrResult.getDecision() == AuthorizationResult.Decision.DENY) {
                        return attrResult;
                    }
                }
                authResp.attributesComplete = true;
            }

            return AuthorizationResult.PERMITTED;
        }
    }

    private AuthorizationResult authorize(Step step, String attribute, Set<Action.ActionEffect> actionEffects) {
        if (isBooting()) {
            return AuthorizationResult.PERMITTED;
        } else {
            AuthorizationResponse authResp = authorizations.get(step.operationId);
            assert authResp != null : "perform resource authorization before attribute authorization";

            TargetAttribute targetAttribute = null;
            for (Action.ActionEffect actionEffect : actionEffects) {
                AuthorizationResult authResult = authResp.getAttributeResult(attribute, actionEffect);
                if (authResult == null) {
                    Action action = authResp.standardAction.limitAction(actionEffect);
                    if (targetAttribute == null) {
                        AttributeAccess attributeAccess = authResp.targetResource.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attribute);
                        targetAttribute = new TargetAttribute(attributeAccess, new ModelNode(), authResp.targetResource);
                    }
                    authResult = modelController.getAuthorizer().authorize(getCaller(), callEnvironment, action, targetAttribute);
                    authResp.addAttributeResult(attribute, actionEffect, authResult);
                }
                if (authResult.getDecision() == AuthorizationResult.Decision.DENY) {
                    return authResult;
                }
            }

            return AuthorizationResult.PERMITTED;
        }
    }

    private AuthorizationResponse getBasicAuthorizationResponse(Step step) {
        Caller caller = getCaller();
        ImmutableManagementResourceRegistration mrr = modelController.getRootRegistration().getSubModel(step.address);
        if (mrr == null) {
            return null;
        }
        Action action = getAuthorizationAction(mrr, step);
        if (action == null) {
            return null;
        }

        Resource resource = getAuthorizationResource(step.address);
        ProcessType processType = getProcessType();
        TargetResource targetResource = (processType == ProcessType.STANDALONE_SERVER || processType == ProcessType.EMBEDDED_SERVER)
                ? TargetResource.forStandalone(mrr, resource)
                : TargetResource.forDomain(mrr, resource, getServerGroups(step.address, resource), getHosts(step.address, resource));

        AuthorizationResponse result = new AuthorizationResponse(action, targetResource);
        AuthorizationResult simple = modelController.getAuthorizer().authorize(caller, callEnvironment, action, targetResource);
        if (simple.getDecision() == AuthorizationResult.Decision.PERMIT) {
            for (Action.ActionEffect actionEffect : action.getActionEffects()) {
                result.addResourceResult(actionEffect, simple);
            }
        }
        // else something was denied. Find out exactly what was denied when needed
        authorizations.put(step.operationId, result);
        return result;
    }

    private Set<String> getServerGroups(PathAddress address, Resource resource) {
        //TODO implement getServerGroups
//        throw new UnsupportedOperationException();
        return Collections.emptySet();
    }

    private Set<String> getHosts(PathAddress address, Resource resource) {
        //TODO implement getHosts
//        throw new UnsupportedOperationException();
        return Collections.emptySet();
    }

    private Caller getCaller() {
        // TODO implement
        return null;
    }

    private Resource getAuthorizationResource(PathAddress address) {
        Resource model = this.model;
        final Iterator<PathElement> iterator = address.iterator();
        while (iterator.hasNext()) {
            final PathElement element = iterator.next();
            // Allow wildcard navigation for the last element
            if(element.isWildcard()) {
                model = Resource.Factory.create();
                final Set<Resource.ResourceEntry> children = model.getChildren(element.getKey());
                for (final Resource.ResourceEntry entry : children) {
                    model.registerChild(entry.getPathElement(), entry);
                }
            } else if (model.hasChild(element)) {
                model = model.getChild(element);
            } else {
                return Resource.Factory.create();
            }

        }
        return model;
    }

    private Action getAuthorizationAction(ImmutableManagementResourceRegistration mrr, Step step) {
        OperationEntry entry = mrr.getOperationEntry(PathAddress.EMPTY_ADDRESS, step.operationId.name);
        if (entry == null) {
            return null;
        }
        return new Action(step.operation, entry);
    }

    class ContextServiceTarget implements ServiceTarget {

        private final ModelControllerImpl modelController;

        ContextServiceTarget(final ModelControllerImpl modelController) {
            this.modelController = modelController;
        }

        public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) {
            final ServiceBuilder<T> realBuilder = modelController.getServiceTarget().addServiceValue(name, value);
            return new ContextServiceBuilder<T>(realBuilder, name);
        }

        public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) {
            return addServiceValue(name, new ImmediateValue<Service<T>>(service));
        }

        public ServiceTarget addMonitor(final StabilityMonitor monitor) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addMonitors(final StabilityMonitor... monitors) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget removeMonitor(final StabilityMonitor monitor) {
            throw new UnsupportedOperationException();
        }

        public Set<StabilityMonitor> getMonitors() {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener<Object> listener) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener<Object>... listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final Collection<ServiceListener<Object>> listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget removeListener(final ServiceListener<Object> listener) {
            throw new UnsupportedOperationException();
        }

        public Set<ServiceListener<Object>> getListeners() {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final ServiceName dependency) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final ServiceName... dependencies) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final Collection<ServiceName> dependencies) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget removeDependency(final ServiceName dependency) {
            throw new UnsupportedOperationException();
        }

        public Set<ServiceName> getDependencies() {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget subTarget() {
            throw new UnsupportedOperationException();
        }

        public BatchServiceTarget batchTarget() {
            throw new UnsupportedOperationException();
        }
    }

    class ContextServiceBuilder<T> implements ServiceBuilder<T> {

        private final ServiceBuilder<T> realBuilder;
        private final ServiceName name;

        ContextServiceBuilder(final ServiceBuilder<T> realBuilder, final ServiceName name) {
            this.realBuilder = realBuilder;
            this.name = name;
        }

        public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
            realBuilder.addAliases(aliases);
            return this;
        }

        public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
            realBuilder.setInitialMode(mode);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final ServiceName... dependencies) {
            realBuilder.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final ServiceName... dependencies) {
            realBuilder.addDependencies(dependencyType, dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> dependencies) {
            realBuilder.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final Iterable<ServiceName> dependencies) {
            realBuilder.addDependencies(dependencyType, dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency) {
            realBuilder.addDependency(dependency);
            return this;
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency) {
            realBuilder.addDependency(dependencyType, dependency);
            return this;
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
            realBuilder.addDependency(dependency, target);
            return this;
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Injector<Object> target) {
            realBuilder.addDependency(dependencyType, dependency, target);
            return this;
        }

        public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            realBuilder.addDependency(dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            realBuilder.addDependency(dependencyType, dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
            realBuilder.addInjection(target, value);
            return this;
        }

        public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
            realBuilder.addInjectionValue(target, value);
            return this;
        }

        public ServiceBuilder<T> addInjection(final Injector<? super T> target) {
            realBuilder.addInjection(target);
            return this;
        }

        public ServiceBuilder<T> addMonitor(StabilityMonitor monitor) {
            realBuilder.addMonitor(monitor);
            return this;
        }

        public ServiceBuilder<T> addMonitors(StabilityMonitor... monitors) {
            realBuilder.addMonitors(monitors);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T> listener) {
            realBuilder.addListener(listener);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T>... listeners) {
            realBuilder.addListener(listeners);
            return this;
        }

        public ServiceBuilder<T> addListener(final Collection<? extends ServiceListener<? super T>> listeners) {
            realBuilder.addListener(listeners);
            return this;
        }

        public ServiceController<T> install() throws ServiceRegistryException, IllegalStateException {
            final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
            synchronized (map) {
                boolean intr = false;
                try {
                    while (map.containsKey(name)) {
                        try {
                            map.wait();
                        } catch (InterruptedException e) {
                            intr = true;
                            if (respectInterruption) {
                                cancelled = true;
                                throw MESSAGES.serviceInstallCancelled();
                            } // else keep waiting and mark the thread interrupted at the end
                        }
                    }

                    // If a step removed this ServiceName before, it's no longer responsible
                    // for any ill effect
                    removalSteps.remove(name);

                    return realBuilder.install();
                } finally {
                    if (intr) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /** Verifies that any service removals performed by this operation did not trigger a missing dependency */
    private class ServiceRemovalVerificationHandler implements OperationStepHandler {

        private final ContainerStateMonitor.ContainerStateChangeReport containerStateChangeReport;

        private ServiceRemovalVerificationHandler(ContainerStateMonitor.ContainerStateChangeReport containerStateChangeReport) {
            this.containerStateChangeReport = containerStateChangeReport;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {


            final Map<Step, Map<ServiceName, Set<ServiceName>>> missingByStep = new HashMap<Step, Map<ServiceName, Set<ServiceName>>>();
            // The realRemovingControllers map acts as the guard for the removalSteps map
            Object mutex = realRemovingControllers;
            synchronized (mutex) {
                for (Map.Entry<ServiceName, ContainerStateMonitor.MissingDependencyInfo> entry : containerStateChangeReport.getMissingServices().entrySet()) {
                    ContainerStateMonitor.MissingDependencyInfo missingDependencyInfo = entry.getValue();
                    Step removalStep = removalSteps.get(entry.getKey());
                    if (removalStep != null) {
                        Map<ServiceName, Set<ServiceName>> stepBadRemovals = missingByStep.get(removalStep);
                        if (stepBadRemovals == null) {
                            stepBadRemovals = new HashMap<ServiceName, Set<ServiceName>>();
                            missingByStep.put(removalStep, stepBadRemovals);
                        }
                        stepBadRemovals.put(entry.getKey(), missingDependencyInfo.getDependents());
                    }
                }
            }

            for (Map.Entry<Step, Map<ServiceName, Set<ServiceName>>> entry : missingByStep.entrySet()) {
                Step step = entry.getKey();
                if (!step.response.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION)) {
                    StringBuilder sb = new StringBuilder(MESSAGES.removingServiceUnsatisfiedDependencies());
                    for (Map.Entry<ServiceName, Set<ServiceName>> removed : entry.getValue().entrySet()) {
                        sb.append(MESSAGES.removingServiceUnsatisfiedDependencies(removed.getKey().getCanonicalName()));
                        boolean first = true;
                        for (ServiceName dependent : removed.getValue()) {
                            if (!first) {
                                sb.append(", ");
                            } else {
                                first = false;
                            }
                            sb.append(dependent);
                        }
                    }
                    step.response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).set(sb.toString());
                }
                // else a handler already recorded a failure; don't overwrite
            }

            if (!missingByStep.isEmpty() && context.isRollbackOnRuntimeFailure()) {
                context.setRollbackOnly();
            }
            context.completeStep(RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    private class OperationContextServiceRegistry implements ServiceRegistry {
        private final ServiceRegistry registry;

        public OperationContextServiceRegistry(ServiceRegistry registry) {
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServiceController<?> getRequiredService(ServiceName serviceName) throws ServiceNotFoundException {
            return new OperationContextServiceController(registry.getRequiredService(serviceName));
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServiceController<?> getService(ServiceName serviceName) {
            ServiceController<?> controller = registry.getService(serviceName);
            if (controller == null) {
                return null;
            }
            return new OperationContextServiceController(controller);
        }

        @Override
        public List<ServiceName> getServiceNames() {
            return registry.getServiceNames();
        }
    }

    private class OperationContextServiceController<S> implements ServiceController<S> {
        private final ServiceController<S> controller;

        public OperationContextServiceController(ServiceController<S> controller) {
            this.controller = controller;
        }

        public ServiceController<?> getParent() {
            return controller.getParent();
        }

        public ServiceContainer getServiceContainer() {
            return controller.getServiceContainer();
        }

        public Mode getMode() {
            return controller.getMode();
        }

        public boolean compareAndSetMode(Mode expected,
                org.jboss.msc.service.ServiceController.Mode newMode) {
            checkModeTransition(newMode);
            return controller.compareAndSetMode(expected, newMode);
        }

        public void setMode(Mode mode) {
            checkModeTransition(mode);
            controller.setMode(mode);
        }

        private void checkModeTransition(Mode mode) {
            if (mode == Mode.REMOVE) {
                throw MESSAGES.useOperationContextRemoveService();
            }
        }

        public org.jboss.msc.service.ServiceController.State getState() {
            return controller.getState();
        }

        public org.jboss.msc.service.ServiceController.Substate getSubstate() {
            return controller.getSubstate();
        }

        public S getValue() throws IllegalStateException {
            return controller.getValue();
        }

        public Service<S> getService() throws IllegalStateException {
            return controller.getService();
        }

        public ServiceName getName() {
            return controller.getName();
        }

        public ServiceName[] getAliases() {
            return controller.getAliases();
        }

        public void addListener(ServiceListener<? super S> serviceListener) {
            controller.addListener(serviceListener);
        }

        public void removeListener(ServiceListener<? super S> serviceListener) {
            controller.removeListener(serviceListener);
        }

        public StartException getStartException() {
            return controller.getStartException();
        }

        public void retry() {
            controller.retry();
        }

        public Set<ServiceName> getImmediateUnavailableDependencies() {
            return controller.getImmediateUnavailableDependencies();
        }

        public S awaitValue() throws IllegalStateException, InterruptedException {
            return controller.awaitValue();
        }

        public S awaitValue(long time, TimeUnit unit) throws IllegalStateException, InterruptedException, TimeoutException {
            return controller.awaitValue(time, unit);
        }
    }

    private static class AuthorizationResponse {

        private Map<Action.ActionEffect, AuthorizationResult> resourceResults = new HashMap<Action.ActionEffect, AuthorizationResult>();
        private Map<String, Map<Action.ActionEffect, AuthorizationResult>> attributeResults = new HashMap<String, Map<Action.ActionEffect, AuthorizationResult>>();
        private final TargetResource targetResource;
        private final Action standardAction;
        private volatile boolean attributesComplete = false;

        AuthorizationResponse(Action standardAction, TargetResource targetResource) {
            this.standardAction = standardAction;
            this.targetResource = targetResource;
        }

        AuthorizationResult getResourceResult(Action.ActionEffect actionEffect) {
            return resourceResults.get(actionEffect);
        }

        AuthorizationResult getAttributeResult(String attribute, Action.ActionEffect actionEffect) {
            Map<Action.ActionEffect, AuthorizationResult> attrResults = attributeResults.get(attribute);
            return attrResults == null ? null : attrResults.get(actionEffect);
        }

        private void addResourceResult(Action.ActionEffect actionEffect, AuthorizationResult result) {
            resourceResults.put(actionEffect, result);
        }

        private void addAttributeResult(String attribute, Action.ActionEffect actionEffect, AuthorizationResult result) {
            Map<Action.ActionEffect, AuthorizationResult> attrResults = attributeResults.get(attribute);
            if (attrResults == null) {
                attrResults = new HashMap<Action.ActionEffect, AuthorizationResult>();
                attributeResults.put(attribute, attrResults);
            }
            attrResults.put(actionEffect, result);
        }
    }
}
