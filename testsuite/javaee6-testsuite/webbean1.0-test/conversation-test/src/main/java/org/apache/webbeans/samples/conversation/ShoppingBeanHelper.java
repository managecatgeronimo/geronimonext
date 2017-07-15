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
 
package org.apache.webbeans.samples.conversation;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIData;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class ShoppingBeanHelper {

    private UIData uiTable;

    private @Inject ShoppingBean shopingBean;

    /**
     * @return the uiTable
     */
    public UIData getUiTable() {
        return uiTable;
    }

    /**
     * @param uiTable
     *            the uiTable to set
     */
    public void setUiTable(UIData uiTable) {
        this.uiTable = uiTable;
    }

    public String buy() {
        Item item = (Item) uiTable.getRowData();
        this.shopingBean.getItems().add(item);

        return null;
    }

}
