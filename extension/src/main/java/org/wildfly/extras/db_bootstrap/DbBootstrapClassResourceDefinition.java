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
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Nicky Moelholm (moelholm@gmail.com)
 */
class DbBootstrapClassResourceDefinition extends PersistentResourceDefinition {
    static final DbBootstrapClassResourceDefinition INSTANCE = new DbBootstrapClassResourceDefinition();
    static final SimpleAttributeDefinition CLASSNAME = new SimpleAttributeDefinitionBuilder(DbBootstrapExtension.CLASSNAME_ATTR, ModelType.STRING, false).build();

    private DbBootstrapClassResourceDefinition() {
        super(DbBootstrapExtension.CLASS_PATH,
                DbBootstrapExtension.getResolver(),
                new DbBootstrapClassAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.<AttributeDefinition>asList(CLASSNAME);
    }

}
