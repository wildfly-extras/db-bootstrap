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

import org.hibernate.Session;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.ModuleLoadException;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VirtualFileFilterWithAttributes;
import org.jboss.vfs.VisitorAttributes;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase;
import org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema;
import org.wildfly.extras.db_bootstrap.annotations.UpdateSchema;
import org.wildfly.extras.db_bootstrap.matchfilter.FilenameContainFilter;
import org.wildfly.extras.db_bootstrap.providers.BootstrapProvider;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Reacts on the deployment process on the specified archives in the configuration. It scan all JAR archives for
 * {@link org.wildfly.extras.db_bootstrap.annotations.BootstrapDatabase} annotation to locate database
 * bootstrapping classes. <br>
 *
 * By default it all children in the archive is added to a new class loader and passed to the Hibernate
 * {@link org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl} for creating a new
 * {@link org.hibernate.SessionFactory}
 *
 * @author Frank Vissing (frank.vissing@schneider-electric.com)
 * @author Flemming Harms (flemming.harms@gmail.com)
 * @author Nicky Moelholm (moelholm@gmail.com)
 * @author Rasmus Lund
 */
class DbBootstrapScanDetectorProcessor implements DeploymentUnitProcessor {

    private final String filename;
    private final BootstrapProvider provider;
    private final FilenameContainFilter filterOnJarFilename;

