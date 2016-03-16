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
package org.wildfly.extras.db_bootstrap.providers;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.wildfly.extras.db_bootstrap.DbBootstrapLogger;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * @author Flemming Harms
 */
public class HibernateBootstrapProvider implements BootstrapProvider {

    /**
     * Defines the prefix of the system property names that can be used to set and/or override the hibernate properties defined
     * in user-space hibernate configuration files (referenced by the {@link BootstrapDatabase} annotation). <br>
     * <br>
     * For example: Setting the system property <code>dbbootstrap.foohibcfg.connection.url</code> will override any existing
     * hibernate configuration property <code>connection.url</code> in the hibernate configuration xml file referenced by the
     * {@link BootstrapDatabase} annotation with the name <code>foohibcfg</code>.
     */
    public static final String DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX = "dbbootstrap";

    /**
     * Wrap transaction around the invoke with the {@link Session}, if any exception throw it roll back the tx otherwise commit
     * the tx;
     * @see BootstrapProvider
     */
    @Override
    public void invokeWithParameters(String prefix, String configuration, Object bootstrapClass, ClassLoader classLoader, Method method) throws Exception {
        Session session = createSession(prefix, configuration, classLoader);
        Transaction tx = session.beginTransaction();
        try {
            method.invoke(bootstrapClass, session);
        } catch (Exception e) {
            DbBootstrapLogger.ROOT_LOGGER.error(String.format("Unable to invoke method %s ", method.getName()), e);
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.commit();
            }
            session.close();
            session.getSessionFactory().close();
        }
    }

    /**
     * Invoke the annotated method for the specified bootstrap class
     * @see BootstrapProvider
     */
    @Override
    public void invoke(Method method, Object bootstrapClass) throws InvocationTargetException, IllegalAccessException {
        method.invoke(bootstrapClass);
    }

    /**
     * Create a {@link Session} based on the provided configuration file.
     *
     * @param prefix - the prefix for using when loading properties for the session
     * @param hibernateCfg - The location of the hibernate configuration file.
     * @param classLoader - class loader to use with the session factory
     * @return {@link Session}
     * @throws Exception
     */
    private Session createSession(final String prefix, final String hibernateCfg, final ClassLoader classLoader)
            throws Exception {
        SessionFactory sessionFactory = null;
        DbBootstrapLogger.ROOT_LOGGER.tracef("Using hibernate configuration file %s", hibernateCfg);

        BootstrapServiceRegistryBuilder serviceRegistryBuilder = new BootstrapServiceRegistryBuilder();
        serviceRegistryBuilder.with(classLoader)
                .with(this.getClass().getClassLoader());

        StandardServiceRegistryBuilder standardRegistryBuilder = new StandardServiceRegistryBuilder(serviceRegistryBuilder.build())
                .configure(hibernateCfg);

        configureSettingsFromSystemProperties(prefix, standardRegistryBuilder);
        Metadata metadata = new MetadataSources(standardRegistryBuilder.build() ).buildMetadata();

        sessionFactory = metadata.getSessionFactoryBuilder()
                .build();

        return sessionFactory.openSession();
    }

    /**
     * Loads all <code>dbbootstrap.[user-space-cfg-name-here].[hibernate-property-name-here]</code> properties from system
     * properties. <br>
     * <br>
     * Any existing hibernate properties with the same name (<code>[hibernate-property-name-here]</code> in above example) will
     * be replaced by the matching system property. <br>
     * <br>
     *
     * @param configuration  - the runtime hibernate configuration object
     */
    private void configureSettingsFromSystemProperties(String prefix, StandardServiceRegistryBuilder configuration) {
        String propertyPrefix = String.format("%s.%s", DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX, prefix);
        DbBootstrapLogger.ROOT_LOGGER.tracef(
                "Searching for system properties with prefix %s to set and/or override hibernate configuration properties",
                propertyPrefix);
        for (Map.Entry<Object, Object> entrySet : (System.getProperties().entrySet())) {
            if (entrySet.getKey().toString().startsWith(propertyPrefix)) {
                    String hibernatePropertyName = entrySet.getKey().toString().replace(String.format("%s.", propertyPrefix), "");
                    String newHibernatePropertyValue = entrySet.getValue().toString();
                    DbBootstrapLogger.ROOT_LOGGER.tracef("Setting hibernate property: %s=%s", hibernatePropertyName,
                            newHibernatePropertyValue);
                    configuration.applySetting(hibernatePropertyName, newHibernatePropertyValue);
            }
        }
    }

}
