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
package org.wildfly.extension.db_bootstrap.databasebootstrap;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wildfly.extension.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extension.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extension.db_bootstrap.annotations.UpdateSchema;

@BootstrapDatabase(hibernateCfg="META-INF/hibernate.cfg.xml", priority = 99)
public class DatabaseBootstrapTester {

    @BootstrapSchema
    private void createSchema(Session session) {
        SQLQuery query = session.createSQLQuery("create table person (PersonId int, Firstname varchar(255))");
        query.executeUpdate();
        query = session.createSQLQuery("insert into person VALUES (1, 'John')");
        query.executeUpdate();
        session.flush();
    }

    @UpdateSchema
    private void updateSchema(Session session) {
        SQLQuery query = session.createSQLQuery("ALTER TABLE person ADD Lastname varchar(255)");
        query.executeUpdate();
        query = session.createSQLQuery("update person set Lastname ='Doe' where PersonId = '1'");
        query.executeUpdate();
        session.flush();

    }
}
