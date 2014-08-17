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

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
class DbBootstrapDetectorResourceDefinition extends SimpleResourceDefinition {
    static final DbBootstrapDetectorResourceDefinition INSTANCE = new DbBootstrapDetectorResourceDefinition();
    static final SimpleAttributeDefinition FILENAME = new SimpleAttributeDefinitionBuilder(DbBootstrapExtension.FILENAME_ATTR, ModelType.STRING, false).build();
    static final SimpleAttributeDefinition FILTER_ON_NAME = new SimpleAttributeDefinitionBuilder(DbBootstrapExtension.FILTER_ON_NAME_ATTR, ModelType.STRING, true).build();

    private DbBootstrapDetectorResourceDefinition() {
        super(DbBootstrapExtension.EXTENSION_PATH,
                DbBootstrapExtension.getResolver(DbBootstrapExtension.RESOLVER),
                new DbBootstrapDetectorAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(FILENAME, null);
        resourceRegistration.registerReadOnlyAttribute(FILTER_ON_NAME, null);
    }
}
