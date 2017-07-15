/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.security.jacc;

import java.security.PermissionCollection;
import java.util.Map;
import java.io.Serializable;

/**
 * @version $Rev$ $Date$
 */
public class ComponentPermissions implements Serializable {
    private final PermissionCollection excludedPermissions ;
    private final PermissionCollection uncheckedPermissions;
    private final Map<String, PermissionCollection> rolePermissions;

    public ComponentPermissions(PermissionCollection excludedPermissions, PermissionCollection uncheckedPermissions, Map<String, PermissionCollection> rolePermissions) {
        this.excludedPermissions = excludedPermissions;
        this.uncheckedPermissions = uncheckedPermissions;
        this.rolePermissions = rolePermissions;
    }

    public PermissionCollection getExcludedPermissions() {
        return excludedPermissions;
    }

    public PermissionCollection getUncheckedPermissions() {
        return uncheckedPermissions;
    }

    public Map<String, PermissionCollection> getRolePermissions() {
        return rolePermissions;
    }
}
