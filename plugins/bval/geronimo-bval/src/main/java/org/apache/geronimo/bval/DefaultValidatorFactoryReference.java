/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.bval;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.apache.xbean.naming.reference.SimpleReference;

/**
 * @version $Rev$ $Date$
 */
public class DefaultValidatorFactoryReference extends SimpleReference {
    private ValidatorFactory factory;
    
    @Override
    public Object getContent() throws NamingException {
        if(factory == null) {
            factory = Validation.buildDefaultValidatorFactory();
        }
        
        return factory;
    }
}
