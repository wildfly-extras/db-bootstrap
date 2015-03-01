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
import org.hibernate.Transaction;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extras.db_bootstrap.annotations.UpdateSchema;
import org.wildfly.extras.db_bootstrap.dbutils.HibernateTestUtil;
/**
 * @author Flemming Harms
 */
@BootstrapDatabase(priority = 97)
public class DatabaseBootstrapWarTester {

    @BootstrapSchema
    private void createSchema() {
        Session session = HibernateTestUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        HibernateTestUtil.createTestSchema(session);
        SQLQuery query = session.createSQLQuery("INSERT INTO PERSON (PersonId,Firstname) VALUES (4, 'Batman')");
        query.executeUpdate();
        tx.commit();
        session.close();
    }

    @UpdateSchema
    private void updateSchema() {
        Session session = HibernateTestUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        HibernateTestUtil.alterTestSchema(session, "sidekick");
        SQLQuery query = session.createSQLQuery("UPDATE PERSON SET sidekick ='Robin' WHERE personId = '4'");
        query.executeUpdate();
        tx.commit();
        session.close();
    }
}
