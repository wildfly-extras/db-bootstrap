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
package org.wildfly.extras.db_bootstrap.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies this is a update method. This annotation applies to a method
 * and is executed a part of the {@link org.jboss.as.server.deployment.Phase}.PARSE
 * and {@link org.jboss.as.server.deployment.Phase}.PARSE_EJB_DEPLOYMENT and is the
 * second phase of bootstrapping and run after {@link BootstrapSchema}
 * <br><br>
 * If the parameter signature of the method is {@link org.hibernate.Session} the
 * hibernate session will be passed into the method call.
 * If it's not present the method is responsible for creating a connection to the database.
 *
 * @author Flemming Harms
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UpdateSchema {
}
