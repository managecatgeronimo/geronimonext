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
package org.apache.geronimo.management;

/**
 * Represents the JSR-77 type with the same name
 *
 * @version $Rev$ $Date$
 */
public interface JDBCResource extends J2EEResource {
    /**
     * A list of data sources this JDBC resource provides.
     * @see "JSR77.3.25.1.1"
     * @return the ObjectNames of the JDBCDataSources
     */
    String[] getJDBCDataSources();
}
