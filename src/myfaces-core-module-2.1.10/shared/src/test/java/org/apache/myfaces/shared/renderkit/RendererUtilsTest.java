/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.myfaces.shared.renderkit;

import java.io.IOException;
import java.io.StringWriter;

import javax.el.ELContext;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.context.FacesContext;

import junit.framework.Assert;

import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.test.base.AbstractJsfTestCase;
import org.apache.myfaces.test.mock.MockResponseWriter;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

/**
 * 
 * @author markoc
 */
public class RendererUtilsTest extends AbstractJsfTestCase {

	private MockResponseWriter writer;

	/**
	 * ResourceHandler nice easy mock
	 */
	ResourceHandler resourceHandlerMock;

	/**
	 * {@link Application} nice easy mock
	 */
	Application applicationMock;

	/**
	 * A {@link Resource} nice easy mock
	 */
	private Resource resourceMock;

	String libraryName = "images";

	String resourceName = "picture.gif";

	String requestPath = "/somePrefix/faces/javax.faces.resource/picture.gif?ln=\"images\"";

	// a Component instance:
	HtmlGraphicImage graphicImage = new HtmlGraphicImage();

    private UIPanel parent;

	public RendererUtilsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();

		writer = new MockResponseWriter(new StringWriter(), null, null);
		facesContext.setResponseWriter(writer);

		applicationMock = EasyMock.createNiceMock(Application.class);
		facesContext.setApplication(applicationMock);

		resourceHandlerMock = EasyMock.createNiceMock(ResourceHandler.class);
		applicationMock.getResourceHandler();
		EasyMock.expectLastCall().andReturn(resourceHandlerMock);
		
		applicationMock.getProjectStage();
		EasyMock.expectLastCall().andReturn(ProjectStage.Development);

		resourceMock = EasyMock.createNiceMock(Resource.class);

		EasyMock.replay(applicationMock);

		graphicImage.getAttributes().put(JSFAttr.LIBRARY_ATTR, libraryName);
		graphicImage.getAttributes().put(JSFAttr.NAME_ATTR, resourceName);
		graphicImage.setId("graphicImageId");
		
		parent = new UIPanel();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * 
	 */
	public void testGetIconSrc() {

		// Training a mock:
		resourceHandlerMock.createResource(resourceName, libraryName);
		EasyMock.expectLastCall().andReturn(resourceMock);
		resourceMock.getRequestPath();
		EasyMock.expectLastCall().andReturn(requestPath);
		EasyMock.replay(resourceHandlerMock);
		EasyMock.replay(resourceMock);

		// Tested method :
		String iconSrc = RendererUtils.getIconSrc(facesContext, graphicImage,
				HTML.IMG_ELEM);

		assertEquals(
				"If name or name/library present, source must be obtained from ResourceHandler",
				requestPath, iconSrc);

	}

	public void testGetIconSrcResourceNotFound() throws Exception {
		// Training a mock:
		EasyMock.reset(resourceHandlerMock);
		resourceHandlerMock.createResource(resourceName, libraryName);
		EasyMock.expectLastCall().andReturn(null);
		EasyMock.replay(resourceHandlerMock);

		// Tested method :
		String iconSrc = RendererUtils.getIconSrc(facesContext, graphicImage,
				HTML.IMG_ELEM);

		assertEquals("RES_NOT_FOUND", iconSrc);
		assertTrue("If resource is not found, a Message must be added",
				facesContext.getMessages(graphicImage.getClientId(facesContext)).hasNext());

	}

    public void testGetStringValue()
    {
        // Test for situation where submittedValue IS NOT String: 
        UIInput uiInput = new UIInput();
        Object submittedValue = new Object();
        uiInput.setSubmittedValue(submittedValue);
        
        String stringValue = RendererUtils.getStringValue(facesContext, uiInput);
        assertNotNull(stringValue);
        assertEquals("If submittedvalue is not String, toString() must be used", submittedValue.toString(), stringValue);
    }

    public void testGetConvertedUIOutputValue()
    {
        UIInput uiInput = new UIInput();
        StringBuilder submittedValue = new StringBuilder("submittedValue");
        uiInput.setSubmittedValue(submittedValue);
        
        
        Object convertedUIOutputValue = RendererUtils.getConvertedUIOutputValue(facesContext, uiInput, submittedValue);
        assertEquals("If submittedvalue is not String, toString() must be used", submittedValue.toString(), convertedUIOutputValue);
    }

    /**
     * test for MYFACES-3126
     */
    @Test
    public void testRenderChild() throws IOException
    {
        
       UIInput uiInput = _setUpComponentStack();
       
       RendererUtils.renderChild(facesContext, uiInput);
       
       assertEquals("Invocation must not change the current component", parent, UIComponent.getCurrentComponent(facesContext));
    }

    
    /**
     * Test that no method encode* are called if component is not rendered 
     */
    @Test
    public void testRenderChild2() throws IOException {

        MockComponent component = new MockComponent();
        
        RendererUtils.renderChild(facesContext, component);
    }
    
    @Test
    public void testIsRendered()
    {
        UIComponent uiComponent = new UIOutput();
        boolean rendered = RendererUtils.isRendered(facesContext, uiComponent);
        assertTrue(rendered);
        
        uiComponent.setRendered(false);
        rendered = RendererUtils.isRendered(facesContext, uiComponent);
        assertFalse(rendered);
        
        uiComponent = _setUpComponentStack();
        rendered = RendererUtils.isRendered(facesContext, uiComponent);
        assertFalse(rendered);
        assertEquals("isRendered must not change current component", parent, UIComponent.getCurrentComponent(facesContext));
    }
    
    /**
     * Verifies the current component on stack
     */
    private class MockRenderedValueExpression extends org.apache.myfaces.test.el.MockValueExpression {

        private final UIComponent toVerify;

        public MockRenderedValueExpression(String expression, Class<?> expectedType, UIComponent toVerify) {
            super(expression, expectedType);
            this.toVerify = toVerify;
        }
        
        @Override
        public Object getValue(ELContext elContext) {
          UIComponent currentComponent = UIComponent.getCurrentComponent(facesContext);
          Assert.assertEquals("If this VE is called, component on stack must be actual" , currentComponent , toVerify);
          return false;
        }
    }
    
    /** Verifies no call to encode* methods */
    private class MockComponent extends UIOutput
    {
        @Override
        public boolean isRendered() {
            return false;
        }
        @Override
        public void encodeBegin(FacesContext context) throws IOException {
            fail();
        }
        @Override
        public void encodeChildren(FacesContext context) throws IOException {
            fail();
        }
        @Override
        public void encodeEnd(FacesContext context) throws IOException {
            fail();
        }
    }
    
    private UIInput _setUpComponentStack() {
        UIInput uiInput = new UIInput();
        parent.getChildren().add(uiInput);
        uiInput.setId("testId");
        
        
        MockRenderedValueExpression ve = new MockRenderedValueExpression("#{component.id eq 'testId'}", Boolean.class, uiInput);
        uiInput.setValueExpression("rendered", ve);
        
       // simlulate that parent panel encodes children and is on the stack:
       parent.pushComponentToEL(facesContext, null);
        return uiInput;
    }

}
