/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.axis2.testdata.rpclit;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name="RPCLit",
            portName="RPCLit",
            serviceName="RPCLitService",
            targetNamespace = "http://org/apache/geronimo/axis2/rpclit")
@SOAPBinding(style=SOAPBinding.Style.RPC, 
             use=SOAPBinding.Use.LITERAL)
public class RPCLitService {
	
	@WebMethod
	public String testSimple(String in){
		return "Hello "+in;
	}
	
	@WebMethod
	public String testSimple2(String simple2In1, String simple2In2) {
        return simple2In1 + simple2In2;
    }
}
