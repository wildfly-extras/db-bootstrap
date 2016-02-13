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
import org.wildfly.extras.db_bootstrap.databasebootstrap.DatabaseBootstrapWarTester;
import org.wildfly.extras.db_bootstrap.databasebootstrap.DatabaseBootstrapWithDuke;
import org.wildfly.extras.db_bootstrap.databasebootstrap.DatabaseBootstrapWithTux;
import org.wildfly.extras.db_bootstrap.databasebootstrap.PersonSchema;
import org.wildfly.extras.db_bootstrap.dbutils.HibernateTestUtil;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

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
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap.jar");
        lib.addClasses(DatabaseBootstrapTester.class);
        lib.addClasses(HibernateTestUtil.class);
        addBaseResources(lib);
        war.addAsLibraries(lib);
        return war;
    }
    
    @Deployment(order = 2, name = "without-hibernate-cfg")
    public static Archive<?> deployWithOutHibernate() throws Exception {
        return archiveWithoutHibernate("-no-hibernate.war", DatabaseBootstrapNoCfgFileTester.class);
    }

    @Deployment(order = 3, name = "with-explicitly-listed-classes")
    public static Archive<?> deployWithExplicitlyListedClasses() throws Exception {
        return archiveWithoutHibernate("-with-explicitly-listed-classes.war", DatabaseBootstrapWithDuke.class, DatabaseBootstrapWithTux.class);
    }

    private static WebArchive archiveWithoutHibernate(String suffix, Class<?> ... bootstrapClasses) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + suffix);
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap-no-hibernate.jar");
        lib.addClass(PersonSchema.class);
        lib.addClasses(bootstrapClasses);
        lib.addClasses(HibernateTestUtil.class);
        addBaseResources(lib);
        war.addAsLibraries(lib);
        return war;
    }
    
    @Deployment(order = 4, name = "dummy-exploded")
    public static Archive<?> deployDummyExploded() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + "-dummy-exploded.ear");
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap-dummy.jar");
        addBaseResources(lib);
        ear.addAsLibraries(lib);
        return ear;
    }
    
    @Deployment(order = 5, name = "war-inside-ear")
    public static Archive<?> deployWarInsideEar() throws Exception {
        WebArchive warLib = ShrinkWrap.create(WebArchive.class, "bootstrap.war");
        warLib.addClasses(DatabaseBootstrapWarTester.class);
        warLib.addClasses(HibernateTestUtil.class);
        warLib.addClasses(BootstrapDatabaseITCase.class);
        warLib.addAsWebInfResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
        warLib.addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("classes/META-INF/beans.xml"));
        warLib.addAsWebInfResource(new StringAsset(hibernate_cfg_xml), "classes/META-INF/hibernate.cfg.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + "-war-inside-ear.ear");
        ear.addAsModule(warLib);
        return ear;
    }
    
    private static void addBaseResources(JavaArchive lib) {
        lib.addClasses(BootstrapDatabaseITCase.class);
        lib.addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
        lib.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        lib.addAsManifestResource(new StringAsset(hibernate_cfg_xml), "hibernate.cfg.xml");
    }

    @PersistenceContext(unitName="test")
    EntityManager testEm;

    @Test
    @OperateOnDeployment("with-hibernate-cfg")
    public void testRunBootstrapWithHibernate() throws Exception {
        Query query = testEm.createNativeQuery("select * from person where PersonId = '1'");
        List<Object> result = Arrays.asList((Object[]) query.getSingleResult());
        assertThat(result, hasItems((Object)"John","555-1234"));
    }
    
    @Test
    @OperateOnDeployment("without-hibernate-cfg")
    public void testRunBootstrapWithoutHibernate() throws Exception {
        Query query = testEm.createNativeQuery("select * from person where PersonId = '2'");
        List<Object> result = Arrays.asList((Object[]) query.getSingleResult());
        assertThat(result, hasItems((Object)"Jane","Way"));
    }
    
    @Test
    @OperateOnDeployment("dummy-exploded")
    public void testRunBootstrapWithExplodedEARfile() throws Exception {
        Query query = testEm.createNativeQuery("select * from person where PersonId = '3'");
        List<Object> result = Arrays.asList((Object[]) query.getSingleResult());
        assertThat(result, hasItems((Object)"Superman","Earth"));
    }
    
    @Test
    @OperateOnDeployment("war-inside-ear")
    public void testRunBootstrapWithWarInsideEARfile() throws Exception {
        Query query = testEm.createNativeQuery("select * from person where PersonId = '4'");
        List<Object> result = Arrays.asList((Object[]) query.getSingleResult());
        assertThat(result, hasItems((Object)"Batman","Robin"));
    }

}
