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

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extras.db_bootstrap.annotations.UpdateSchema;
import org.wildfly.extras.db_bootstrap.dbutils.HibernateTestUtil;
/**
 * @author Flemming Harms
 */
@BootstrapDatabase(hibernateCfg="META-INF/hibernate.cfg.xml", priority = 99)
public class DatabaseBootstrapTester {

    @BootstrapSchema
    public void createSchema(Session session) {
        HibernateTestUtil.createTestSchema(session);
        SQLQuery query = session.createSQLQuery("INSERT INTO PERSON (PersonId,Firstname) VALUES (1, 'John')");
        query.executeUpdate();
        session.flush();
    }

    @UpdateSchema
    public void updateSchema(Session session) {
        HibernateTestUtil.alterTestSchemaAddColumn(session,"Mobile");
        SQLQuery query = session.createSQLQuery("UPDATE PERSON SET Mobile ='555-1234' WHERE PersonId = '1'");
        query.executeUpdate();
        session.flush();
    }
}
