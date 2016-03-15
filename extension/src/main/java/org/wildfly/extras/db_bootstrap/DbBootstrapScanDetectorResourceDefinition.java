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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
class DbBootstrapScanDetectorResourceDefinition extends PersistentResourceDefinition {
    static final DbBootstrapScanDetectorResourceDefinition INSTANCE = new DbBootstrapScanDetectorResourceDefinition();
    static final SimpleAttributeDefinition FILENAME = new SimpleAttributeDefinitionBuilder(DbBootstrapExtension.FILENAME_ATTR, ModelType.STRING, false).build();
    static final StringListAttributeDefinition FILTER_ON_NAME = new StringListAttributeDefinition.Builder(DbBootstrapExtension.FILTER_ON_NAME_ATTR)
    .setAllowNull(true)
    .setAttributeMarshaller(new DefaultAttributeMarshaller() {
        @Override
        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws
                XMLStreamException {

            StringBuilder builder = new StringBuilder();
            if (resourceModel.hasDefined(attribute.getName())) {
                for (ModelNode p : resourceModel.get(attribute.getName()).asList()) {
                    builder.append(p.asString()).append(", ");
                }
            }
            if (builder.length() > 3) {
                builder.setLength(builder.length() - 2);
            }
            if (builder.length() > 0) {
                writer.writeAttribute(attribute.getXmlName(), builder.toString());
            }
        }
    })
    .build();

    private DbBootstrapScanDetectorResourceDefinition() {
        super(DbBootstrapExtension.SCAN_PATH,
                DbBootstrapExtension.getResolver(DbBootstrapExtension.RESOLVER),
                new DbBootstrapScanDetectorAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(FILENAME, FILTER_ON_NAME);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(DbBootstrapClassResourceDefinition.INSTANCE);
    }
}
