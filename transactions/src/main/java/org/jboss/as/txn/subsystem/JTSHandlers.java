/*
Copyright 2018 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.txn.subsystem.TransactionExtension.JTS_PATH;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.JTS;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * OperationStepHandler implementations related to handling JTS.
 *
 * @author Brian Stansberry
 */
final class JTSHandlers {

    private static final OperationStepHandler INTERNAL_ADD = new AddHandler(true);
    static final OperationStepHandler ADD = new AddHandler(false);

    private static final OperationStepHandler INTERNAL_REMOVE = new RemoveHandler(true);
    static final OperationStepHandler REMOVE = new RemoveHandler(false);

    private static final OperationStepHandler INTERNAL_WRITE = new WriteHandler(true);
    static final OperationStepHandler WRITE = new WriteHandler(false);

    static final OperationStepHandler READ = new ReadHandler();

    private static class AddHandler extends ModelOnlyAddStepHandler {

        private final boolean internal;

        AddHandler(boolean internal) {
            this.internal = internal;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            if (!internal) {
                // A external call to this op means any value stored in the
                // parent resource 'jts' attribute is overruled.
                // Add a step to clear any such value
                ModelNode writeOp = Util.getWriteAttributeOperation(context.getCurrentAddress().getParent(), JTS.getName(), true);
                context.addStep(writeOp, INTERNAL_WRITE, OperationContext.Stage.MODEL, true);
            }
        }
    }

    private static class RemoveHandler extends ModelOnlyRemoveStepHandler {

        private final boolean internal;

        RemoveHandler(boolean internal) {
            this.internal = internal;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            if (!internal) {
                // A external call to this op means any value stored in the
                // parent resource 'jts' attribute is overruled.
                // Add a step to clear any such value. It should execute immediately
                // in case this removal is part of a removal of the parent. In that
                // case clearing the value is pointless but we don't want it to fail
                // due to the parent already being gone.
                ModelNode writeOp = Util.getWriteAttributeOperation(context.getCurrentAddress().getParent(), JTS.getName(), false);
                context.addStep(writeOp, INTERNAL_WRITE, OperationContext.Stage.MODEL, true);
            }
        }
    }

    private static class WriteHandler extends ModelOnlyWriteAttributeHandler {

        private final boolean internal;

        WriteHandler(boolean internal) {
            this.internal = internal;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (internal) {
                // An internal call is just a clear of the attribute because the presence or absence
                // of the child is the controlling value
                ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
                model.get(JTS.getName()).set(new ModelNode());
            } else {
                super.execute(context, operation);
                // Resolve the value of the attribute and then based on that
                // add an internal add or remove step if needed.
                Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                boolean jts = JTS.resolveModelAttribute(context, resource.getModel()).asBoolean();
                if (resource.hasChild(TransactionExtension.JTS_PATH)) {
                    if (!jts) {
                        ModelNode removeOp = Util.createRemoveOperation(context.getCurrentAddress().append(JTS_PATH));
                        context.addStep(removeOp, INTERNAL_REMOVE, OperationContext.Stage.MODEL);
                    }
                } else if (jts) {
                    addJTSAddStep(context);
                }
            }
        }
    }

    private static class ReadHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
            ModelNode result = getJTSValue(Resource.Tools.readModel(resource));
            if (!result.isDefined() && operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).asBoolean(true)) {
                result = JTS.getDefaultValue();
            }
            context.getResult().set(result);
        }
    }

    static ModelNode getJTSValue(ModelNode subsystemRoot) {
        ModelNode attrVal = subsystemRoot.get(JTS.getName());
        if (!attrVal.isDefined() && subsystemRoot.has(JTS_PATH.getKeyValuePair())) {
            attrVal = ModelNode.TRUE;
        }
        return attrVal;
    }

    static void addJTSAddStep(OperationContext context) {
        ModelNode addOp = Util.createAddOperation(context.getCurrentAddress().append(JTS_PATH));
        context.addStep(addOp, INTERNAL_ADD, OperationContext.Stage.MODEL);

    }

    static void parseJTS(XMLExtendedStreamReader reader, PathAddress subsystemAddress, List<ModelNode> operations) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        requireNoContent(reader);

        operations.add(Util.createAddOperation(subsystemAddress.append(JTS_PATH)));
    }
}
