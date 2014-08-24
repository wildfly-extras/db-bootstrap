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

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
final class DbBootstrapScanDetectorAdd extends AbstractBoottimeAddStepHandler {

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        DbBootstrapScanDetectorResourceDefinition.NAME.validateAndSet(operation, model);
        DbBootstrapScanDetectorResourceDefinition.FILENAME.validateAndSet(operation, model);
        DbBootstrapScanDetectorResourceDefinition.FILTER_ON_NAME.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String filename = model.get(DbBootstrapExtension.FILENAME_ATTR).asString();
        final List<ModelNode> filterOnName =  model.get(DbBootstrapExtension.FILTER_ON_NAME_ATTR).asList();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                DbBootstrapLogger.ROOT_LOGGER.tracef("%s:'%s' %s:'%s'",DbBootstrapExtension.FILENAME_ATTR,filename,DbBootstrapExtension.FILTER_ON_NAME_ATTR,filterOnName);
                try {
                    processorTarget.addDeploymentProcessor(DbBootstrapExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT,new DbBootstrapScanDetectorProcessor(filename,filterOnName));
                } catch (Exception e) {
                    DbBootstrapLogger.ROOT_LOGGER.error("Error in instanciating DbBootstraper add handler", e);
                }


            }
        }, OperationContext.Stage.RUNTIME);
    }
}
