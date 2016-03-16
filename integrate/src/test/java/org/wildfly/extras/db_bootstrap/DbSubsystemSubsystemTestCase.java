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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

import java.io.IOException;

public class DbSubsystemSubsystemTestCase extends AbstractSubsystemBaseTest {

    public DbSubsystemSubsystemTestCase() {
        super(DbBootstrapExtension.SUBSYSTEM_NAME, new DbBootstrapExtension());
    }


    @Override
    @Test
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

    public void testSchemaOfSubsystemTemplates() {}

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "docs/schema/wildfly-bootstrap-1.0.xsd";
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("db_bootstrap-1.0.xml");
    }

}
