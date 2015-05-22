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
import org.wildfly.extras.db_bootstrap.dbutils.HibernateTestUtil;
/**
 * @author Nicky Moelholm (moelholm@gmail.com)
 */
public class PersonSchema {
    
   static void createTablePerson() {
        Session session = HibernateTestUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        HibernateTestUtil.createTestSchema(session);
        tx.commit();
        session.close();
   }

   static void alterTablePersonAddColumnLastname() {
         Session session = HibernateTestUtil.getSessionFactory().openSession();
         Transaction tx = session.beginTransaction();
         HibernateTestUtil.alterTestSchema(session,"Lastname");
         tx.commit();
         session.close();
   }

   static void insertPerson(int primaryKey, String firstName) {
       Session session = HibernateTestUtil.getSessionFactory().openSession();
       Transaction tx = session.beginTransaction();
       SQLQuery query = session.createSQLQuery(String.format("INSERT INTO PERSON (PersonId,Firstname) VALUES (%s, '%s')", primaryKey, firstName));
       query.executeUpdate();
       tx.commit();
       session.close();
   }
  
  static void setPersonLastName(int primaryKey, String lastName) {
      Session session = HibernateTestUtil.getSessionFactory().openSession();
      Transaction tx = session.beginTransaction();
      SQLQuery query = session.createSQLQuery(String.format("UPDATE PERSON SET Lastname = '%s' WHERE personId = %s", lastName, primaryKey));
      query.executeUpdate();
      tx.commit();
      session.close();
  }
  
}
