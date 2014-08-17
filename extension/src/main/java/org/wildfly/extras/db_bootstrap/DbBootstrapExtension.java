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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
public class DbBootstrapExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "db_bootstrap";
    static final String EXTENSION_TYPE ="extension";
    static final String BOOTSTRAP_DEPLOYMENT = "bootstrap-deployments";
    static final String SCAN = "scan";
    static final String FILENAME_ATTR = "filename";
    static final String FILTER_ON_NAME_ATTR = "filter-on-name";
    static final String RESORUCES = "resources";
    static final String RESOURCE = "resource";
    static final String FILTER_ATTR = "filter";
    static final String NAME_ATTR = "name";
    static final String PATH_ATTR = "path";
    static final String USE_PHYSICAL_CODE_SOURCE_ATTR = "use-physical-code-source";
    static final String RESOLVER ="config-bootstrap";

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement EXTENSION_PATH = PathElement.pathElement(EXTENSION_TYPE);
    private static final String RESOURCE_NAME = DbBootstrapExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        // resource resolver for properties file
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, DbBootstrapExtension.class.getClassLoader(), true, false);

    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DB_BOOTSTRAP_1_0.getUriString(), DbBootstrapSubsystemParser.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(DbBootstrapRootResource.INSTANCE);
        registration.registerSubModel(DbBootstrapDetectorResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(DbBootstrapSubsystemParser.INSTANCE);
    }

}