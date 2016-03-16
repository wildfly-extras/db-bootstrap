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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Defines the contract for the Bootstrap provider that implements the specific database
 * logic for connecting and executing database scripts.
 *
 * @author Flemming Harms
 */
public interface BootstrapProvider {

    /**
     * Invoke the provider with a configuration file
     *
     * @param prefix - the prefix for using when loading properties for the session
     * @param configuration - The location of the configuration file.
     * @param bootstrapClass - the class to invoke the method on
     * @param classLoader - The classloader to load resources from
     * @param method  - the method to invoke
     * @throws Exception
     */
    void invokeWithParameters(String prefix, String configuration, Object bootstrapClass, final ClassLoader classLoader, Method method) throws Exception;

    /**
     * Invoke the annotated method for the specified bootstrap class
     * @see BootstrapProvider
     */
    void invoke(Method method, Object bootstrapClass) throws InvocationTargetException, IllegalAccessException;
}
