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
package org.apache.geronimo.axis2.pojo;

import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.jaxws.context.WebServiceContextImpl;
import org.apache.axis2.jaxws.context.factory.MessageContextFactory;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.handler.SoapMessageContext;
import org.apache.axis2.jaxws.server.endpoint.lifecycle.EndpointLifecycleException;
import org.apache.axis2.jaxws.server.endpoint.lifecycle.EndpointLifecycleManager;

public class POJOEndpointLifecycleManager implements EndpointLifecycleManager {
        
    /* 
     * This method is called on each web service call.
     */
    public Object createServiceInstance(MessageContext context, Class serviceClass) throws EndpointLifecycleException {
        org.apache.axis2.context.MessageContext msgContext = context.getAxisMessageContext();
        
        ServiceContext serviceContext = msgContext.getServiceContext();                
        Object instance = serviceContext.getProperty(ServiceContext.SERVICE_OBJECT);
        
        SoapMessageContext soapMessageContext =
            MessageContextFactory.createSoapMessageContext(context);
        WebServiceContextImpl wsContext = new WebServiceContextImpl();
        wsContext.setSoapMessageContext(soapMessageContext);
        
        // associate JAX-WS MessageContext with the thread
        POJOWebServiceContext.set(wsContext);
        
        return instance;
    }
    
    public void invokePostConstruct() throws EndpointLifecycleException { 
    }

    public void invokePreDestroy() throws EndpointLifecycleException {
    }
   
}
