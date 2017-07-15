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


package org.apache.geronimo.crypto.asn1.x509;

import org.apache.geronimo.crypto.asn1.DERObjectIdentifier;

/**
 * PolicyQualifierId, used in the CertificatePolicies
 * X509V3 extension.
 *
 * <pre>
 *    id-qt          OBJECT IDENTIFIER ::=  { id-pkix 2 }
 *    id-qt-cps      OBJECT IDENTIFIER ::=  { id-qt 1 }
 *    id-qt-unotice  OBJECT IDENTIFIER ::=  { id-qt 2 }
 *  PolicyQualifierId ::=
 *       OBJECT IDENTIFIER ( id-qt-cps | id-qt-unotice )
 * </pre>
 */
public class PolicyQualifierId extends DERObjectIdentifier
{
   private static final String id_qt = "1.3.6.1.5.5.7.2";

   private PolicyQualifierId(String id)
      {
         super(id);
      }

   public static final PolicyQualifierId id_qt_cps =
       new PolicyQualifierId(id_qt + ".1");
   public static final PolicyQualifierId id_qt_unotice =
       new PolicyQualifierId(id_qt + ".2");
}
