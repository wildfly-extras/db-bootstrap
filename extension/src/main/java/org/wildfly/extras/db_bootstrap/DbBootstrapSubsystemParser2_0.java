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
package org.wildfly.extras.db_bootstrap;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * @author Flemming Harms
 */
class DbBootstrapSubsystemParser2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final DbBootstrapSubsystemParser2_0 INSTANCE = new DbBootstrapSubsystemParser2_0();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(DbBootstrapRootResourceDefinition.INSTANCE)
                .addChild(builder(DbBootstrapDeploymentResourceDefinition.INSTANCE)
                        .addChild(builder(DbBootstrapScanDetectorResourceDefinition.INSTANCE)
                                .addAttribute(DbBootstrapScanDetectorResourceDefinition.FILENAME)
                                .addAttribute(DbBootstrapScanDetectorResourceDefinition.FILTER_ON_NAME))
                )
                .build();
    }

    public void writeContent(XMLExtendedStreamWriter writer,
                             SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(DbBootstrapRootResourceDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }

    /**
     * A reader which pulls an object out of some XML element and appends it to a provided object model.
     */
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list)
            throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}
