/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.axis2;

import javax.xml.ws.handler.Handler;

import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.handler.lifecycle.factory.HandlerLifecycleManager;
import org.apache.axis2.jaxws.injection.ResourceInjectionException;
import org.apache.axis2.jaxws.lifecycle.LifecycleException;

public class GeronimoHandlerLifecycleManager implements HandlerLifecycleManager {
        
    public Handler createHandlerInstance(MessageContext context, Class handlerClass) 
        throws LifecycleException, ResourceInjectionException {     
             
        Handler instance = null;
        
        try {
            instance = (Handler) handlerClass.newInstance();
        } catch (Exception e) {
            throw new LifecycleException("Failed to create handler", e);
        }
                
        return instance;
    }
      
    public void invokePostConstruct() throws LifecycleException { 
    }

    public void invokePreDestroy() throws LifecycleException {
    }

    public void destroyHandlerInstance(MessageContext cxt, Handler handler)
        throws LifecycleException, ResourceInjectionException {
    }
   
}
