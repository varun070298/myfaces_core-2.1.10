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
package org.apache.myfaces.application;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;

import org.apache.myfaces.component.ComponentResourceContainer;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.renderkit.html.HtmlScriptRenderer;
import org.apache.myfaces.test.base.junit4.AbstractJsfConfigurableMockTestCase;
import org.apache.myfaces.test.el.MockExpressionFactory;
import org.apache.myfaces.test.mock.MockPropertyResolver;
import org.apache.myfaces.test.mock.MockRenderKit;
import org.apache.myfaces.test.mock.MockRenderKitFactory;
import org.apache.myfaces.test.mock.MockServletContext;
import org.apache.myfaces.test.mock.MockVariableResolver;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationImplAnnotationTest extends AbstractJsfConfigurableMockTestCase
{
    //TODO: need mock objects for VDL/VDLFactory
    //remove from excludes list in pom.xml after complete
    
    public ApplicationImplAnnotationTest()
    {
    }

    @Override
    protected void setFactories() throws Exception
    {
        ((MockServletContext)servletContext).addInitParameter(
                "javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE", "true");
        super.setFactories();
        FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY,
                ApplicationFactoryImpl.class.getName());
        FactoryFinder.setFactory(
                FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY,
                "org.apache.myfaces.view.ViewDeclarationLanguageFactoryImpl");
    }

    @Override
    protected void setUpExternalContext() throws Exception
    {
        super.setUpExternalContext();
        //Set RuntimeConfig object properly to make work ValueExpressions 
        RuntimeConfig.getCurrentInstance(externalContext).setPropertyResolver(
                new MockPropertyResolver());
        RuntimeConfig.getCurrentInstance(externalContext).setVariableResolver(
                new MockVariableResolver());
        RuntimeConfig.getCurrentInstance(externalContext).setExpressionFactory(
                new MockExpressionFactory());
    }

    @Override
    protected void setUpApplication() throws Exception
    {
        super.setUpApplication();
        //We need this two components added
        application.addComponent(UIOutput.COMPONENT_TYPE, UIOutput.class
                .getName());
        application.addComponent(UIPanel.COMPONENT_TYPE, UIPanel.class
                .getName());
        application.addComponent(ComponentResourceContainer.COMPONENT_TYPE, 
                ComponentResourceContainer.class.getName());
    }

    @Override
    protected void setUpRenderKit() throws Exception
    {
        RenderKitFactory renderKitFactory = (RenderKitFactory)
        FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        renderKit = new MockRenderKit();
        renderKit.addRenderer("javax.faces.Output", "javax.faces.resource.Script", new HtmlScriptRenderer());
        renderKitFactory.addRenderKit(RenderKitFactory.HTML_BASIC_RENDER_KIT, renderKit);
    }

    @Override
    public void tearDown() throws Exception
    {
        RuntimeConfig.getCurrentInstance(externalContext).purge();
        super.tearDown();
    }

    /**
     * Test component for ApplicationImplTest
     * 
     */
    public static class UITestComponentA extends UIComponentBase
    {
        public static final String COMPONENT_TYPE = "javax.faces.TestComponentA";
        public static final String COMPONENT_FAMILY = "javax.faces.TestComponentA";
        public static final String DEFAULT_RENDERER_TYPE = "javax.faces.TestComponentA";

        public UITestComponentA()
        {
            setRendererType(DEFAULT_RENDERER_TYPE);
        }

        @Override
        public String getFamily()
        {
            return COMPONENT_FAMILY;
        }
    }

    @ListenerFor(systemEventClass=PostAddToViewEvent.class,
            sourceClass=UIComponentBase.class)
    @ResourceDependency(library = "testLib", name = "testResource.js")
    public static class TestRendererA extends Renderer
    {

    }

    @Test
    public void testCreateComponentRenderer() throws Exception
    {
        facesContext.getViewRoot().setRenderKitId(
                MockRenderKitFactory.HTML_BASIC_RENDER_KIT);
        facesContext.getRenderKit().addRenderer(
                UITestComponentA.COMPONENT_FAMILY,
                UITestComponentA.DEFAULT_RENDERER_TYPE, new TestRendererA());

        application.addComponent(UITestComponentA.COMPONENT_TYPE,
                UITestComponentA.class.getName());
        
        UITestComponentA comp = (UITestComponentA) application.createComponent(facesContext, 
                UITestComponentA.COMPONENT_TYPE,
                UITestComponentA.DEFAULT_RENDERER_TYPE);
        
        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(1,componentResources.size());
        Map<String,Object> attrMap = componentResources.get(0).getAttributes();
        Assert.assertEquals("testResource.js",attrMap.get("name"));
        Assert.assertEquals("testLib",attrMap.get("library"));
    }

    @ResourceDependency(name = "testResource.js")
    public static class UITestComponentB extends UIComponentBase
    {
        public static final String COMPONENT_TYPE = "javax.faces.TestComponentB";
        public static final String COMPONENT_FAMILY = "javax.faces.TestComponentB";
        public static final String DEFAULT_RENDERER_TYPE = "javax.faces.TestComponentB";

        public UITestComponentB()
        {
            setRendererType(DEFAULT_RENDERER_TYPE);
        }

        @Override
        public String getFamily()
        {
            return COMPONENT_FAMILY;
        }
    }

    @Test
    public void testCreateComponentAnnotation() throws Exception
    {
        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(UITestComponentB.COMPONENT_TYPE);
        
        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(1,componentResources.size());
        Map<String,Object> attrMap = componentResources.get(0).getAttributes();
        Assert.assertEquals("testResource.js",attrMap.get("name"));
    }
    
    @Test
    public void testCreateComponentRendererAnnotation() throws Exception
    {
        facesContext.getViewRoot().setRenderKitId(
                MockRenderKitFactory.HTML_BASIC_RENDER_KIT);
        facesContext.getRenderKit().addRenderer(
                UITestComponentB.COMPONENT_FAMILY,
                UITestComponentB.DEFAULT_RENDERER_TYPE, new TestRendererA());

        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(facesContext, 
                UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.DEFAULT_RENDERER_TYPE);
        
        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(2,componentResources.size());
        for (UIComponent component : componentResources)
        {
            if ("testResource.js".equals(component.getAttributes().get("name")))
            {
                //Good!
            }
            else
            {
                Assert.fail("Not expected resource found"+component.getAttributes().get("name"));
            }
        }
    }
    
    @Test
    public void testCreateComponentValueExpression1() throws Exception
    {
        
        ValueExpression expr = application.getExpressionFactory().createValueExpression(
                facesContext.getELContext(), "#{testComponent}", Object.class);
        
        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(
                expr, facesContext, UITestComponentB.COMPONENT_TYPE);
        
        List<UIComponent> componentResources = 
            facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(1,componentResources.size());
        Map<String,Object> attrMap = componentResources.get(0).getAttributes();
        Assert.assertEquals("testResource.js",attrMap.get("name"));
    }
    
    @Test
    public void testCreateComponentValueExpression2() throws Exception
    {
        
        ValueExpression expr = application.getExpressionFactory().createValueExpression(
                facesContext.getELContext(), "#{testComponent}", Object.class);
        
        expr.setValue(facesContext.getELContext(), new UITestComponentB());
        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(
                expr, facesContext, UITestComponentB.COMPONENT_TYPE);
        
        List<UIComponent> componentResources = 
            facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(1,componentResources.size());
        Map<String,Object> attrMap = componentResources.get(0).getAttributes();
        Assert.assertEquals("testResource.js",attrMap.get("name"));
    }

    @Test
    public void testCreateComponentValueExpression3() throws Exception
    {
        
        ValueExpression expr = application.getExpressionFactory().createValueExpression(
                facesContext.getELContext(), "#{testComponent}", Object.class);
        
        facesContext.getViewRoot().setRenderKitId(
                MockRenderKitFactory.HTML_BASIC_RENDER_KIT);
        facesContext.getRenderKit().addRenderer(
                UITestComponentB.COMPONENT_FAMILY,
                UITestComponentB.DEFAULT_RENDERER_TYPE, new TestRendererA());

        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(expr, facesContext, 
                UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.DEFAULT_RENDERER_TYPE);
        
        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(2,componentResources.size());
        for (UIComponent component : componentResources)
        {
            if ("testResource.js".equals(component.getAttributes().get("name")))
            {
                //Good!
            }
            else
            {
                Assert.fail("Not expected resource found"+component.getAttributes().get("name"));
            }
        }
    }
    
    @Test
    public void testCreateComponentValueExpression4() throws Exception
    {
        
        ValueExpression expr = application.getExpressionFactory().createValueExpression(
                facesContext.getELContext(), "#{testComponent}", Object.class);
        
        expr.setValue(facesContext.getELContext(), new UITestComponentB());
        
        facesContext.getViewRoot().setRenderKitId(
                MockRenderKitFactory.HTML_BASIC_RENDER_KIT);
        facesContext.getRenderKit().addRenderer(
                UITestComponentB.COMPONENT_FAMILY,
                UITestComponentB.DEFAULT_RENDERER_TYPE, new TestRendererA());

        application.addComponent(UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.class.getName());
        
        UITestComponentB comp = (UITestComponentB) application.createComponent(expr, facesContext, 
                UITestComponentB.COMPONENT_TYPE,
                UITestComponentB.DEFAULT_RENDERER_TYPE);
        
        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext, "head");
        Assert.assertEquals(2,componentResources.size());
        for (UIComponent component : componentResources)
        {
            if ("testResource.js".equals(component.getAttributes().get("name")))
            {
                //Good!
            }
            else
            {
                Assert.fail("Not expected resource found"+component.getAttributes().get("name"));
            }
        }
    }
    
    @Test
    public void testDatetimeconverterDefaultTimezoneIsSystemTimezoneInitParameter()
    {
        application.addConverter(java.util.Date.class, "javax.faces.convert.DateTimeConverter");
        Converter converter = application.createConverter(java.util.Date.class);
        Assert.assertEquals(((DateTimeConverter) converter).getTimeZone(), TimeZone.getDefault());
    }

}
