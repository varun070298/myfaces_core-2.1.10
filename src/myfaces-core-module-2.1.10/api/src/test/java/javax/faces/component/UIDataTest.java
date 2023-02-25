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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.faces.application.StateManager;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.render.Renderer;

import org.apache.myfaces.Assert;
import org.apache.myfaces.TestRunner;
import org.apache.myfaces.mock.MockRenderedValueExpression;
import org.apache.myfaces.test.base.AbstractJsfTestCase;
import org.apache.myfaces.test.mock.visit.MockVisitContext;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

/**
 * @author Mathias Broekelmann (latest modification by $Author: lu4242 $)
 * @version $Revision: 1150982 $ $Date: 2011-07-25 23:06:31 -0500 (Mon, 25 Jul 2011) $
 */
public class UIDataTest extends AbstractJsfTestCase
{

    public UIDataTest(String name)
    {
        super(name);
    }

    private IMocksControl _mocksControl;
    private UIData _testImpl;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        _mocksControl = EasyMock.createControl();
        _testImpl = new UIData();
    }

    /**
     * Test method for
     * {@link javax.faces.component.UIData#setValueExpression(java.lang.String, javax.el.ValueExpression)}.
     */
    public void testValueExpression()
    {
        assertSetValueExpressionException(IllegalArgumentException.class, "rowIndex");
        assertSetValueExpressionException(NullPointerException.class, null);
    }

    private void assertSetValueExpressionException(Class<? extends Throwable> expected, final String name)
    {
        Assert.assertException(expected, new TestRunner()
        {
            public void run() throws Throwable
            {
                _testImpl.setValueExpression(name, null);
            }
        });
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getClientId(javax.faces.context.FacesContext)}.
     */
    public void testGetClientId()
    {
        _testImpl.setId("xxx");
        Renderer renderer = _mocksControl.createMock(Renderer.class);
        renderKit.addRenderer(UIData.COMPONENT_FAMILY, UIData.COMPONENT_TYPE, renderer);
        assertEquals("xxx", _testImpl.getClientId(facesContext));
        _testImpl.setRowIndex(99);
        //MYFACES-2744 UIData.getClientId() should not append rowIndex, instead use UIData.getContainerClientId()
        assertEquals("xxx", _testImpl.getClientId(facesContext)); 
        assertEquals("xxx:99", _testImpl.getContainerClientId(facesContext));
    }

    /**
     * Test method for
     * {@link javax.faces.component.UIData#invokeOnComponent(javax.faces.context.FacesContext, java.lang.String, javax.faces.component.ContextCallback)}
     * .
     * Tests, if invokeOnComponent also checks the facets of the h:column children (MYFACES-2370)
     */
    public void testInvokeOnComponentFacesContextStringContextCallback()
    {
        /**
         * Concrete class of ContextCallback needed to test invokeOnComponent. 
         * @author Jakob Korherr
         */
        final class MyContextCallback implements ContextCallback
        {
            
            private boolean invoked = false;

            public void invokeContextCallback(FacesContext context,
                    UIComponent target)
            {
                this.invoked = true;
            }
            
        }
        
        UIComponent facet = new UIOutput();
        facet.setId("facet");
        UIColumn column = new UIColumn();
        column.setId("column");
        column.getFacets().put("header", facet);
        _testImpl.setId("uidata");
        _testImpl.getChildren().add(column);
        
        MyContextCallback callback = new MyContextCallback();
        _testImpl.invokeOnComponent(facesContext, facet.getClientId(facesContext), callback);
        // the callback should have been invoked
        assertTrue(callback.invoked);
    }

    /**
     * Test method for {@link javax.faces.component.UIData#broadcast(javax.faces.event.FacesEvent)}.
     */
    public void testBroadcastFacesEvent()
    {
        // create event mock
        final FacesEvent originalEvent = _mocksControl.createMock(FacesEvent.class);
        
        // create the component for the event
        UIComponent eventComponent = new UICommand()
        {

            @Override
            public void broadcast(FacesEvent event)
                    throws AbortProcessingException
            {
                // the event must be the originalEvent
                assertEquals(originalEvent, event);
                
                // the current row index must be the row index from the time the event was queued
                assertEquals(5, _testImpl.getRowIndex());
                
                // the current component must be this (pushComponentToEL() must have happened)
                assertEquals(this, UIComponent.getCurrentComponent(facesContext));
                
                // to be able to verify that broadcast() really has been called
                getAttributes().put("broadcastCalled", Boolean.TRUE);
            }
            
        };
        
        // set component on event
        EasyMock.expect(originalEvent.getComponent()).andReturn(eventComponent).anyTimes();
        // set phase on event
        EasyMock.expect(originalEvent.getPhaseId()).andReturn(PhaseId.INVOKE_APPLICATION).anyTimes();
        _mocksControl.replay();
        
        // set PhaseId for event processing
        facesContext.setCurrentPhaseId(PhaseId.INVOKE_APPLICATION);
        // set row index for event
        _testImpl.setRowIndex(5);
        // UIData must be a child of UIViewRoot to queue and event
        facesContext.getViewRoot().getChildren().add(_testImpl);
        // queue event (this will create a FacesEventWrapper with the current row index)
        _testImpl.queueEvent(originalEvent);
        // change the current row index
        _testImpl.setRowIndex(0);
        // now broadcast the event (this will call UIData.broadcast())
        facesContext.getViewRoot().broadcastEvents(facesContext, PhaseId.INVOKE_APPLICATION);
        
        // -= Assertions =-
        
        // the current component must be null (popComponentFromEL() must have happened)
        assertNull(UIComponent.getCurrentComponent(facesContext));
        
        // the row index must now be 0 (at broadcast() it must be 5)
        assertEquals(0, _testImpl.getRowIndex());
        
        // verify mock behavior
        _mocksControl.verify();
        
        // verify that broadcast() really has been called
        assertEquals(Boolean.TRUE, eventComponent.getAttributes().get("broadcastCalled"));
    }

    /**
     * Test method for {@link javax.faces.component.UIData#encodeBegin(javax.faces.context.FacesContext)}.
     */
    public void testEncodeBeginFacesContext()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#encodeEnd(javax.faces.context.FacesContext)}.
     */
    public void testEncodeEndFacesContext()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#queueEvent(javax.faces.event.FacesEvent)}.
     */
    public void testQueueEventFacesEvent()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#processDecodes(javax.faces.context.FacesContext)}.
     */
    public void testProcessDecodesFacesContext()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#processValidators(javax.faces.context.FacesContext)}.
     */
    public void testProcessValidatorsFacesContext()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#processUpdates(javax.faces.context.FacesContext)}.
     */
    public void testProcessUpdatesFacesContext()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#saveState(javax.faces.context.FacesContext)}.
     */
    public void testSaveState()
    {
        // TODO
    }

    /**
     * Test method for
     * {@link javax.faces.component.UIData#restoreState(javax.faces.context.FacesContext, java.lang.Object)}.
     */
    public void testRestoreState()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#UIData()}.
     */
    public void testUIData()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setFooter(javax.faces.component.UIComponent)}.
     */
    public void testSetFooter()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getFooter()}.
     */
    public void testGetFooter()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setHeader(javax.faces.component.UIComponent)}.
     */
    public void testSetHeader()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getHeader()}.
     */
    public void testGetHeader()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#isRowAvailable()}.
     */
    public void testIsRowAvailable()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getRowCount()}.
     */
    public void testGetRowCount()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getRowData()}.
     */
    public void testGetRowData()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getRowIndex()}.
     */
    public void testGetRowIndex()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setRowIndex(int)}.
     */
    public void testSetRowIndex()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getDataModel()}.
     */
    public void testGetDataModel()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setDataModel(javax.faces.model.DataModel)}.
     */
    public void testSetDataModel()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setValue(java.lang.Object)}.
     */
    public void testSetValue()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setRows(int)}.
     */
    public void testSetRows()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setFirst(int)}.
     */
    public void testSetFirst()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getValue()}.
     */
    public void testGetValue()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getVar()}.
     */
    public void testGetVar()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#setVar(java.lang.String)}.
     */
    public void testSetVar()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getRows()}.
     */
    public void testGetRows()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getFirst()}.
     */
    public void testGetFirst()
    {
        // TODO
    }

    /**
     * Test method for {@link javax.faces.component.UIData#getFamily()}.
     */
    public void testGetFamily()
    {
        // TODO
    }
    
    /**
     * Test method for 
     * {@link javax.faces.component.UIData#visitTree(javax.faces.component.visit.VisitContext, javax.faces.component.visit.VisitCallback)}.
     */
    public void testVisitTree() {
        UIData uidata = new UIData();
        // value
        Collection<String> value = new ArrayList<String>();
        value.add("value#1");
        value.add("value#2");
        uidata.setValue(value);
        // header facet
        UIComponent headerFacet = new HtmlPanelGroup();
        headerFacet.setId("headerFacet");
        uidata.setHeader(headerFacet);
        // footer facet
        UIComponent footerFacet = new HtmlPanelGroup();
        footerFacet.setId("footerFacet");
        uidata.setFooter(footerFacet);
        // first child
        UIComponent child1 = new UIColumn();
        // facet of first child
        UIComponent facetOfChild1 = new HtmlPanelGroup();
        child1.getFacets().put("someFacet", facetOfChild1);
        // child of first child
        UIOutput childOfChild1 = new UIOutput();
        childOfChild1.setId("childOfColumn");
        child1.getChildren().add(childOfChild1);
        uidata.getChildren().add(child1);
        // second child (should not be processed --> != UIColumn)
        UIComponent child2 = new HtmlPanelGroup(); 
        uidata.getChildren().add(child2);
        VisitCallback callback = null;
        
        IMocksControl control = EasyMock.createControl();
        VisitContext visitContextMock = control.createMock(VisitContext.class);
        EasyMock.expect(visitContextMock.getFacesContext()).andReturn(facesContext).anyTimes();
        EasyMock.expect(visitContextMock.getHints()).andReturn(Collections.<VisitHint>emptySet()).anyTimes();
        Collection<String> subtreeIdsToVisit = new ArrayList<String>();
        subtreeIdsToVisit.add("1");
        EasyMock.expect(visitContextMock.getSubtreeIdsToVisit(uidata)).andReturn(subtreeIdsToVisit);
        EasyMock.expect(visitContextMock.invokeVisitCallback(uidata, callback)).andReturn(VisitResult.ACCEPT);
        EasyMock.expect(visitContextMock.invokeVisitCallback(headerFacet, callback)).andReturn(VisitResult.ACCEPT);
        EasyMock.expect(visitContextMock.invokeVisitCallback(footerFacet, callback)).andReturn(VisitResult.ACCEPT);
        EasyMock.expect(visitContextMock.invokeVisitCallback(facetOfChild1, callback)).andReturn(VisitResult.ACCEPT);
        EasyMock.expect(visitContextMock.invokeVisitCallback(child1, callback)).andReturn(VisitResult.ACCEPT);
        EasyMock.expect(visitContextMock.invokeVisitCallback(childOfChild1, callback)).andReturn(VisitResult.ACCEPT).times(2);
        control.replay();
        
        uidata.visitTree(visitContextMock, callback);
        
        control.verify();
        
        // VisitHint.SKIP_ITERATION test:
        
        // (1) uiData with two rows - should iterate over row twice
        MockVisitContext mockVisitContext = new MockVisitContext(facesContext, null);
        CountingVisitCallback countingVisitCallback = new CountingVisitCallback(2);
        uidata.visitTree(mockVisitContext, countingVisitCallback);
        countingVisitCallback.verify();
        
        // (2) uiData with two values - should iterate over row ones - SKIP_ITERATION is used
        mockVisitContext = new MockVisitContext(facesContext, EnumSet.of(VisitHint.SKIP_ITERATION));
        countingVisitCallback = new CountingVisitCallback(1);
        uidata.visitTree(mockVisitContext, countingVisitCallback);
        countingVisitCallback.verify();
        
        // (3) uiData with five values - should iterate over row five times
        value = new ArrayList<String>();
        value.add("1");
        value.add("2");
        value.add("3");
        value.add("4");
        value.add("5");
        uidata.setValue(value);
        mockVisitContext = new MockVisitContext(facesContext, null);
        countingVisitCallback = new CountingVisitCallback(5);
        uidata.visitTree(mockVisitContext, countingVisitCallback);
        countingVisitCallback.verify();
        
        // (4) uiData with five values - should iterate over child ones - SKIP_ITERATION is used
        mockVisitContext = new MockVisitContext(facesContext, EnumSet.of(VisitHint.SKIP_ITERATION));
        countingVisitCallback = new CountingVisitCallback(1);
        uidata.visitTree(mockVisitContext, countingVisitCallback);
        countingVisitCallback.verify();
    }

    
    public static class RowData
    {
        private String text;

        public RowData(String text, String style)
        {
           super();
            this.text = text;
            this.style = style;
        }

        private String style;
        
        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public String getStyle()
        {
            return style;
        }

        public void setStyle(String style)
        {
            this.style = style;
        }
    }
    
    public void testPreserveRowComponentState1() throws Exception
    {
        List<RowData> model = new ArrayList<RowData>();
        model.add(new RowData("text1","style1"));
        model.add(new RowData("text1","style2"));
        model.add(new RowData("text1","style3"));
        model.add(new RowData("text1","style4"));
        
        //Put on request map to be resolved later
        request.setAttribute("list", model);
        
        UIViewRoot root = facesContext.getViewRoot();
        UIData table = new UIData();
        UIColumn column = new UIColumn();
        UIOutput text = new UIOutput();
        
        //This is only required if markInitiaState fix is not used 
        root.setId(root.createUniqueId());
        table.setId(root.createUniqueId());
        column.setId(root.createUniqueId());
        text.setId(root.createUniqueId());
        
        table.setVar("row");
        table.setRowStatePreserved(true);
        table.setValueExpression("value", application.
                getExpressionFactory().createValueExpression(
                        facesContext.getELContext(),"#{list}",List.class));
        
        text.setValueExpression("value", application.
                getExpressionFactory().createValueExpression(
                        facesContext.getELContext(),"#{row.text}",String.class));
        
        root.getChildren().add(table);
        table.getChildren().add(column);
        column.getChildren().add(text);

        //Simulate markInitialState call.
        facesContext.getAttributes().put(StateManager.IS_BUILDING_INITIAL_STATE, Boolean.TRUE);
        root.markInitialState();
        table.markInitialState();
        column.markInitialState();
        text.markInitialState();
        facesContext.getAttributes().remove(StateManager.IS_BUILDING_INITIAL_STATE);
        
        //Check the value expressions are working and change the component state 
        for (int i = 0; i < model.size(); i++)
        {
            RowData rowData = model.get(i); 
            table.setRowIndex(i);
            assertEquals(rowData.getText(), text.getValue());
            text.getAttributes().put("style", rowData.getStyle());
        }
        
        //Reset row index
        table.setRowIndex(-1);

        //Check the values were not lost
        for (int i = 0; i < model.size(); i++)
        {
            table.setRowIndex(i);
            assertEquals(model.get(i).getStyle(), text.getAttributes().get("style"));
        }
        
    }

    private final class CountingVisitCallback implements VisitCallback {
        
        public final int expectedVisits;
        
        public CountingVisitCallback(int expectedRowVisits) {
            super();
            this.expectedVisits = expectedRowVisits;
        }

        public int headerFacetVisits = 0;
        public int footerFacetVisits = 0;
        public int rowVisits = 0;
        
        public VisitResult visit(VisitContext context, UIComponent target) {
            
            if ("headerFacet".equals(target.getId())) {
                headerFacetVisits++;
            } else if ("footerFacet".equals(target.getId())) {
                footerFacetVisits++;
            } else if ("childOfColumn".equals(target.getId())) {
                rowVisits++;
            }
            return VisitResult.ACCEPT;
        }
        
        public void verify() {
                assertEquals("header facet must be visited only ones", 1, headerFacetVisits);
                assertEquals("footer facet must be visited only ones", 1, footerFacetVisits);
                assertEquals("Expected row visit does not match", expectedVisits, rowVisits);
        }
    }
    
    public void testProcessDecodesRenderedFalse() throws Exception {
        UIData uiData = new VerifyNoLifecycleMethodComponent();
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext,  uiData, false);
        
        uiData.processDecodes(facesContext);
        
        assertEquals("processDecodes must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
        
    }
    
    public void testProcessDecodesRenderedTrue() throws Exception {
        
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext, _testImpl, true);
        _addColumn();
        
        _testImpl.processDecodes(facesContext);
        
        assertEquals("processDecodes must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
    }
    
    
    public void testProcessValidatorsRenderedFalse() throws Exception {
        UIData uiData = new VerifyNoLifecycleMethodComponent();
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext,  uiData, false);
        
        uiData.processValidators(facesContext);
        
        assertEquals("processDecodes must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
        
    }
    
    public void testProcessValidatorsRenderedTrue() throws Exception {
        
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext, _testImpl, true);
        _addColumn();
        
        _testImpl.processValidators(facesContext);
        
        assertEquals("processValidators must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
    }
    
    public void testProcessUpdatesRenderedFalse() throws Exception {
        UIData uiData = new VerifyNoLifecycleMethodComponent();
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext,  uiData, false);
        
        uiData.processUpdates(facesContext);
        
        assertEquals("processUpdates must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
        
    }
    
    public void testProcessUpdatesRenderedTrue() throws Exception {
        
        UIComponent parent = MockRenderedValueExpression.setUpComponentStack(facesContext, _testImpl, true);
        _addColumn();
        
        _testImpl.processUpdates(facesContext);
        
        assertEquals("processUpdates must not change currentComponent", parent, UIComponent.getCurrentComponent(facesContext));
    }

    private void _addColumn() {
        UIColumn uiColumn = new UIColumn();
        uiColumn.setId("testId");
        _testImpl.getChildren().add(uiColumn);
        MockRenderedValueExpression ve = new MockRenderedValueExpression("#{component.is eq 'testId'}", Boolean.class, uiColumn, true);
        uiColumn.setValueExpression("rendered", ve);
    }
    

    /** Verifies no call to encode* and process* methods */
    public class VerifyNoLifecycleMethodComponent extends UIData
    {
        public void setRowIndex(int rowIndex) {
            fail();
        }
        public void decode(FacesContext context) {
            fail();
        }
        public void validate(FacesContext context) {
            fail();
        }
        public void updateModel(FacesContext context) {
            fail();
        }
        public void encodeBegin(FacesContext context) throws IOException {
            fail();
        }
        public void encodeChildren(FacesContext context) throws IOException {
            fail();
        }
        public void encodeEnd(FacesContext context) throws IOException {
            fail();
        }
    }   
}
