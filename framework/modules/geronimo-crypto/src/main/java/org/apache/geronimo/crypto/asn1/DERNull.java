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

package org.apache.geronimo.crypto.asn1;

import java.io.IOException;

/**
 * A NULL object.
 */
public class DERNull
    extends ASN1Null
{
    byte[]  zeroBytes = new byte[0];

    public DERNull()
    {
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(NULL, zeroBytes);
    }

    public boolean equals(
        Object o)
    {
        if ((o == null) || !(o instanceof DERNull))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return 0;
    }
}
