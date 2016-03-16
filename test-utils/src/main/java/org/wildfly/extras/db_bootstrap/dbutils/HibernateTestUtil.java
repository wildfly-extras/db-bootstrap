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
package org.wildfly.extras.db_bootstrap.dbutils;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;

public class HibernateTestUtil {

    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        try {
            if (sessionFactory == null) {
                BootstrapServiceRegistryBuilder serviceRegistryBuilder = new BootstrapServiceRegistryBuilder();
                serviceRegistryBuilder.with(HibernateTestUtil.class.getClassLoader());

                StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder(serviceRegistryBuilder.build())
                        .configure( "META-INF/hibernate.cfg.xml" )
                        .build();
                Metadata metadata = new MetadataSources( standardRegistry ).buildMetadata();

                sessionFactory = metadata.getSessionFactoryBuilder()
                        .build();
            }
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
        return sessionFactory;
    }

    public static void createTestSchema(Session session) {
        SQLQuery query = session.createSQLQuery("create table IF NOT EXISTS person (PersonId int, Firstname varchar(255))");
        query.executeUpdate();
    }

    public static void dropTestSchema(Session session) {
        SQLQuery query = session.createSQLQuery("drop table IF EXISTS person;");
        query.executeUpdate();
    }

    public static void alterTestSchemaAddColumn(Session session, String column) {
        SQLQuery query = session.createSQLQuery(String.format("ALTER TABLE person ADD IF NOT EXISTS %s varchar(255)", column));
        query.executeUpdate();
    }
}