    public DbBootstrapScanDetectorProcessor(final String filename, final List<ModelNode> filterOnName, final BootstrapProvider provider) {
        this.filename = filename;
        this.provider = provider;
        List<String> filter = new ArrayList<>(filterOnName.size());

        for (ModelNode modelNode : filterOnName) {
            filter.add("**/"+modelNode.asString());
        }

        this.filterOnJarFilename = new FilenameContainFilter(filter, VisitorAttributes.RECURSE);

        if (!filterOnName.isEmpty()) {
            DbBootstrapLogger.ROOT_LOGGER.infof("Archive : %s jar-filter %s", this.filename, filterOnJarFilename.toString());
        }
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName();

        if (isSubdeployment(deploymentUnit)) {
            deploymentName = deploymentUnit.getParent().getName();
        }

        if (deploymentName.equals(filename)) {
            long before = System.currentTimeMillis();
            try {
                scanForAnnotationsAndProcessAnnotatedFiles(deploymentUnit,filterOnJarFilename);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e);
            }
            long duration = System.currentTimeMillis() - before;
            DbBootstrapLogger.ROOT_LOGGER.infof("Database bootstrapping took [%s] ms", duration);
        } else {
            DbBootstrapLogger.ROOT_LOGGER.tracef("%s did not match %s", filename, deploymentName);
        }
    }

    private void scanForAnnotationsAndProcessAnnotatedFiles(DeploymentUnit deploymentUnit, VirtualFileFilterWithAttributes filterOnJarFilename) {
        DotName dotName = DotName.createSimple(BootstrapDatabase.class.getName());
        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        List<AnnotationInstance> indexAnnotations = index.getAnnotations(dotName);
        if (indexAnnotations.isEmpty()) {
            return;
        }

        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();

        DbBootstrapLogger.ROOT_LOGGER.tracef("match on %s", root.getPathName());
        try {
            Set<URL> classLoaderurls = getJarList(root, false, filterOnJarFilename);
            if (classLoaderurls.size() > 0) {
                ClassLoader classLoader = addDynamicResources(classLoaderurls, deploymentUnit);
                processAnnotatedClasses(indexAnnotations, classLoader);
             }
        } catch (Exception e) {
            DbBootstrapLogger.ROOT_LOGGER.error("Unable to process the internal jar files", e);
        }
    }


    /**
     * Process a sorted list of bootstrap classes, by calling method's annotated with
     * {@link org.wildfly.extras.db_bootstrap.annotations.BootstrapSchema} first and second
     * {@link org.wildfly.extras.db_bootstrap.annotations.UpdateSchema}.
     *
     * @param bootstrapList - List of all the scanned AnnotationInstance
     * @param classLoader
     * @throws Exception
     */
    private void processAnnotatedClasses(final List<AnnotationInstance> bootstrapList, final ClassLoader classLoader)
            throws Exception {

        DotName bootstrapSchemaMethod = DotName.createSimple(BootstrapSchema.class.getName());
        DotName updateSchemaMethod = DotName.createSimple(UpdateSchema.class.getName());

        bootstrapList
                .stream()
                .sorted(new BootstrapperSorter())
                .forEachOrdered(a -> {
                    try {
                        DbBootstrapLogger.ROOT_LOGGER.infof("Executing Bootstrap Schema method for %s",a.toString());
                        executeMethod(a, bootstrapSchemaMethod, classLoader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        bootstrapList
                .stream()
                .sorted(new BootstrapperSorter())
                .forEachOrdered(a -> {
                    try {
                        DbBootstrapLogger.ROOT_LOGGER.infof("Executing Update Schema method for %s",a.toString());
                        executeMethod(a, updateSchemaMethod, classLoader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static class BootstrapperSorter implements Comparator<AnnotationInstance> {

        @Override
        public int compare(AnnotationInstance o1, AnnotationInstance o2) {
            int priority1 = o1.value("priority").asInt();
            int priority2 = o2.value("priority").asInt();
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
     * @param annotatedInstance - The annotated class
     * @param name - The name of the method annotated
     * @param classLoader - The class loader
     * @throws Exception
     */
    private <T extends Annotation> void executeMethod(final AnnotationInstance annotatedInstance, final DotName name, final ClassLoader classLoader)
            throws Exception {
        ClassInfo classInfo = (ClassInfo) annotatedInstance.target();
        classInfo.annotations().entrySet()
                .stream()
                .filter(annotationMap -> annotationMap.getKey().equals(name))
                .flatMap(annotationInstanceList -> annotationInstanceList.getValue().stream())
                .filter(methodInstance -> methodInstance.target() instanceof MethodInfo)
                .map(methodTarget -> methodTarget.target())
                .map(MethodInfo.class::cast)
                .forEach(t -> {
                    executeMethodWithParameters(annotatedInstance, classLoader,  t, (ClassInfo) annotatedInstance.target());

                });
    }

    private void executeMethodWithParameters(AnnotationInstance annotatedInstance, ClassLoader classLoader, MethodInfo invoke, ClassInfo annotationTarget) {
        String className = annotationTarget.name().toString();
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            Object bootstrapClass = clazz.newInstance();

            DotName sessionName = DotName.createSimple("org.hibernate.Session");

            boolean session = Stream.of(invoke.args())
                    .filter(types -> types.name().equals(sessionName))
                    .findFirst()
                    .isPresent();

            if (session) {
                Method method = clazz.getMethod(invoke.name().toString(),Session.class);
                String hibernateCfg = annotatedInstance.value("hibernateCfg").asString();
                String prefix = Optional.ofNullable(annotatedInstance.value("name"))
                        .map(AnnotationValue::asString)
                        .orElse("");
                provider.invokeWithParameters(prefix, hibernateCfg, bootstrapClass, classLoader, method);
            } else {
                Method method = clazz.getMethod(invoke.name().toString());
                provider.invoke(method,bootstrapClass);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a list of URL's to all the children to the {@link VirtualFile}
     *
     * @param deploymentRoot
     * @param filter - true if the jar filename filter should be applied
     * @param filterOnJarFilename
     * @return A arrays of {@link URL}
     * @throws DeploymentUnitProcessingException
     * @throws IOException
     */
    private Set<URL> getJarList(final VirtualFile deploymentRoot, boolean filter, VirtualFileFilter filterOnJarFilename) throws DeploymentUnitProcessingException,
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
                URL url = VFSUtils.getPhysicalURL(virtualFile);
                uniqueArchiveUrls.add(url);
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
