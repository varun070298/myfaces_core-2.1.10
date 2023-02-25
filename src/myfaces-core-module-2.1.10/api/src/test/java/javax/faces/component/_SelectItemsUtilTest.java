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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.myfaces.test.base.junit4.AbstractJsfTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author martinkoci
 */
public class _SelectItemsUtilTest extends AbstractJsfTestCase
{

    private static final String NO_SELECTION_ITEM_VALUE = "1.0";
    private UISelectOne uiComponent;
    private Float value;
    private _SelectItemsIterator iterator;
    private UISelectItem noSelectionOption;
    private UISelectItem selectItem1;
    private UISelectItem selectItem2;
    private UISelectItem selectItem3;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        uiComponent = new UISelectOne();
        
        value = Float.valueOf("1.2");

        noSelectionOption = new UISelectItem();
        noSelectionOption.setNoSelectionOption(true);
        noSelectionOption.setItemValue(NO_SELECTION_ITEM_VALUE);
        uiComponent.getChildren().add(noSelectionOption);
        
        selectItem1 = new UISelectItem();
        selectItem1.setItemValue("1.1");
        uiComponent.getChildren().add(selectItem1);
        selectItem2 = new UISelectItem();
        selectItem2.setItemValue("1.2");
        uiComponent.getChildren().add(selectItem2);
        selectItem3 = new UISelectItem();
        selectItem3.setItemValue("1.3");
        uiComponent.getChildren().add(selectItem3);
        
        iterator = new _SelectItemsIterator(uiComponent, facesContext);
    }

    @After
    public void tearDown() throws Exception
    {
        super.tearDown();
        uiComponent = null;
        value = null;
        iterator = null;
        noSelectionOption = null;
        selectItem1 = null;
        selectItem2 = null;
        selectItem3 = null;
    }

    @Test
    public void testMatchValue()
    {
        
        boolean matchValue = _SelectItemsUtil.matchValue(facesContext, uiComponent, value, iterator, null);
        
        assertTrue("Value Float 1.2 must match SelectItem.value \"1.2\" (type of String)", matchValue);
        
        Float valueNotInSelectItems = Float.valueOf("2.0");
        matchValue = _SelectItemsUtil.matchValue(facesContext, uiComponent, valueNotInSelectItems, iterator, null);
        assertFalse(matchValue);
    }
    
    @Test
    public void testMatchValueWithEnums() throws Exception
    {
        noSelectionOption.setItemValue("NONE");
        selectItem1.setItemValue("ONE");
        selectItem2.setItemValue("TWO");
        selectItem3.setItemValue("THREE");
        iterator = new _SelectItemsIterator(uiComponent, facesContext);
        
        Object enumValue = MockEnum.THREE;
        boolean matchValue = _SelectItemsUtil.matchValue(facesContext, uiComponent, enumValue, iterator, null);
        
        assertTrue("Value Enum THREE must match SelectItem.value \"THREE\" (type of String)", matchValue);
        
        enumValue = MockEnum.FOUR;
        matchValue = _SelectItemsUtil.matchValue(facesContext, uiComponent, enumValue, iterator, null);
        assertFalse(matchValue);
    }
    
    @Test 
    public void testMatchValueWithEnumsNoExtends() throws Exception
    {
        noSelectionOption.setItemValue("NONE");
        selectItem1.setItemValue("ONE");
        selectItem2.setItemValue("TWO");
        selectItem3.setItemValue("THREE");
        iterator = new _SelectItemsIterator(uiComponent, facesContext);
        
        Object enumValue = MockEnum.TWO;
        boolean matchValue = _SelectItemsUtil.matchValue(facesContext, uiComponent, enumValue, iterator, null);
        
        assertTrue("Value Enum TWO must match SelectItem.value \"TWO\" (type of String)", matchValue);
    }
    
    private static enum MockEnum {
        NONE,
        ONE {

            @Override
            public String toString()
            {
                return "ONE";
            } 
            
        },TWO,
        THREE {
 
            @Override
            public String toString()
            {
                return "THREE";
            } 
            
        }, FOUR
    }

    @Test
    public void testIsNoSelectionOption()
    {
        Float value = Float.parseFloat(NO_SELECTION_ITEM_VALUE);
        boolean noSelectionOption = _SelectItemsUtil.isNoSelectionOption(facesContext, uiComponent, value, iterator, null);
        assertTrue(noSelectionOption);
        
        Float valueNotInSelectItems = Float.valueOf("2.0");
        noSelectionOption = _SelectItemsUtil.isNoSelectionOption(facesContext, uiComponent, valueNotInSelectItems, iterator, null);
        assertFalse(noSelectionOption);
        
    }

}
