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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.db_bootstrap.providers.HibernateBootstrapProvider;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 * @author Rasmus Lund
 */
class DbBootstrapScanDetectorAdd extends AbstractBoottimeAddStepHandler {

    private final AtomicInteger priorityDelta = new AtomicInteger(1);

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        DbBootstrapScanDetectorResourceDefinition.FILENAME.validateAndSet(operation, model);
        DbBootstrapScanDetectorResourceDefinition.FILTER_ON_NAME.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String filename = model.get(DbBootstrapExtension.FILENAME_ATTR).asString();
        context.addStep(new DbBootstrapDeploymentChainStep(filename), OperationContext.Stage.RUNTIME);
    }

    private final class DbBootstrapDeploymentChainStep extends AbstractDeploymentChainStep {

        private final String filename;

        DbBootstrapDeploymentChainStep(String filename) {
            this.filename = filename;
        }

        @Override
        protected void execute(DeploymentProcessorTarget processorTarget) {

            DbBootstrapLogger.ROOT_LOGGER.tracef("%s:'%s'", DbBootstrapExtension.FILENAME_ATTR, filename);

            try {
                String subsystemName = DbBootstrapExtension.SUBSYSTEM_NAME;
                int priority = Phase.FIRST_MODULE_USE_PERSISTENCE_CLASS_FILE_TRANSFORMER - priorityDelta.getAndIncrement();
                DbBootstrapScanDetectorProcessor processor;

                processor = new DbBootstrapScanDetectorProcessor(filename, new HibernateBootstrapProvider());
                processorTarget.addDeploymentProcessor(subsystemName, Phase.FIRST_MODULE_USE, priority, processor);

            } catch (Exception e) {
                DbBootstrapLogger.ROOT_LOGGER.error("Error when executing db-bootstrap deployment chain step", e);
            }
        }
    }
}
