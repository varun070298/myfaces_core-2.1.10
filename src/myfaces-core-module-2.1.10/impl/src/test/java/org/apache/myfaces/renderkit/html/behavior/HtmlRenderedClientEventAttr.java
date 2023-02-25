/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.renderkit.html.behavior;

/**
 * @author Leonardo Uribe (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 07:21:54 -0500 (Sat, 22 Oct 2011) $
 */
public class HtmlRenderedClientEventAttr
{
    private String name;
    private String clientEvent;
    private String value;
    
    public HtmlRenderedClientEventAttr(String name, String clientEvent) {
        this(name, clientEvent, name);
    }
    
    public HtmlRenderedClientEventAttr(String name, String clientEvent, String value) {
        this.name = name;
        this.clientEvent = clientEvent;
        this.value = value;
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getClientEvent()
    {
        return clientEvent;
    }

    public void setClientEvent(String clientEvent)
    {
        this.clientEvent = clientEvent;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}
