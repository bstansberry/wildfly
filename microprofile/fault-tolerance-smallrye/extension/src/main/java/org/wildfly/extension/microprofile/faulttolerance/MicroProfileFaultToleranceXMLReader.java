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

package org.wildfly.extension.microprofile.faulttolerance;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parses singleton deployer subsystem configuration from XML.
 *
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceXMLReader implements XMLElementReader<List<ModelNode>> {

    @SuppressWarnings("unused")
    private final MicroProfileFaultToleranceSchema schema;

    public MicroProfileFaultToleranceXMLReader(MicroProfileFaultToleranceSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> modelNodes) throws XMLStreamException {
        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(MicroProfileFaultToleranceResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            throw ParseUtils.unexpectedElement(reader);
        }

        modelNodes.addAll(operations.values());
    }
}
