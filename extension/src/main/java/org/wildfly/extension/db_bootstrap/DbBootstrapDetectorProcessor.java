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
package org.wildfly.extension.db_bootstrap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.ModuleLoadException;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.scannotation.AnnotationDB;
import org.wildfly.extension.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extension.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extension.db_bootstrap.annotations.UpdateSchema;
import org.wildfly.extension.db_bootstrap.matchfilter.FilenameContainFilter;

/**
 * Reacts on the deployment process on the specified archives in the configuration. It scan all JAR archives for
 * {@link BootstrapDatabase} annotation to locate database bootstrapping classes. <br>
 * <br>
 *
 * By default it all children in the archive is added to a new class loader and passed to the Hibernate
 * {@link org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl} for creating a new {@link SessionFactory}
 *
 * @author Frank Vissing <frank.vissing@schneider-electric.com>
 * @author Flemming Harms <flemming.harms@gmail.com>
 * @author Nicky Moelholm <moelholm@gmail.com>
 */
public final class DbBootstrapDetectorProcessor implements DeploymentUnitProcessor {

    /**
     * Defines the prefix of the system property names that can be used to set and/or override the hibernate properties defined in user-space hibernate configuration files
     * (referenced by the {@link BootstrapDatabase} annotation).
     * <br><br>
     * For example: Setting the system property <code>dbbootstrap.foohibcfg.connection.url</code> will override any
     * existing hibernate configuration property <code>connection.url</code> in the hibernate configuration xml file referenced by the {@link BootstrapDatabase} annotation
     * with the name <code>foohibcfg</code>.
     */
    public static final String DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX = "dbbootstrap";
    private final String filename;
    private final FilenameContainFilter filterOnJarFilename;
    private final Set<String> parsedArchived;

    public DbBootstrapDetectorProcessor(String filename, String filterOnJarFilename) {
        this.filename = filename;
        if (!filterOnJarFilename.equals("undefined")) {
            this.filterOnJarFilename = new FilenameContainFilter(filterOnJarFilename, ".jar", VisitorAttributes.RECURSE);
        } else {
            this.filterOnJarFilename = null;
        }
        this.parsedArchived = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        DbBootstrapLogger.ROOT_LOGGER.tracef("Archive : %s jar-filter %s", this.filename, this.filterOnJarFilename);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();

        if (parsedArchived.contains(root.getParent().getPathName())) {
            return;
        }

        if (root.getPathName().contains(filename)) {
            parsedArchived.add(root.getPathName());
            DbBootstrapLogger.ROOT_LOGGER.tracef("match on %s", root.getPathName());
            try {
                URL[] classLoaderurls = getJarList(root, false);
                if (classLoaderurls.length > 0) {
                    final AnnotationDB db = new AnnotationDB();
                    ClassLoader classLoader = addDynamicResources(classLoaderurls, deploymentUnit);
                    if (filterOnJarFilename == null) {
                        scanForAnnotation(classLoaderurls, db);
                    } else {
                        scanForAnnotation(getJarList(root, true), db);
                    }
                    processAnnotatedFiles(db, classLoader);
                }
            } catch (Exception e) {
                DbBootstrapLogger.ROOT_LOGGER.error("Unable to process the internal jar files", e);
            }
        } else {
            DbBootstrapLogger.ROOT_LOGGER.tracef("%s did not match %s", filename, root.getPathName());
        }
    }

    private void processAnnotatedFiles(final AnnotationDB db, final ClassLoader classLoader) throws Exception {
        Map<String, Set<String>> annotationIndex = db.getAnnotationIndex();
        Set<String> databaseBoostrapperClasses = annotationIndex.get(BootstrapDatabase.class.getName());
        if (databaseBoostrapperClasses != null) {
            Map<BootstrapDatabase, Class<?>> bootstrapMap = new HashMap<BootstrapDatabase, Class<?>>(databaseBoostrapperClasses.size());
            for (String clazz : databaseBoostrapperClasses) {
                try {
                    Class<?> annotatedClazz = Class.forName(clazz, true, classLoader);
                    BootstrapDatabase dbBoostrapper = annotatedClazz.getAnnotation(BootstrapDatabase.class);
                    bootstrapMap.put(dbBoostrapper, annotatedClazz);
                } catch (ClassNotFoundException e) {
                    DbBootstrapLogger.ROOT_LOGGER.error("Unable to find class", e);
                }
            }
            processAnnotatedClasses(bootstrapMap, classLoader);
        } else {
            DbBootstrapLogger.ROOT_LOGGER.debug("@BootstrapDatabase annotation was not located in the archive");
        }
    }

