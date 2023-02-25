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

import static org.apache.myfaces.test.AssertThrowables.assertThrowable;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.EnumConverter;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.VariableResolver;

import junit.framework.TestCase;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.test.TestRunnable;
import org.apache.myfaces.test.el.MockELContext;
import org.apache.myfaces.test.mock.MockFacesContext12;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

/**
 * @author Mathias Broekelmann (latest modification by $Author: lu4242 $)
 * @version $Revision: 883907 $ $Date: 2009-11-24 17:37:53 -0500 (Tue, 24 Nov 2009) $
 */
public class ApplicationImplTest extends TestCase
{
    //TODO: need mock objects for VDL/VDLFactory
    //remove from excludes list in pom.xml after complete
    
    private ApplicationImpl app;
    private MockFacesContext12 context;

    protected void setUp() throws Exception
    {
        app = new ApplicationImpl(new RuntimeConfig());
        context = new MockFacesContext12();
    }
    
    public void testCreateMethodBinding() throws Exception
    {
        assertThrowable(ReferenceSyntaxException.class, new TestRunnable() {
            public void run() throws Throwable
            {
                app.createMethodBinding("xxx", null);
            }
        });
    }

    /**
     * Test method for
     * {@link org.apache.myfaces.application.ApplicationImpl#getResourceBundle(javax.faces.context.FacesContext, java.lang.String)}.
     */
    public void testGetResourceBundleNPE()
    {
        assertThrowable(NullPointerException.class, new TestRunnable()
        {
            public void run()
            {
                app.getResourceBundle(null, "xxx");
            }
        });
        assertThrowable(NullPointerException.class, new TestRunnable()
        {
            public void run()
            {
                app.getResourceBundle(context, null);
            }
        });
    }

    /**
     * <p>
     * Test if a {@link FacesException} is thrown if the specified resource bundle can not be found.
     * </p>
     * Test method for
     * {@link org.apache.myfaces.application.ApplicationImpl#getResourceBundle(javax.faces.context.FacesContext, java.lang.String)}.
     */
    public void testGetResourceBundleFacesException()
    {
        final ApplicationImpl myApp = new ApplicationImpl(new RuntimeConfig())
        {
            @Override
            String getBundleName(FacesContext facesContext, String name)
            {
                return "bundleName";
            }
        };
        assertThrowable(FacesException.class, new TestRunnable()
        {
            public void run()
            {
                myApp.getResourceBundle(context, "xxx");
            }
        });
    }

    /**
     * Test method for
     * {@link org.apache.myfaces.application.ApplicationImpl#getResourceBundle(javax.faces.context.FacesContext, java.lang.String)}.
     */
    public void testGetResourceBundleWithDefaultLocale()
    {
        assertGetResourceBundleWithLocale(Locale.getDefault());
    }

    /**
     * Test method for
     * {@link org.apache.myfaces.application.ApplicationImpl#getResourceBundle(javax.faces.context.FacesContext, java.lang.String)}.
     */
    public void testGetResourceBundleWithUIViewRootLocale()
    {
        Locale locale = new Locale("xx");
        UIViewRoot viewRoot = new UIViewRoot();
        context.setViewRoot(viewRoot);
        viewRoot.setLocale(locale);
        assertGetResourceBundleWithLocale(locale);
    }

    public void testCreateComponentCallSetValueOnExpressionIfValueNull() throws Exception
    {
        ValueExpression expr = createMock(ValueExpression.class);
        FacesContext context = createMock(FacesContext.class);
        ELContext elcontext = createMock(ELContext.class);
        expect(context.getELContext()).andReturn(elcontext);
        expect(expr.getValue(elcontext)).andReturn(null);
        expr.setValue(eq(elcontext), isA(UIOutput.class));
        app.addComponent("testComponent", UIOutput.class.getName());
        replay(context);
        replay(expr);
        assertTrue(UIOutput.class.isAssignableFrom(app.createComponent(expr, context, "testComponent").getClass()));
    }

