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
package org.apache.geronimo.axis;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.net.URI;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.JavaServiceDesc;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.TypeMappingRegistryImpl;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.RPCProvider;

import org.apache.geronimo.axis.server.AxisWebServiceContainer;
import org.apache.geronimo.axis.server.POJOProvider;
import org.apache.geronimo.axis.server.ReadOnlyServiceDesc;
import org.apache.geronimo.axis.testData.echosample.EchoBean;
import org.apache.geronimo.webservices.WebServiceContainer;

/**
 *
 * @version $Rev$ $Date$
 */
public class AxisWebServiceContainerTest extends AbstractTestCase {
    public AxisWebServiceContainerTest(String testName) {
        super(testName);
    }

    public void testInvokeSOAP() throws Exception {
/* TODO:  temporarily disabled because of logging problem
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        JavaServiceDesc serviceDesc = new JavaServiceDesc();
        serviceDesc.setEndpointURL("http://127.0.0.1:8080/axis/services/echo");
        //serviceDesc.setWSDLFile(portInfo.getWsdlURL().toExternalForm());
        serviceDesc.setStyle(Style.RPC);
        serviceDesc.setUse(Use.ENCODED);

        TypeMappingRegistryImpl tmr = new TypeMappingRegistryImpl();
        tmr.doRegisterFromVersion("1.3");
        TypeMapping typeMapping = tmr.getOrMakeTypeMapping(serviceDesc.getUse().getEncoding());

        serviceDesc.setTypeMappingRegistry(tmr);
        serviceDesc.setTypeMapping(typeMapping);

        OperationDesc op = new OperationDesc();
        op.setName("echoString");
        op.setStyle(Style.RPC);
        op.setUse(Use.ENCODED);
        Class beanClass = EchoBean.class;
        op.setMethod(beanClass.getMethod("echoString", new Class[] { String.class }));
        ParameterDesc parameter =
            new ParameterDesc(
                new QName("http://ws.apache.org/echosample", "in0"),
                ParameterDesc.IN,
                typeMapping.getTypeQName(String.class),
                String.class,
                false,
                false);
        op.addParameter(parameter);
        serviceDesc.addOperationDesc(op);

        serviceDesc.getOperations();
        ReadOnlyServiceDesc sd = new ReadOnlyServiceDesc(serviceDesc, Collections.EMPTY_LIST);

        Class pojoClass = cl.loadClass("org.apache.geronimo.axis.testData.echosample.EchoBean");

        RPCProvider provider = new POJOProvider();
        SOAPService service = new SOAPService(null, provider, null);
        service.setServiceDescription(sd);
        service.setOption("className","org.apache.geronimo.axis.testData.echosample.EchoBean");
        URI wsdlURL = new URI("echo.wsdl");
        URI location = new URI(serviceDesc.getEndpointURL());
        Map wsdlMap = new HashMap();

        AxisWebServiceContainer container =
            new AxisWebServiceContainer(location, wsdlURL, service, wsdlMap, cl);

        InputStream in = cl.getResourceAsStream("echoString-req.txt");

        try {
            AxisRequest req =
                new AxisRequest(
                    504,
                    "text/xml; charset=utf-8",
                    in,
                    0,
                    new HashMap(),
                    location,
                    new HashMap(),
                    "127.0.0.1");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AxisResponse res = new AxisResponse("text/xml; charset=utf-8", "127.0.0.1", null, null, 8080, out);
            req.setAttribute(WebServiceContainer.POJO_INSTANCE, pojoClass.newInstance());
            container.invoke(req, res);

            out.flush();
            log.debug(new String(out.toByteArray()));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
*/
    }


    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

}
