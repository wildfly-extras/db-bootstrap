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
 * Specifies that it's an bootstrapping class. This annotation is applied to the class.
 * This co-exists with the {@link BootstrapDatabase} and {@link UpdateSchema} annotation.
 * @author Flemming Harms
 * @author Nicky Moelholm (moelholm@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface BootstrapDatabase {
    /**
     * The name of the hibernate configuration file.
     * <br><br>
     * This method will first search the parent class loader for the resource;
     * if the parent is null the path of the class loader built-in to the virtual machine is searched.
     * That failing, this method will invoke findResource(String) to find the resource.
     * This is optional and if not specified no Session will be created
     */
    String hibernateCfg() default "";
    /**
     * The priority it should organized after. Higher priorities will be executed first
     */
    int priority() default 1;
    /**
     * An optional name of this configuration.
     * <br><br>
     * This name can be referenced when defining runtime configuration (system properties). The
     * system properties will have precedence to the properties defined in the hibernate cfg
     * file.
     */
    String name() default "";
}
