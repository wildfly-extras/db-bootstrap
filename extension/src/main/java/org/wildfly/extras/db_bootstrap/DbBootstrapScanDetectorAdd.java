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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 * @author Rasmus Lund
 */
class DbBootstrapScanDetectorAdd extends AbstractBoottimeAddStepHandler {

    private final AtomicInteger priorityDelta = new AtomicInteger(0);

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        DbBootstrapScanDetectorResourceDefinition.FILENAME.validateAndSet(operation, model);
        DbBootstrapScanDetectorResourceDefinition.FILTER_ON_NAME.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        String filename = model.get(DbBootstrapExtension.FILENAME_ATTR).asString();
        ModelNode filterOnNameAttributeModelNode = model.get(DbBootstrapExtension.FILTER_ON_NAME_ATTR);

        List<ModelNode> filterOnNames = new LinkedList<ModelNode>();
        if (filterOnNameAttributeModelNode.isDefined()) {
            filterOnNames.addAll(filterOnNameAttributeModelNode.asList());
        }

        final PathAddress myModelsAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        ModelNode myFullModelWithChildren = Resource.Tools.readModel(context.readResourceFromRoot(myModelsAddress, true), -1);
        ModelNode myClassModelNodes = myFullModelWithChildren.get(DbBootstrapExtension.CLASS);
        List<String> classes = new LinkedList<String>();
        if (myClassModelNodes.isDefined()) {
            for (ModelNode classModelNode : myClassModelNodes.asList()) {
                if (classModelNode.isDefined()) {
                    String className = classModelNode.get(0).get(DbBootstrapExtension.CLASSNAME_ATTR).asString();
                    classes.add(className);
                }
            }
        }

        context.addStep(new DbBootstrapDeploymentChainStep(filterOnNames, filename, classes), OperationContext.Stage.RUNTIME);
    }

    private final class DbBootstrapDeploymentChainStep extends AbstractDeploymentChainStep {

        private final List<ModelNode> filterOnNames;
        private final String filename;
        private final List<String> classes;

        DbBootstrapDeploymentChainStep(List<ModelNode> filterOnNames, String filename, List<String> classes) {
            this.filterOnNames = filterOnNames;
            this.filename = filename;
            this.classes = classes;
        }

        @Override
        protected void execute(DeploymentProcessorTarget processorTarget) {

            DbBootstrapLogger.ROOT_LOGGER.tracef("%s:'%s' %s:'%s'", DbBootstrapExtension.FILENAME_ATTR, filename,
                    DbBootstrapExtension.FILTER_ON_NAME_ATTR, filterOnNames);

            try {
                String subsystemName = DbBootstrapExtension.SUBSYSTEM_NAME;
                int priority = Phase.PARSE_WEB_DEPLOYMENT + priorityDelta.getAndIncrement();
                DbBootstrapScanDetectorProcessor processor;
                processor = new DbBootstrapScanDetectorProcessor(filename, filterOnNames, classes);
                processorTarget.addDeploymentProcessor(subsystemName, Phase.PARSE, priority, processor);

            } catch (Exception e) {
                DbBootstrapLogger.ROOT_LOGGER.error("Error when executing db-bootstrap deployment chain step", e);
            }
        }
    }
}