    /**
     * Process a sorted list of bootstrap classes, by calling method's annotated with {@link BootstrapSchema} first and
     * second {@link UpdateSchema}.
     * @param bootstrapMap
     * @param classLoader
     * @throws Exception
     */
    private void processAnnotatedClasses(final Map<BootstrapDatabase, Class<?>> bootstrapMap, final ClassLoader classLoader) throws Exception {
        List<Entry<BootstrapDatabase, Class<?>>> sortedList = new ArrayList<Entry<BootstrapDatabase, Class<?>>>(bootstrapMap.entrySet().size());
        sortedList.addAll(bootstrapMap.entrySet());

        Collections.sort(sortedList, new BootstrapperSorter());
        // Run all BootstrapSchema annotations
        for (Entry<BootstrapDatabase, Class<?>> entry : sortedList) {
            executeMethod(entry.getValue(), entry.getKey(), BootstrapSchema.class, classLoader);
        }

        // Run all UpgradeSchema annotations
        for (Entry<BootstrapDatabase, Class<?>> entry : sortedList) {
            executeMethod(entry.getValue(), entry.getKey(), UpdateSchema.class, classLoader);
        }
    }

    private static class BootstrapperSorter implements Comparator<Entry<BootstrapDatabase, Class<?>>> {

        @Override
        public int compare(Entry<BootstrapDatabase, Class<?>> o1, Entry<BootstrapDatabase, Class<?>> o2) {
            int priority1 = o1.getKey().priority();
            int priority2 = o2.getKey().priority();
            if (priority1 == priority2) {
                return 0;
            }
            return (priority1 > priority2 ? -1 : 1);
        }
    }

