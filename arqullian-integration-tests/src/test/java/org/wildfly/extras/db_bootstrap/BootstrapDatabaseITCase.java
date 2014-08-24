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

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.db_bootstrap.databasebootstrap.DatabaseBootstrapNoCfgFileTester;
import org.wildfly.extras.db_bootstrap.databasebootstrap.DatabaseBootstrapTester;

/**
 * @author Flemming Harms 
 */
@RunWith(Arquillian.class)
public class BootstrapDatabaseITCase {

    private static final String ARCHIVE_NAME = "bootstrap_test";

    private static final String hibernate_cfg_xml =
            "<!DOCTYPE hibernate-configuration SYSTEM "+
            "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"+
            "<hibernate-configuration>"+
            "   <session-factory>"+
            "        <property name=\"hibernate.connection.driver_class\">org.h2.Driver</property>"+
            "        <property name=\"hibernate.connection.url\">jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MVCC=true</property>"+
            "        <property name=\"hibernate.connection.username\">sa</property>"+
            "        <property name=\"hibernate.connection.password\">sa</property>"+
            "        <property name=\"javax.persistence.validation.mode\">none</property>"+
            "        <property name=\"hbm2ddl.auto\">validate</property>"+
            "    </session-factory>"+
            "</hibernate-configuration>";

    @Deployment(order = 1, name = "with-hibernate-cfg")
    public static Archive<?> deployWithHibernate() throws Exception {

        WebArchive ear = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap.jar");
        lib.addClasses(DatabaseBootstrapTester.class);
        addBaseResources(lib);
        ear.addAsLibraries(lib);
        return ear;
    }
    
    @Deployment(order = 2, name = "without-hibernate-cfg")
    public static Archive<?> deployWithOutHibernate() throws Exception {

        WebArchive ear = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + "-no-hibernate.war");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap-no-hibernate.jar");
        lib.addClasses(DatabaseBootstrapNoCfgFileTester.class);
        addBaseResources(lib);
        ear.addAsLibraries(lib);
        return ear;
    }

    private static void addBaseResources(JavaArchive lib) {
        lib.addClasses(BootstrapDatabaseITCase.class);
        lib.addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
        lib.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        lib.addAsManifestResource(new StringAsset(hibernate_cfg_xml), "hibernate.cfg.xml");
    }

    @PersistenceContext
    EntityManager em;

    @Test
    @OperateOnDeployment("with-hibernate-cfg")
    public void testRunBootstrapWithHibernate() throws Exception {
        Query query = em.createNativeQuery("select * from person where PersonId = '1'");
        List<?> resultList = query.getResultList();
        assertEquals(1, resultList.size());
        Object[] result = (Object[]) resultList.get(0);
        assertEquals("John",result[1]);
        assertEquals("Doe",result[2]);
    }
    
    @Test
    @OperateOnDeployment("without-hibernate-cfg")
    public void testRunBootstrapWithoutHibernate() throws Exception {
    }

}
