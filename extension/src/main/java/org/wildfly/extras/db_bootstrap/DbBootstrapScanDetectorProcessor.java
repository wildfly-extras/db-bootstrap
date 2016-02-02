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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoadException;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.scannotation.AnnotationDB;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extras.db_bootstrap.annotations.UpdateSchema;
import org.wildfly.extras.db_bootstrap.matchfilter.FilenameContainFilter;

/**
 * Reacts on the deployment process on the specified archives in the configuration. It scan all JAR archives for
 * {@link BootstrapDatabase} annotation to locate database bootstrapping classes. <br>
 * <br>
 *
 * By default it all children in the archive is added to a new class loader and passed to the Hibernate
 * {@link org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl} for creating a new {@link SessionFactory}
 *
 * @author Frank Vissing (frank.vissing@schneider-electric.com)
 * @author Flemming Harms (flemming.harms@gmail.com)
 * @author Nicky Moelholm (moelholm@gmail.com)
 * @author Rasmus Lund
 */
class DbBootstrapScanDetectorProcessor implements DeploymentUnitProcessor {

    /**
     * Defines the prefix of the system property names that can be used to set and/or override the hibernate properties defined
     * in user-space hibernate configuration files (referenced by the {@link BootstrapDatabase} annotation). <br>
     * <br>
     * For example: Setting the system property <code>dbbootstrap.foohibcfg.connection.url</code> will override any existing
     * hibernate configuration property <code>connection.url</code> in the hibernate configuration xml file referenced by the
     * {@link BootstrapDatabase} annotation with the name <code>foohibcfg</code>.
     */
    public static final String DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX = "dbbootstrap";
    private final String filename;
    private final FilenameContainFilter filterOnJarFilename;
    private final List<String> explicitlyListedDatabaseBootstrapperClassNames;