    public void testCreateComponentExpressionFacesExceptionTest() throws Exception
    {
        ValueExpression expr = createMock(ValueExpression.class);
        FacesContext context = createMock(FacesContext.class);
        ELContext elcontext = createMock(ELContext.class);
        expect(context.getELContext()).andReturn(elcontext);
        expect(expr.getValue(elcontext)).andThrow(new IllegalArgumentException());
        replay(context);
        replay(expr);
        try
        {
            app.createComponent(expr, context, "testComponent");
        }
        catch (FacesException e)
        {
            // ok
        }
        catch (Throwable e)
        {
            fail("FacesException expected: " + e.getMessage());
        }
    }
    
    public void testGetVariableResolver() throws Exception
    {
        VariableResolver variableResolver = app.getVariableResolver();
        assertNotNull(variableResolver);
        IMocksControl mocksControl = EasyMock.createControl();
        Application mockApp = mocksControl.createMock(Application.class);
        context.setApplication(mockApp);        
        ELResolver elResolver = mocksControl.createMock(ELResolver.class);
        expect(mockApp.getELResolver()).andReturn(elResolver);
        context.setELContext(new MockELContext());
        expect(elResolver.getValue(eq(context.getELContext()), isNull(), eq("xxx"))).andReturn("testValue");
        mocksControl.replay();
        assertEquals("testValue", variableResolver.resolveVariable(context, "xxx"));
    }

    private void assertGetResourceBundleWithLocale(final Locale expectedLocale)
    {
        final String var = "test";
        final String bundleName = "bundleName";
        final ResourceBundle bundle = new ListResourceBundle()
        {
            @Override
            protected Object[][] getContents()
            {
                return null;
            }
        };
        ApplicationImpl myapp = new ApplicationImpl(new RuntimeConfig())
        {
            @Override
            String getBundleName(FacesContext facesContext, String name)
            {
                assertEquals(var, name);
                return bundleName;
            }

            @Override
            ResourceBundle getResourceBundle(String name, Locale locale, ClassLoader loader)
            {
                assertEquals(Thread.currentThread().getContextClassLoader(), loader);
                assertEquals(bundleName, name);
                assertEquals(expectedLocale, locale);
                return bundle;
            }
        };
        assertSame(bundle, myapp.getResourceBundle(context, var));
    }

    private enum MyEnum {VALUE1, VALUE2}; 

    /**
     * Test method for
     * {@link javax.faces.application.Application#createConverter(java.lang.Class)}.
     */
    public void testCreateEnumConverter() throws Exception
    {
        app.addConverter(Enum.class, EnumConverter.class.getName());

        Converter converter = app.createConverter(MyEnum.class);
        assertNotNull(converter);
        assertEquals(converter.getClass(), EnumConverter.class);
    }    
  

    private interface EnumCoded { public int getCode(); }
    private enum AnotherEnum implements EnumCoded { 
    	VALUE1, VALUE2;
		public int getCode() {return 0;}
	};
	
	public static class EnumCodedTestConverter implements Converter
	{

        public EnumCodedTestConverter()
        {
        }

        public Object getAsObject(FacesContext context, UIComponent component,
                String value) throws ConverterException
        {
            return null;
        }

        public String getAsString(FacesContext context, UIComponent component,
                Object value) throws ConverterException
        {
            return null;
        }
	}
	
    /**
     * Test method for
     * {@link javax.faces.application.Application#createConverter(java.lang.Class)}.
     * <p>
     * Tests the situation when a object is both, an enum and an implementor of an
     * interface for which we have a specific converter registered. 
     * The interface should take precedence over the fact that our object is also
     * an enum.
     */
    public void testCreateConverterForInterface() throws Exception 
    {
        app.addConverter(Enum.class, EnumConverter.class.getName());
    	app.addConverter(EnumCoded.class, EnumCodedTestConverter.class.getName());
    	
    	Converter converter = app.createConverter(AnotherEnum.class);
    	assertNotNull(converter);
        assertEquals(converter.getClass(), EnumCodedTestConverter.class);
    }
}
