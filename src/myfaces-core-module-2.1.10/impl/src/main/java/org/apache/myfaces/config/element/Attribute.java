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
package org.apache.myfaces.config.element;

import java.io.Serializable;
import java.util.Collection;

/**
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public abstract class Attribute implements Serializable
{
    public abstract Collection<? extends String> getDescriptions();

    public abstract Collection<? extends String> getDisplayNames();

    public abstract Collection<? extends String> getIcons();

    public abstract String getAttributeName();

    public abstract String getAttributeClass();

    public abstract String getDefaultValue();

    public abstract String getSuggestedValue();

    public abstract Collection<? extends String> getAttributeExtensions();
}
