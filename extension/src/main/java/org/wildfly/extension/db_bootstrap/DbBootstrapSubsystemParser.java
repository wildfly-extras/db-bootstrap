/**
 * Copyright (C) 2014 Umbrew (Flemming.Harms@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.db_bootstrap;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
class DbBootstrapSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final DbBootstrapSubsystemParser INSTANCE = new DbBootstrapSubsystemParser();

    private DbBootstrapSubsystemParser() {
    }

    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode model = context.getModelNode();
        ModelNode extension = model.get(DbBootstrapExtension.EXTENSION_TYPE);
        writer.writeStartElement(DbBootstrapExtension.BOOTSTRAP_DEPLOYMENT);

        for(Property p : extension.asPropertyList()){
            writer.writeStartElement(DbBootstrapExtension.SCAN);
            DbBootstrapDetectorResourceDefinition.FILENAME.marshallAsAttribute(p.getValue(), true, writer);
            DbBootstrapDetectorResourceDefinition.FILTER_ON_NAME.marshallAsAttribute(p.getValue(), true, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement(); //end the BOOTSTRAP_DEPLOYMENT
        writer.writeEndElement(); //end the subsystem
    }

    /**
     * A reader which pulls an object out of some XML element and appends it to a provided object model.
     */

    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(DbBootstrapExtension.SUBSYSTEM_PATH);
        list.add(Util.createAddOperation(address));
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DB_BOOTSTRAP_1_0: {
                    if (!reader.getLocalName().equals(DbBootstrapExtension.BOOTSTRAP_DEPLOYMENT)) {
                        throw unexpectedElement(reader);
                    }
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        ModelNode op = new ModelNode();
                        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
                        if(reader.isStartElement()){
                            if (!reader.getLocalName().equals(DbBootstrapExtension.SCAN)) {
                                throw unexpectedElement(reader);
                            }
                            String extension = null;
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (DbBootstrapExtension.FILENAME_ATTR.equals(reader.getAttributeLocalName(i))) {
                                    extension = reader.getAttributeValue(i);
                                    DbBootstrapDetectorResourceDefinition.FILENAME.parseAndSetParameter(reader.getAttributeValue(i), op, reader);
                                    DbBootstrapLogger.ROOT_LOGGER.tracef("%s:'%s'",DbBootstrapExtension.FILENAME_ATTR,reader.getAttributeValue(i));
                                } else if (DbBootstrapExtension.FILTER_ON_NAME_ATTR.equals(reader.getAttributeLocalName(i))) {
                                    DbBootstrapDetectorResourceDefinition.FILTER_ON_NAME.parseAndSetParameter(reader.getAttributeValue(i), op, reader);
                                    DbBootstrapLogger.ROOT_LOGGER.tracef("%s:'%s'",DbBootstrapExtension.FILTER_ON_NAME_ATTR,reader.getAttributeValue(i));
                                } else {
                                    reader.handleAny(list);
                                }
                            }
                            ParseUtils.requireNoContent(reader);
                            // use the extension to make path unique
                            PathAddress addr = PathAddress.pathAddress(DbBootstrapExtension.SUBSYSTEM_PATH,PathElement.pathElement(DbBootstrapExtension.EXTENSION_TYPE, extension));
                            op.get(ModelDescriptionConstants.OP_ADDR).set(addr.toModelNode());
                            list.add(op);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }
}

