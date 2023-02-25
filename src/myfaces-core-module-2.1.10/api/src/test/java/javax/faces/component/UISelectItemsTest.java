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
package javax.faces.component;

import java.util.ArrayList;
import java.util.List;

import javax.el.ValueExpression;

import org.apache.myfaces.test.base.AbstractJsfTestCase;
import org.apache.myfaces.test.el.MockValueExpression;

/**
 * Tests for UISelectItems.
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 883906 $ $Date: 2009-11-24 17:34:46 -0500 (Tue, 24 Nov 2009) $
 */
public class UISelectItemsTest extends AbstractJsfTestCase
{

    public UISelectItemsTest(String name)
    {
        super(name);
    }

    public void testStringListAsValue() 
    {
        List<String> value = new ArrayList<String>();
        value.add("#1");
        value.add("#2");
        value.add("#3");
        
        UISelectItems selectItems = new UISelectItems();
        selectItems.setValue(value);
        selectItems.getAttributes().put("var", "item");
        ValueExpression itemValue = new MockValueExpression("#{item}", Object.class);
        selectItems.setValueExpression("itemValue" , itemValue);
        
        UISelectOne selectOne = new UISelectOne();
        selectOne.getChildren().add(selectItems);
        
        _SelectItemsIterator iter = new _SelectItemsIterator(selectOne, facesContext);
        List<String> options = new ArrayList<String>();
        while(iter.hasNext())
        {
            options.add((String) iter.next().getValue());
        }
        
        assertEquals(value, options);
    }
    
    public void testPrimitiveArrayAsValue()
    {
        int[] value = new int[3];
        value[0] = 1;
        value[1] = 2;
        value[2] = 3;
        
        UISelectItems selectItems = new UISelectItems();
        selectItems.setValue(value);
        selectItems.getAttributes().put("var", "item");
        ValueExpression itemValue = new MockValueExpression("#{item}", Object.class);
        selectItems.setValueExpression("itemValue" , itemValue);
        
        UISelectOne selectOne = new UISelectOne();
        selectOne.getChildren().add(selectItems);
        
        _SelectItemsIterator iter = new _SelectItemsIterator(selectOne, facesContext);
        int[] options = new int[3];
        for (int i = 0; i < 3; i++)
        {
            options[i] = (Integer) iter.next().getValue();
            
            // test equality
            assertEquals(value[i], options[i]);
        }
    }
}