    /**
     * Execute the method annotated with specified class. If the annotated method has parameter signature
     * {@link Session} it will create a session a pass it as parameter.
     *
     * @param annotatedClazz
     *            - The bootstrap class to execute the method on
     * @param bootstrapDatabaseAnnotation
     *            - The configuration for creating a session
     * @param annotation
     *            - The annotation the method need to be annotated with for calling
     * @param classLoader
     *            - The class loader
     * @throws Exception
     */
    private <T extends Annotation> void executeMethod(final Class<?> annotatedClazz, final BootstrapDatabase bootstrapDatabaseAnnotation, final Class<T> annotation, final ClassLoader classLoader)
            throws Exception {
        Method[] methods = annotatedClazz.getDeclaredMethods();
        for (Method method : methods) {
            method.setAccessible(true);
            if (method.getAnnotation(annotation) != null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean sessionParameter = false;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].equals(Session.class)) {
                        sessionParameter = true;
                        break;
                    }
                }
                Object bootstrapClass = annotatedClazz.newInstance();
                if (sessionParameter) {
                    invokeWithSession(bootstrapDatabaseAnnotation, classLoader, method, bootstrapClass);
                } else {
                    method.invoke(bootstrapClass, annotatedClazz);
                }
            }
        }
    }

    /**
     * Wrap transaction around the invoke with the {@link Session}, if any exception throw it roll back the tx
     * otherwise commit the tx;
     * @param bootstrapDatabaseAnnotation - the boostrap configuration source
     * @param classLoader - The classloader to load the hibernate resources from
     * @param method - the method to invoke
     * @param bootstrapClass - the class to invoke the method on
     * @throws Exception
     */
    private void invokeWithSession(final BootstrapDatabase bootstrapDatabaseAnnotation, final ClassLoader classLoader, Method method, Object bootstrapClass) throws Exception {
        Session session = createSession(bootstrapDatabaseAnnotation, classLoader);
        Transaction tx = session.beginTransaction();
        try {
            method.invoke(bootstrapClass, session);
        } catch (Exception e) {
           DbBootstrapLogger.ROOT_LOGGER.error(String.format("Unable to invoke method %s ",method.getName()),e);
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
     * Create a {@link Session} based on the provided configuration file.
     *
     * @param bootstrapDatabaseAnnotation
     *            - bootstrap configuration source
     * @param classLoader
     *            - class loader to use with the session factory
     * @return {@link Session}
     * @throws Exception
     */
    private Session createSession(final BootstrapDatabase bootstrapDatabaseAnnotation, final ClassLoader classLoader) throws Exception {
        URL resource = classLoader.getResource(bootstrapDatabaseAnnotation.hibernateCfg());
        DbBootstrapLogger.ROOT_LOGGER.tracef("Using hibernate configuration file %s", bootstrapDatabaseAnnotation.hibernateCfg());
        Configuration configuration = new Configuration();
        configuration.configure(resource); // configures settings from hibernate.cfg.xml
        configureSettingsFromSystemProperties(bootstrapDatabaseAnnotation, configuration);
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                configuration.getProperties()).build();
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        return sessionFactory.openSession();
    }

    /**
     * Loads all <code>dbbootstrap.[user-space-cfg-name-here].[hibernate-property-name-here]</code> properties from system properties. <br>
     * <br>
     * Any existing hibernate properties with the same name (<code>[hibernate-property-name-here]</code> in above example) will be replaced by the matching system property. <br>
     * <br>
     * @param bootstrapDatabaseAnnotation - bootstrap configuration source
     * @param configuration - the runtime hibernate configuration object
     */
    private void configureSettingsFromSystemProperties(BootstrapDatabase bootstrapDatabaseAnnotation, Configuration configuration) {
        String propertyPrefix = String.format("%s.%s", DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX, bootstrapDatabaseAnnotation.name());
        DbBootstrapLogger.ROOT_LOGGER.tracef("Searching for system properties with prefix %s to set and/or override hibernate configuration properties", propertyPrefix);
        for (Entry<Object, Object> entrySet : ((Set<Entry<Object, Object>>) System.getProperties().entrySet())) {
            if (entrySet.getKey().toString().startsWith(propertyPrefix)) {
                String hibernatePropertyName = entrySet.getKey().toString().replace(String.format("%s.", propertyPrefix), "");
                String oldHibernatePropertyValue = (configuration.getProperty(hibernatePropertyName) == null) ? " (New property)" : String.format(
                        " (Replacing existing property with old value=%s)", configuration.getProperty(hibernatePropertyName));
                String newHibernatePropertyValue = entrySet.getValue().toString();
                DbBootstrapLogger.ROOT_LOGGER.tracef("Setting hibernate property: %s=%s%s", hibernatePropertyName, newHibernatePropertyValue, oldHibernatePropertyValue);
                configuration.setProperty(hibernatePropertyName, newHibernatePropertyValue);
            }
        }
    }

    /**
     * Scan all jar files for annotations and build a internal map with the result.
     *
     * @param jarList
     * @param db
     * @throws IOException
     * @throws URISyntaxException
     */
    private void scanForAnnotation(URL[] jars, AnnotationDB db) throws IOException, URISyntaxException {
        db.setScanClassAnnotations(true);
        db.setScanFieldAnnotations(false);
        db.setScanMethodAnnotations(false);
        db.setScanParameterAnnotations(false);
        db.scanArchives(jars);
    }

    /**
     * Return a list of URL's to all the children to the {@link VirtualFile}
     *
     * @param deploymentRoot
     * @param filter
     *            - true if the jar filename filter should be applied
     * @return A arrays of {@link URL}
     * @throws DeploymentUnitProcessingException
     * @throws IOException
     */
    private URL[] getJarList(final VirtualFile deploymentRoot, boolean filter) throws DeploymentUnitProcessingException, IOException {
        List<VirtualFile> entries;
        if (filter) {
            entries = deploymentRoot.getChildrenRecursively(filterOnJarFilename);
        } else {
            entries = deploymentRoot.getChildrenRecursively();
        }

        int idx = 0;
        URL[] urls = new URL[entries.size()];
        for (VirtualFile virtualFile : entries) {
            urls[idx++] = VFSUtils.getRootURL(virtualFile);
        }
        return urls;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();
        parsedArchived.remove(root.getParent().getPathName());
    }

    /**
     * Add all jar files as dynamic resources to a new class load.
     *
     * @param urls
     *            - List of all the jar files to add
     * @param deploymentUnit
     *            - The deployment unit the resources should be added too
     * @throws DeploymentUnitProcessingException
     * @throws ModuleLoadException
     */
    private ClassLoader addDynamicResources(final URL[] urls, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException, ModuleLoadException {
        return URLClassLoader.newInstance(urls,getClass().getClassLoader());
    }

}
