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

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.AbstractVirtualFileFilterWithAttributes;

/**
 * Match all files that contain the filtered list
 * @author Flemming Harms
 *
 */
public class FilenameContainFilter extends AbstractVirtualFileFilterWithAttributes {
    private List<ModelNode> filterOnName;

    public FilenameContainFilter(List<ModelNode> filterOnName, VisitorAttributes attributes) {
        super(attributes);
        this.filterOnName = filterOnName;
    }

    @Override
    public boolean accepts(VirtualFile file) {
        for (ModelNode filter : filterOnName) {
            PathFilter matchFilter = PathFilters.match(filter.asString());
            if (matchFilter.accept(file.getName())) {
                return true;
            }
        }
        return false;
    }

}
