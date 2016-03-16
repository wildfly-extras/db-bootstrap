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

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
class DbBootstrapSubsystemParser extends PersistentResourceXMLParser {

    static final DbBootstrapSubsystemParser INSTANCE = new DbBootstrapSubsystemParser();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(DbBootstrapRootResourceDefinition.INSTANCE, Namespace.DB_BOOTSTRAP_1_0.getUriString())
            .addChild(builder(DbBootstrapDeploymentResourceDefinition.INSTANCE)
                .addChild(builder(DbBootstrapScanDetectorResourceDefinition.INSTANCE)
                    .addChild(builder(DbBootstrapClassResourceDefinition.INSTANCE)
                            .addAttribute(DbBootstrapClassResourceDefinition.CLASSNAME)
                     )
                    .addAttribute(DbBootstrapScanDetectorResourceDefinition.FILENAME)
                    .addAttribute(DbBootstrapScanDetectorResourceDefinition.FILTER_ON_NAME))
                    )
            .build();
    }


    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
