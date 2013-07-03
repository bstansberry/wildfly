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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHECK_RESOURCE_ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.ACCESS_CONTROL;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.INCLUDE_ALIASES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.LOCALE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.PROXIES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE_DEPTH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.UnauthorizedException;
import org.jboss.as.controller.access.Action.ActionEffect;
import org.jboss.as.controller.access.AuthorizationResponse;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.AuthorizationResult.Decision;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} querying the complete type description of a given model node.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ReadResourceDescriptionHandler implements OperationStepHandler {

    private static final SimpleAttributeDefinition INHERITED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INHERITED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(true))
            .build();

    private static final SimpleAttributeDefinition OPERATIONS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.OPERATIONS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_RESOURCE_DESCRIPTION_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(OPERATIONS, INHERITED, RECURSIVE, RECURSIVE_DEPTH, PROXIES, INCLUDE_ALIASES, ACCESS_CONTROL, LOCALE)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .build();

    static final OperationStepHandler INSTANCE = new ReadResourceDescriptionHandler();

    private ReadResourceDescriptionHandler() {
    }

    ReadResourceDescriptionAccessControlContext getAccessControlContext() {
        return null;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        ReadResourceDescriptionAccessControlContext accessControlContext = getAccessControlContext() == null ? new ReadResourceDescriptionAccessControlContext(null) : getAccessControlContext();
        if (getAccessControlContext() == null && address.isMultiTarget()) {
            executeMultiTarget(context, operation, accessControlContext);
        } else {
            doExecute(context, operation, accessControlContext);
        }
    }


    private void doExecute(OperationContext context, ModelNode operation, ReadResourceDescriptionAccessControlContext accessControlContext) throws OperationFailedException {
        if (accessControlContext.parentAddresses == null) {
            doExecuteInternal(context, operation, accessControlContext);
        } else {
            try {
                doExecuteInternal(context, operation, accessControlContext);
            } catch (NoSuchResourceException nsre) {
                context.getResult().set(new ModelNode());
                context.stepCompleted();
            } catch (UnauthorizedException ue) {
                context.getResult().set(new ModelNode());
                context.stepCompleted();
            }
        }
    }

    private void doExecuteInternal(final OperationContext context, final ModelNode operation, final ReadResourceDescriptionAccessControlContext accessControlContext) throws OperationFailedException {

        for (AttributeDefinition def : DEFINITION.getParameters()) {
            def.validateOperation(operation);
        }

        final String opName = operation.require(OP).asString();
        final PathAddress opAddr = PathAddress.pathAddress(operation.get(OP_ADDR));
        final int recursiveDepth = RECURSIVE_DEPTH.resolveModelAttribute(context, operation).asInt();
        final boolean recursive = recursiveDepth > 0 || RECURSIVE.resolveModelAttribute(context, operation).asBoolean();
        final boolean proxies = PROXIES.resolveModelAttribute(context, operation).asBoolean();
        final boolean ops = OPERATIONS.resolveModelAttribute(context, operation).asBoolean();
        final boolean aliases = INCLUDE_ALIASES.resolveModelAttribute(context, operation).asBoolean();
        final boolean inheritedOps = INHERITED.resolveModelAttribute(context, operation).asBoolean();
        final boolean includeAccess = ACCESS_CONTROL.resolveModelAttribute(context, operation).asBoolean();

        final ImmutableManagementResourceRegistration registry = getResourceRegistrationCheckForAlias(context, opAddr, accessControlContext);

        final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
        final Locale locale = GlobalOperationHandlers.getLocale(context, operation);

        final ModelNode nodeDescription = descriptionProvider.getModelDescription(locale);
        final Map<String, ModelNode> operations = ops ? new HashMap<String, ModelNode>() : null;
        final Map<PathElement, ModelNode> childResources = recursive ? new HashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

        List<PathAddress> localResourceAddresses = null;
        Map<PathAddress, ModelNode> localResourceAccessControlResults = null;
        if (includeAccess) {
            localResourceAddresses = getLocalResourceAddresses(context, operation, accessControlContext);
            localResourceAccessControlResults = new HashMap<PathAddress, ModelNode>();
        }


        // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
        // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works
        // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
        final ReadResourceDescriptionAssemblyHandler assemblyHandler = new ReadResourceDescriptionAssemblyHandler(nodeDescription, operations, childResources, localResourceAccessControlResults);
        context.addStep(assemblyHandler, OperationContext.Stage.MODEL, true);

        if (ops) {
            for (final Map.Entry<String, OperationEntry> entry : registry.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, inheritedOps).entrySet()) {
                if (entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                    if (context.getProcessType() != ProcessType.DOMAIN_SERVER || entry.getValue().getFlags().contains(OperationEntry.Flag.RUNTIME_ONLY)) {
                        final DescriptionProvider provider = entry.getValue().getDescriptionProvider();
                        operations.put(entry.getKey(), provider.getModelDescription(locale));
                    }
                }
            }
        }
        if (nodeDescription.hasDefined(ATTRIBUTES)) {
            for (final String attr : nodeDescription.require(ATTRIBUTES).keys()) {
                final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attr);
                // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                // be writable without a registered handler. This opens the possibility that out-of-date metadata
                // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                // an unexpected undefined value returned. But it removes the possibility of a
                // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                // in the valid attribute "foo" not being readable
                final AttributeAccess.AccessType accessType = access == null ? AttributeAccess.AccessType.READ_ONLY : access.getAccessType();
                final AttributeAccess.Storage storage = access == null ? AttributeAccess.Storage.CONFIGURATION : access.getStorageType();
                final ModelNode attrNode = nodeDescription.get(ATTRIBUTES, attr);
                //AS7-3085 - For a domain mode server show writable attributes as read-only
                String displayedAccessType =
                        context.getProcessType() == ProcessType.DOMAIN_SERVER && storage == AttributeAccess.Storage.CONFIGURATION ?
                                AttributeAccess.AccessType.READ_ONLY.toString() : accessType.toString();
                attrNode.get(ACCESS_TYPE).set(displayedAccessType);
                attrNode.get(STORAGE).set(storage.toString());
                if (accessType == AttributeAccess.AccessType.READ_WRITE) {
                    Set<AttributeAccess.Flag> flags = access.getFlags();
                    if (flags.contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
                        attrNode.get(RESTART_REQUIRED).set("all-services");
                    } else if (flags.contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)) {
                        attrNode.get(RESTART_REQUIRED).set("resource-services");
                    } else if (flags.contains(AttributeAccess.Flag.RESTART_JVM)) {
                        attrNode.get(RESTART_REQUIRED).set("jvm");
                    } else {
                        attrNode.get(RESTART_REQUIRED).set("no-services");
                    }
                }
            }
        }

        if (includeAccess) {
            checkResourceAccess(localResourceAccessControlResults, context, nodeDescription, localResourceAddresses, operations);
        }

        if (recursive) {
            for (final PathElement element : registry.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
                PathAddress relativeAddr = PathAddress.pathAddress(element);
                ImmutableManagementResourceRegistration childReg = registry.getSubModel(relativeAddr);

                boolean readChild = true;
                if (childReg.isRemote() && !proxies) {
                    readChild = false;
                }
                if (childReg.isAlias() && !aliases) {
                    readChild = false;
                }

                if (readChild) {
                    final int newDepth = recursiveDepth > 0 ? recursiveDepth - 1 : 0;
                    ModelNode rrOp = operation.clone();
                    try {
                        rrOp.get(OP_ADDR).set(PathAddress.pathAddress(opAddr, element).toModelNode());
                    } catch (Exception e) {
                        continue;
                    }
                    rrOp.get(RECURSIVE_DEPTH.getName()).set(newDepth);
                    ModelNode rrRsp = new ModelNode();
                    childResources.put(element, rrRsp);

                    final OperationStepHandler handler = childReg.isRemote() ? childReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, opName) :
                        new NestedReadResourceDescriptionHandler(new ReadResourceDescriptionAccessControlContext(localResourceAddresses, accessControlContext)) {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                doExecute(context, operation, accessControlContext);
                            }
                        };
                    context.addStep(rrRsp, rrOp, handler, OperationContext.Stage.MODEL, true);
                }
                //Add a "child" => undefined
                nodeDescription.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue());
            }
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {

                if (!context.hasFailureDescription()) {
                    for (final ModelNode value : childResources.values()) {
                        if (value.hasDefined(FAILURE_DESCRIPTION)) {
                            context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                            break;
                        }
                    }
                }
            }
        });
    }

    private ImmutableManagementResourceRegistration getResourceRegistrationCheckForAlias(OperationContext context, PathAddress opAddr, ReadResourceDescriptionAccessControlContext accessControlContext) {
        //The direct root registration is only needed if we are doing access-control=true
        final ImmutableManagementResourceRegistration root = context.getRootResourceRegistration();
        final ImmutableManagementResourceRegistration registry = root.getSubModel(opAddr);

        AliasEntry aliasEntry = registry.getAliasEntry();
        if (aliasEntry == null) {
            return registry;
        }
        //Get hold of the real registry if it was an alias
        return root.getSubModel(aliasEntry.convertToTargetAddress(opAddr));
    }


    private void executeMultiTarget(final OperationContext context, final ModelNode operation, final ReadResourceDescriptionAccessControlContext accessControlContext) {
        // Format wildcard queries as list
        final ModelNode result = context.getResult().setEmptyList();
        context.addStep(new ModelNode(), GlobalOperationHandlers.AbstractMultiTargetHandler.FAKE_OPERATION.clone(),
            new GlobalOperationHandlers.RegistrationAddressResolver(operation, result,
                new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        // step handler bypassing further wildcard resolution
                        doExecute(context, operation, accessControlContext);
                    }
                }), OperationContext.Stage.MODEL, true);
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (!context.hasFailureDescription()) {
                    String op = operation.require(OP).asString();
                    Map<PathAddress, ModelNode> failures = new HashMap<PathAddress, ModelNode>();
                    for (ModelNode resultItem : result.asList()) {
                        if (resultItem.hasDefined(FAILURE_DESCRIPTION)) {
                            final PathAddress failedAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                            ModelNode failedDesc = resultItem.get(FAILURE_DESCRIPTION);
                            failures.put(failedAddress, failedDesc);
                        }
                    }

                    if (failures.size() == 1) {
                        Map.Entry<PathAddress, ModelNode> entry = failures.entrySet().iterator().next();
                        if (entry.getValue().getType() == ModelType.STRING) {
                            context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtSingleAddress(op, entry.getKey(), entry.getValue().asString()));
                        } else {
                            context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtSingleAddressWithComplexFailure(op, entry.getKey()));
                        }
                    } else if (failures.size() > 1) {
                        context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtMultipleAddresses(op, failures.keySet()));
                    }
                }
            }
        });
    }

    private List<PathAddress> getLocalResourceAddresses(OperationContext context, ModelNode operation, ReadResourceDescriptionAccessControlContext accessControlContext){
        List<PathAddress> localResourceAddresses = null;
        PathAddress opAddr = PathAddress.pathAddress(operation.require(OP_ADDR));
        if (accessControlContext.parentAddresses == null) {
            if (opAddr.size() == 0) {
                return Collections.singletonList(PathAddress.EMPTY_ADDRESS);
            } else {
                localResourceAddresses = new ArrayList<>();
                getAllActualResourceAddresses(context, operation, localResourceAddresses, PathAddress.EMPTY_ADDRESS, opAddr, accessControlContext);
            }
        } else {
            localResourceAddresses = new ArrayList<>();
            for (PathAddress pathAddress : accessControlContext.parentAddresses) {
                getAllActualResourceAddresses(context, operation, localResourceAddresses, pathAddress, opAddr, accessControlContext);
            }
        }
        return localResourceAddresses;

    }

    private void getAllActualResourceAddresses(OperationContext context, ModelNode operation, List<PathAddress> addresses, PathAddress currentAddress, PathAddress opAddress, ReadResourceDescriptionAccessControlContext accessControlContext) {
        if (opAddress.size() == 0) {
            return;
        }

        final int length = currentAddress.size();
        final PathElement currentElement = opAddress.getElement(length);
        if (currentElement.isWildcard()) {

            Resource resource;
            try {
                resource = context.readResourceFromRoot(currentAddress);
            } catch (UnauthorizedException e) {
                //We could not read the resource, now check if that is due not to having access or read-config permissions
                AuthorizationResponse response = context.authorizeResource(false);
                if (response.getResourceResult(ActionEffect.ACCESS).getDecision() != Decision.PERMIT) {
                    //We do not have access permissions
                    return;
                }
                //We do not have read permissions, get the resource by other means
                //TODO revisit this, since resource.getChildXXX() should probably need some authorization as well
                resource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
                for (PathElement element : currentAddress) {
                    resource = resource.getChild(element);
                }
            }

            ImmutableManagementResourceRegistration directRegistration = context.getRootResourceRegistration().getSubModel(currentAddress);

            Map<String, Set<String>> childAddresses = GlobalOperationHandlers.getChildAddresses(context,
                                                                                    currentAddress,
                                                                                    directRegistration,
                                                                                    resource,
                                                                                    currentElement.getKey());
            Set<String> childNames = childAddresses.get(currentElement.getKey());
            if (childNames != null) {
                for (String name : childNames) {
                    PathAddress address = currentAddress.append(PathElement.pathElement(currentElement.getKey(), name));
                    if (addParentResource(context, addresses, address)) {
                        if (address.size() == opAddress.size()) {
                            addresses.add(address);
                        } else {
                            getAllActualResourceAddresses(context, operation, addresses, address, opAddress, accessControlContext);
                        }
                    }
                }
            }
        } else {
            PathAddress address = currentAddress.append(currentElement);
            if (addParentResource(context, addresses, address)) {
                if (address.size() == opAddress.size()) {
                    addresses.add(address);
                } else {
                    getAllActualResourceAddresses(context, operation, addresses, address, opAddress, accessControlContext);
                }
            }
        }
    }

    private void checkResourceAccess(final Map<PathAddress, ModelNode> accessControlResults, final OperationContext context,
            final ModelNode nodeDescription, final List<PathAddress> localResourceAddresses, Map<String, ModelNode> operations) {
        for (final PathAddress address : localResourceAddresses) {
            final ModelNode op = Util.createOperation(CHECK_RESOURCE_ACCESS, address);
            final ModelNode response = new ModelNode();
            context.addStep(response, op, new CheckResourceAccessHandler(address, accessControlResults, nodeDescription, operations), OperationContext.Stage.MODEL, true);
        }
    }

    private boolean addParentResource(OperationContext context, List<PathAddress> addresses, PathAddress address) {
        try {
            context.readResourceFromRoot(address);
        } catch (NoSuchResourceException nsre) {
            // Don't include the result
            return false;
        } catch (UnauthorizedException ue) {
            //We are not allowed to read it, but still we know it exists
        }
        return true;
    }



    /**
     *
     * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
     */
    static final class CheckResourceAccessHandler implements OperationStepHandler {

        static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.CHECK_RESOURCE_ACCESS, new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

        private final PathAddress address;
        private final Map<PathAddress, ModelNode> accessControlResults;
        private final ModelNode nodeDescription;
        private final Map<String, ModelNode> operations;

        CheckResourceAccessHandler(PathAddress address, Map<PathAddress, ModelNode> accessControlResults, ModelNode nodeDescription, Map<String, ModelNode> operations) {
            this.address = address;
            this.accessControlResults = accessControlResults;
            this.nodeDescription = nodeDescription;
            this.operations = operations;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode result = new ModelNode();
            AuthorizationResponse authResp = context.authorizeResource(true);
            if (authResp.getResourceResult(ActionEffect.ACCESS).getDecision() == Decision.DENY) {
                //We are not allowed to see the resource, delete it from the list of local resource addresses,
                //so that if this is a recursive invocation the children don't need to include this in the list of
                //parents
                accessControlResults.remove(address);
            } else {
                addResourceAuthorizationResults(result, authResp);
                accessControlResults.put(address, result);

                ModelNode attributes = new ModelNode();
                attributes.setEmptyObject();

                if (result.get(ActionEffect.READ_CONFIG.toString()).asBoolean()) {
                    for (Property attrProp : nodeDescription.require(ATTRIBUTES).asPropertyList()) {
                        if (authResp.getAttributeResult(attrProp.getName(), ActionEffect.ACCESS).getDecision() == Decision.PERMIT) {
                            ModelNode attributeResult = new ModelNode();
                            Storage storage = Storage.valueOf(attrProp.getValue().get(STORAGE).asString().toUpperCase());
                            addAttributeAuthorizationResults(attributeResult, attrProp.getName(), authResp, storage == Storage.RUNTIME);
                            attributes.get(attrProp.getName()).set(attributeResult);
                        }
                    }
                    result.get(ATTRIBUTES).set(attributes);

                    if (operations != null) {
                        ModelNode ops = new ModelNode();
                        ops.setEmptyObject();
                        for (Map.Entry<String, ModelNode> entry : operations.entrySet()) {

                            ModelNode operationToCheck = Util.createOperation(entry.getKey(), PathAddress.pathAddress(operation.require(OP_ADDR)));

                            AuthorizationResult authorization = context.authorizeOperation(operationToCheck, true);
                            if (authorization.getDecision() != Decision.PERMIT) {
                                continue;
                            }

                            ModelNode operationResult = new ModelNode();

                            addOperationAuthorizationResult(context, operationResult, operationToCheck, entry.getKey());

                            ops.get(entry.getKey()).set(operationResult);
                        }
                        result.get(ModelDescriptionConstants.OPERATIONS).set(ops);
                    }
                }
            }
            context.getResult().set(result);
            context.stepCompleted();
        }

        private void addResourceAuthorizationResults(ModelNode result, AuthorizationResponse authResp) {
            addResourceAuthorizationResult(result, authResp, ActionEffect.READ_CONFIG);
            addResourceAuthorizationResult(result, authResp, ActionEffect.WRITE_CONFIG);
            addResourceAuthorizationResult(result, authResp, ActionEffect.READ_RUNTIME);
            addResourceAuthorizationResult(result, authResp, ActionEffect.WRITE_RUNTIME);
        }

        private void addResourceAuthorizationResult(ModelNode result, AuthorizationResponse authResp, ActionEffect actionEffect) {
            AuthorizationResult authResult = authResp.getResourceResult(actionEffect);
            result.get(actionEffect.toString()).set(authResult.getDecision() == Decision.PERMIT);
        }

        private void addAttributeAuthorizationResults(ModelNode result, String attributeName, AuthorizationResponse authResp, boolean runtime) {
            if (runtime) {
                addAttributeAuthorizationResult(result, attributeName, authResp, ActionEffect.READ_RUNTIME);
                addAttributeAuthorizationResult(result, attributeName, authResp, ActionEffect.WRITE_RUNTIME);
            } else {
                addAttributeAuthorizationResult(result, attributeName, authResp, ActionEffect.READ_CONFIG);
                addAttributeAuthorizationResult(result, attributeName, authResp, ActionEffect.WRITE_CONFIG);
            }
        }

        private void addAttributeAuthorizationResult(ModelNode result, String attributeName, AuthorizationResponse authResp, ActionEffect actionEffect) {
            AuthorizationResult authorizationResult = authResp.getAttributeResult(attributeName, actionEffect);
            result.get(actionEffect.toString()).set(authorizationResult.getDecision() == Decision.PERMIT);
        }

        private void addOperationAuthorizationResult(OperationContext context, ModelNode result, ModelNode operation, String operationName) {
            AuthorizationResult authorizationResult = context.authorizeOperation(operation, false);
            result.get(EXECUTE).set(authorizationResult.getDecision() == Decision.PERMIT);
        }
    }

    /**
     * Assembles the response to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadResourceDescriptionAssemblyHandler implements OperationStepHandler {

        private final ModelNode nodeDescription;
        private final Map<String, ModelNode> operations;
        private final Map<PathElement, ModelNode> childResources;
        private final Map<PathAddress, ModelNode> localResourceAccessControl;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param nodeDescription basic description of the node, of its attributes and of its child types
         * @param operations      descriptions of the resource's operations
         * @param childResources  read-resource-description response from child resources, where the key is the PathAddress
         *                        relative to the address of the operation this handler is handling and the
         *                        value is the full read-resource response. Will not be {@code null}
         */
        private ReadResourceDescriptionAssemblyHandler(final ModelNode nodeDescription, final Map<String, ModelNode> operations,
                final Map<PathElement, ModelNode> childResources, Map<PathAddress, ModelNode> localResourceAccessControl) {
            this.nodeDescription = nodeDescription;
            this.operations = operations;
            this.childResources = childResources;
            this.localResourceAccessControl = localResourceAccessControl;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            for (Map.Entry<PathElement, ModelNode> entry : childResources.entrySet()) {
                final PathElement element = entry.getKey();
                final ModelNode value = entry.getValue();
                if (!value.has(FAILURE_DESCRIPTION)) {
                    nodeDescription.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue()).set(value.get(RESULT));
                } else if (value.hasDefined(FAILURE_DESCRIPTION)) {
                    context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                    break;
                }
            }

            if (operations != null) {
                for (Map.Entry<String, ModelNode> entry : operations.entrySet()) {
                    nodeDescription.get(OPERATIONS.getName(), entry.getKey()).set(entry.getValue());
                }
            }

            if (localResourceAccessControl != null) {
                ModelNode accessControl = new ModelNode();
                accessControl.setEmptyObject();
                for (Map.Entry<PathAddress, ModelNode> entry : localResourceAccessControl.entrySet()) {
                    accessControl.get(entry.getKey().toModelNode().asString()).set(entry.getValue());
                }
                nodeDescription.get(ACCESS_CONTROL.getName()).set(accessControl);
            }

            context.getResult().set(nodeDescription);
            context.stepCompleted();
        }
    }

    private final class ReadResourceDescriptionAccessControlContext {
        private final List<PathAddress> parentAddresses;

        ReadResourceDescriptionAccessControlContext(List<PathAddress> parentAddresses) {
            this.parentAddresses = parentAddresses;
        }

        ReadResourceDescriptionAccessControlContext(List<PathAddress> parentAddresses,
                ReadResourceDescriptionAccessControlContext parent) {
            this.parentAddresses = parentAddresses;
        }
    }

    private static class NestedReadResourceDescriptionHandler extends ReadResourceDescriptionHandler {
        final ReadResourceDescriptionAccessControlContext accessControlContext;

        public NestedReadResourceDescriptionHandler(ReadResourceDescriptionAccessControlContext accessControlContext) {
            this.accessControlContext = accessControlContext;
        }
    }
}
