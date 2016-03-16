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
package org.wildfly.extras.db_bootstrap.databasebootstrap;

import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extras.db_bootstrap.annotations.UpdateSchema;
/**
 * @author Nicky Moelholm (moelholm@gmail.com)
 */
@BootstrapDatabase(priority = 98)
public class DatabaseBootstrapWithTux {
    
    @BootstrapSchema
    public void createSchema() {
        PersonSchema.createTablePerson();
        PersonSchema.insertPerson(10, "Tux");
    }

    @UpdateSchema
    public void updateSchema() {
        PersonSchema.alterTablePersonAddColumnLastname();
        PersonSchema.setPersonLastName(10, "Thorvalds");
    }
}
