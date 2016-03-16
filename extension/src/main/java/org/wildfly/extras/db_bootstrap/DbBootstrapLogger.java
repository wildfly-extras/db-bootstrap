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

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Frank Vissing
 * @author Flemming Harms
 */
@MessageLogger(projectCode = "DB_BOOTSTRAP")
public interface DbBootstrapLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    DbBootstrapLogger ROOT_LOGGER = Logger.getMessageLogger(DbBootstrapLogger.class, DbBootstrapLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Starting Database Bootstrapper subsystem")
    void subsystemStarted();

    @LogMessage(level = WARN)
    @Message(id = 2, value = "Could not process hibernate configurations")
    void couldNotProcessHibernateConfigurations(@Cause Throwable cause);

}