    public DbBootstrapScanDetectorProcessor(final String filename, final List<ModelNode> filterOnName, List<String> explicitlyListedDatabaseBootstrapperClassNames) {
        this.filename = filename;
        this.explicitlyListedDatabaseBootstrapperClassNames = explicitlyListedDatabaseBootstrapperClassNames;
        List<String> filter = new ArrayList<>(filterOnName.size());

        for (ModelNode modelNode : filterOnName) {
            filter.add("**/"+modelNode.asString());
        }

        this.filterOnJarFilename = new FilenameContainFilter(filter, VisitorAttributes.RECURSE);

        DbBootstrapLogger.ROOT_LOGGER.infof("Archive: [%s], jar-filter: %s, classes: %s", this.filename, filterOnJarFilename.toString(), explicitlyListedDatabaseBootstrapperClassNames.toString());
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName();

        if (isSubdeployment(deploymentUnit)) {
            return;
        }

        if (deploymentName.equals(filename)) {
            long before = System.currentTimeMillis();
            try {
                scanForAnnotationsAndProcessAnnotatedFiles(deploymentUnit);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e);
            }
            long duration = System.currentTimeMillis() - before;
            DbBootstrapLogger.ROOT_LOGGER.infof("Database bootstrapping took [%s] ms", duration);
        } else {
            DbBootstrapLogger.ROOT_LOGGER.tracef("%s did not match %s", filename, deploymentName);
        }
    }

    private void scanForAnnotationsAndProcessAnnotatedFiles(DeploymentUnit deploymentUnit) throws Exception {
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        DbBootstrapLogger.ROOT_LOGGER.tracef("match on %s", deploymentRoot.getRoot().getPathName());
        try {
            Set<URL> classLoaderurls = getJarList(deploymentRoot.getRoot(), false);
            if (classLoaderurls.size() > 0) {
                ClassLoader classLoader = addDynamicResources(classLoaderurls, deploymentUnit);
                boolean hasExplicitlyListedDatabaseBootstrapperClasses = !explicitlyListedDatabaseBootstrapperClassNames.isEmpty();
                if (hasExplicitlyListedDatabaseBootstrapperClasses) {
                    DbBootstrapLogger.ROOT_LOGGER.tracef("Using manually configured @%s classes: %s", BootstrapDatabase.class.getSimpleName(), explicitlyListedDatabaseBootstrapperClassNames);
                    processAnnotatedFiles(classLoader, explicitlyListedDatabaseBootstrapperClassNames);
                } else {
                    DbBootstrapLogger.ROOT_LOGGER.tracef("Scanning for @%s classes", BootstrapDatabase.class.getSimpleName());
                    scanForAnnotationsAndProcessAnnotatedFiles(classLoader, classLoaderurls, deploymentRoot.getRoot());
                }
            }
        } catch (Exception e) {
            DbBootstrapLogger.ROOT_LOGGER.error("Unable to process the internal jar files", e);
            throw e;
        }
    }

    private void scanForAnnotationsAndProcessAnnotatedFiles(final ClassLoader classLoader, Set<URL> classLoaderurls, VirtualFile deploymentRoot) throws Exception {
        AnnotationDB db;
        if (filterOnJarFilename == null) {
            db = scanForAnnotation(classLoaderurls);
        } else {
            db = scanForAnnotation(getJarList(deploymentRoot, true));
        }
        Set<String> databaseBoostrapperClasses = db.getAnnotationIndex().get(BootstrapDatabase.class.getName());
        processAnnotatedFiles(classLoader, databaseBoostrapperClasses);
    }

    private void processAnnotatedFiles(final ClassLoader classLoader, Collection<String> databaseBoostrapperClasses) throws Exception {
        if (databaseBoostrapperClasses != null && databaseBoostrapperClasses.size() > 0) {
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
     * Process a sorted list of bootstrap classes, by calling method's annotated with {@link BootstrapSchema} first and second
     * {@link UpdateSchema}.
     *
     * @param bootstrapMap
     * @param classLoader
     * @throws Exception
     */
    private void processAnnotatedClasses(final Map<BootstrapDatabase, Class<?>> bootstrapMap, final ClassLoader classLoader)
            throws Exception {
        List<Entry<BootstrapDatabase, Class<?>>> sortedList = new ArrayList<Entry<BootstrapDatabase, Class<?>>>(bootstrapMap
                .entrySet().size());
        sortedList.addAll(bootstrapMap.entrySet());

        Collections.sort(sortedList, new BootstrapperSorter());
        // Run all BootstrapSchema annotations
        for (Entry<BootstrapDatabase, Class<?>> entry : sortedList) {
            DbBootstrapLogger.ROOT_LOGGER.infof("Executing Bootstrap Schema method for %s %s",entry.getKey().name(),entry.getValue().getName());
            executeMethod(entry.getValue(), entry.getKey(), BootstrapSchema.class, classLoader);
        }

        // Run all UpgradeSchema annotations
        for (Entry<BootstrapDatabase, Class<?>> entry : sortedList) {
            DbBootstrapLogger.ROOT_LOGGER.infof("Executing Update Schema method for %s %s",entry.getKey().name(),entry.getValue().getName());
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
     * Execute the method annotated with specified class. If the annotated method has parameter signature {@link Session} it
     * will create a session a pass it as parameter.
     *
     * @param annotatedClazz - The bootstrap class to execute the method on
     * @param bootstrapDatabaseAnnotation - The configuration for creating a session
     * @param annotation - The annotation the method need to be annotated with for calling
     * @param classLoader - The class loader
     * @throws Exception
     */
    private <T extends Annotation> void executeMethod(final Class<?> annotatedClazz,
            final BootstrapDatabase bootstrapDatabaseAnnotation, final Class<T> annotation, final ClassLoader classLoader)
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
                    method.invoke(bootstrapClass);
                }
            }
        }
    }

    /**
     * Wrap transaction around the invoke with the {@link Session}, if any exception throw it roll back the tx otherwise commit
     * the tx;
     *
     * @param bootstrapDatabaseAnnotation - the boostrap configuration source
     * @param classLoader - The classloader to load the hibernate resources from
     * @param method - the method to invoke
     * @param bootstrapClass - the class to invoke the method on
     * @throws Exception
     */
    private void invokeWithSession(final BootstrapDatabase bootstrapDatabaseAnnotation, final ClassLoader classLoader,
            Method method, Object bootstrapClass) throws Exception {
        Session session = createSession(bootstrapDatabaseAnnotation, classLoader);
        Transaction tx = session.beginTransaction();
        try {
            method.invoke(bootstrapClass, session);
        } catch (Exception e) {
            DbBootstrapLogger.ROOT_LOGGER.error(String.format("Unable to invoke method %s ", method.getName()), e);
            tx.rollback();
            throw e;
        } finally {
            if (tx.getStatus() == TransactionStatus.ACTIVE) {
                tx.commit();
            }
            session.close();
            session.getSessionFactory().close();
        }
    }

    /**
     * Create a {@link Session} based on the provided configuration file.
     *
     * @param bootstrapDatabaseAnnotation - bootstrap configuration source
     * @param classLoader - class loader to use with the session factory
     * @return {@link Session}
     * @throws Exception
     */
    private Session createSession(final BootstrapDatabase bootstrapDatabaseAnnotation, final ClassLoader classLoader)
            throws Exception {
        URL resource = classLoader.getResource(bootstrapDatabaseAnnotation.hibernateCfg());
        DbBootstrapLogger.ROOT_LOGGER.tracef("Using hibernate configuration file %s",
                bootstrapDatabaseAnnotation.hibernateCfg());
        Configuration configuration = new Configuration();
        configuration.configure(resource); // configures settings from hibernate.cfg.xml
        configureSettingsFromSystemProperties(bootstrapDatabaseAnnotation, configuration);
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                configuration.getProperties()).build();
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
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
     * @param bootstrapDatabaseAnnotation - bootstrap configuration source
     * @param configuration - the runtime hibernate configuration object
     */
    private void configureSettingsFromSystemProperties(BootstrapDatabase bootstrapDatabaseAnnotation,
            Configuration configuration) {
        String propertyPrefix = String.format("%s.%s", DBBOOTSTRAP_SYSTEM_PROPERTY_PREFIX, bootstrapDatabaseAnnotation.name());
        DbBootstrapLogger.ROOT_LOGGER.tracef(
                "Searching for system properties with prefix %s to set and/or override hibernate configuration properties",
                propertyPrefix);
        for (Entry<Object, Object> entrySet : (System.getProperties().entrySet())) {
            if (entrySet.getKey().toString().startsWith(propertyPrefix)) {
                String hibernatePropertyName = entrySet.getKey().toString().replace(String.format("%s.", propertyPrefix), "");
                String oldHibernatePropertyValue = (configuration.getProperty(hibernatePropertyName) == null) ? " (New property)"
                        : String.format(" (Replacing existing property with old value=%s)",
                                configuration.getProperty(hibernatePropertyName));
                String newHibernatePropertyValue = entrySet.getValue().toString();
                DbBootstrapLogger.ROOT_LOGGER.tracef("Setting hibernate property: %s=%s%s", hibernatePropertyName,
                        newHibernatePropertyValue, oldHibernatePropertyValue);
                configuration.setProperty(hibernatePropertyName, newHibernatePropertyValue);
            }
        }
    }

    /**
     * Scan all jar files for annotations and build a internal map with the result.
     *
     * @param jarList
     * @throws IOException
     * @throws URISyntaxException
     */
    private AnnotationDB scanForAnnotation(Set<URL> jars) throws IOException, URISyntaxException {
        AnnotationDB db = new AnnotationDB();
        db.setScanClassAnnotations(true);
        db.setScanFieldAnnotations(false);
        db.setScanMethodAnnotations(false);
        db.setScanParameterAnnotations(false);
        long before = System.currentTimeMillis();
        DbBootstrapLogger.ROOT_LOGGER.tracef("Scanning for annotations started");
        db.scanArchives(jars.toArray(new URL[0]));
        long duration = System.currentTimeMillis() - before;
        DbBootstrapLogger.ROOT_LOGGER.tracef("Scanning for annotations finished - took [%s] ms", duration);
        return db;
    }

    /**
     * Return a list of URL's to all the children to the {@link VirtualFile}
     *
     * @param deploymentRoot
     * @param filter - true if the jar filename filter should be applied
     * @return A arrays of {@link URL}
     * @throws DeploymentUnitProcessingException
     * @throws IOException
     */
    private Set<URL> getJarList(final VirtualFile deploymentRoot, boolean filter) throws DeploymentUnitProcessingException,
            IOException {
        TreeSet<URL> uniqueArchiveUrls = new TreeSet<URL>(new UniqueArchiveUrlsComparator());
        List<VirtualFile> entries;

        if (filter) {
            entries = deploymentRoot.getChildrenRecursively(filterOnJarFilename);
        } else {
            entries = deploymentRoot.getChildrenRecursively();
        }
        for (VirtualFile virtualFile : entries) {
            try {
                URL url = VFSUtils.getRootURL(virtualFile);
                String lowerCasePathName = virtualFile.getPathName().trim().toLowerCase();
                if (lowerCasePathName.endsWith(".war")) {
                    uniqueArchiveUrls.add(new URL(url, "WEB-INF/classes/"));
                } else if (lowerCasePathName.endsWith(".jar")) {
                    uniqueArchiveUrls.add(url);
                }
            } catch (NullPointerException ignore) {
                // Happens if 'filename' refers to a dir or file, which is not an archive or which is not inside an archive.
                // These can safely be ignored.
            }
        }
        return uniqueArchiveUrls;
    }

    private static class UniqueArchiveUrlsComparator implements Comparator<URL> {
        @Override
        public int compare(URL o1, URL o2) {
            return o1.toString().compareTo(o2.toString());
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
    }

    /**
     * Add all jar files as dynamic resources to a new class load.
     *
     * @param urls - List of all the jar files to add
     * @param deploymentUnit - The deployment unit the resources should be added too
     * @throws DeploymentUnitProcessingException
     * @throws ModuleLoadException
     */
    private ClassLoader addDynamicResources(final Set<URL> urls, final DeploymentUnit deploymentUnit)
            throws DeploymentUnitProcessingException, ModuleLoadException {
        return URLClassLoader.newInstance(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    /**
     * As the top level module (including its submodules) gets scanned, we don't want to scan its submodules seperately
     * as well (would lead to double scanning)
     * @param deploymentUnit - the dployment unit to test
     * @return true if this is a sub deployment
     */
    private boolean isSubdeployment(DeploymentUnit deploymentUnit) {
        boolean currentModuleIsSubmoduleInAnotherModule = deploymentUnit.getParent() != null;
        if (currentModuleIsSubmoduleInAnotherModule) {
            DbBootstrapLogger.ROOT_LOGGER.tracef("Not registering module '%s' for scaning, as it is a submodule of '%s'",
                    deploymentUnit.getName(), deploymentUnit.getParent().getName());
            return true;
        }
        return false;
    }

}
