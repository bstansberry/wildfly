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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.UUID_KEY_GENERATOR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * This class extends the parser for CMP_1_0 schema with extensions of CMP_1_1 schema.
 * Extension for CMP_1_1 is an optional attribute {@link Attribute#JNDI_NAME JNDI_NAME} for uuid and hilo key generators.
 *
 * @author Manuel Fehlhammer
 */
class CmpSubsystem11Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    CmpSubsystem11Parser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(SUBSYSTEM, CmpExtension.SUBSYSTEM_NAME);

        requireNoAttributes(reader);
        final ModelNode cmpSubsystem = Util.createAddOperation(address);
        operations.add(cmpSubsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case KEY_GENERATORS: {
                    this.parseKeyGenerators(reader, operations, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseKeyGenerators(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final PathAddress parentAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case UUID: {
                    operations.add(parseUuid(reader, parentAddress));
                    break;
                }
                case HILO: {
                    operations.add(parseHilo(reader, parentAddress));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseHilo(final XMLExtendedStreamReader reader, final PathAddress parentAddress) throws XMLStreamException {
        final ModelNode op = Util.createAddOperation();
        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case JNDI_NAME: {
                    HiLoKeyGeneratorResourceDefinition.JNDI_NAME.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        op.get(OP_ADDR).set(parentAddress.append(HILO_KEY_GENERATOR, name).toModelNode());

        final EnumSet<Element> required = EnumSet.of(Element.DATA_SOURCE, Element.TABLE_NAME, Element.ID_COLUMN, Element.SEQUENCE_COLUMN, Element.SEQUENCE_NAME);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String value = reader.getElementText();
            final String tag = reader.getLocalName();
            final Element element = Element.forName(tag);
            required.remove(element);

            SimpleAttributeDefinition attribute = HiLoKeyGeneratorResourceDefinition.ATTRIBUTE_MAP.get(tag);
            if(attribute == null) {
                throw unexpectedElement(reader);
            }
            attribute.parseAndSetParameter(value, op, reader);
        }
        if(!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
        return op;
    }

    private ModelNode parseUuid(final XMLExtendedStreamReader reader, final PathAddress parentAddress) throws XMLStreamException {
        final ModelNode op = Util.createAddOperation();
        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case JNDI_NAME: {
                    UUIDKeyGeneratorResourceDefinition.JNDI_NAME.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        op.get(OP_ADDR).set(parentAddress.append(UUID_KEY_GENERATOR, name).toModelNode());

        requireNoContent(reader);
        return op;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(CmpExtension.LATEST_NAMESPACE, false);

        final ModelNode model = context.getModelNode();
        if (model.hasDefined(UUID_KEY_GENERATOR) || model.hasDefined(HILO_KEY_GENERATOR)) {
            writer.writeStartElement(Element.KEY_GENERATORS.getLocalName());

            if (model.hasDefined(UUID_KEY_GENERATOR)) {
                for (Property keyGen : model.get(UUID_KEY_GENERATOR).asPropertyList()) {
                    final String name = keyGen.getName();
                    final ModelNode keyGenModel = keyGen.getValue();
                    writeUuid(writer, name, keyGenModel);
                }
            }
            if (model.hasDefined(HILO_KEY_GENERATOR)) {
                for (Property keyGen : model.get(HILO_KEY_GENERATOR).asPropertyList()) {
                    final String name = keyGen.getName();
                    final ModelNode keyGenModel = keyGen.getValue();
                    writeHilo(writer, name, keyGenModel);
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeHilo(final XMLExtendedStreamWriter writer, final String name, final ModelNode model) throws XMLStreamException {
        writer.writeStartElement(Element.HILO.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        HiLoKeyGeneratorResourceDefinition.JNDI_NAME.marshallAsAttribute(model, writer);

        for(SimpleAttributeDefinition attribute : HiLoKeyGeneratorResourceDefinition.ATTRIBUTES) {
            if (!attribute.equals(HiLoKeyGeneratorResourceDefinition.JNDI_NAME))
                attribute.marshallAsElement(model, writer);
        }
        writer.writeEndElement();
    }

    private void writeUuid(final XMLExtendedStreamWriter writer, final String name, final ModelNode model) throws XMLStreamException {
        writer.writeStartElement(Element.UUID.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        UUIDKeyGeneratorResourceDefinition.JNDI_NAME.marshallAsAttribute(model, writer);
        writer.writeEndElement();
    }
}
