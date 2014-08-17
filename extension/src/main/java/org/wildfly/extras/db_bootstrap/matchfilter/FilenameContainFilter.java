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
package org.wildfly.extras.db_bootstrap.matchfilter;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.AbstractVirtualFileFilterWithAttributes;

/**
 * Match all files that contain the filter text and ends with the suffix
 * @author Flemming Harms
 *
 */
public class FilenameContainFilter extends AbstractVirtualFileFilterWithAttributes {
    private final String filter;
    private final String suffix;

    public FilenameContainFilter(String filter, String suffix) {
        this(filter, suffix, VisitorAttributes.DEFAULT);
    }

    public FilenameContainFilter(String filter, String suffix, VisitorAttributes attributes) {
        super(attributes);
        this.filter = filter;
        this.suffix = suffix;
    }

    @Override
    public boolean accepts(VirtualFile file) {
        String name = file.getName();
        return (name.contains(filter) && name.endsWith(suffix));
    }

}
