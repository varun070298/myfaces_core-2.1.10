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
package org.apache.myfaces.webapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.FacesConfigValidator;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.config.ManagedBeanBuilder;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.shared.context.ExceptionHandlerImpl;
import org.apache.myfaces.shared.util.StateUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.spi.WebConfigProvider;
import org.apache.myfaces.spi.WebConfigProviderFactory;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

import javax.el.ExpressionFactory;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.PreDestroyApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.myfaces.util.ExternalSpecifications;

/**
 * Performs common initialization tasks.
 */
public abstract class AbstractFacesInitializer implements FacesInitializer
{
    /**
     * The logger instance for this class.
     */
    //private static final Log log = LogFactory.getLog(AbstractFacesInitializer.class);
    private static final Logger log = Logger.getLogger(AbstractFacesInitializer.class.getName());
    
    /**
     * If the servlet mapping for the FacesServlet is added dynamically, Boolean.TRUE 
     * is stored under this key in the ServletContext.
     * ATTENTION: this constant is duplicate in MyFacesContainerInitializer.
     */
    private static final String FACES_SERVLET_ADDED_ATTRIBUTE = "org.apache.myfaces.DYNAMICALLY_ADDED_FACES_SERVLET";

    /**
     * This parameter specifies the ExpressionFactory implementation to use.
     */
    @JSFWebConfigParam(since="1.2.7", group="EL")
    protected static final String EXPRESSION_FACTORY = "org.apache.myfaces.EXPRESSION_FACTORY";
    
    /**
     * If this param is set to true, the check for faces servlet mapping is not done 
     */
    @JSFWebConfigParam(since="2.0.3", defaultValue="false")
    protected static final String INITIALIZE_ALWAYS_STANDALONE = "org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE";
    
    /**
     * Indicate if log all web config params should be done before initialize the webapp. 
     * <p>
     * If is set in "auto" mode, web config params are only logged on "Development" and "Production" project stages.
     * </p> 
     */
    @JSFWebConfigParam(expectedValues="true, auto, false", defaultValue="auto")
    public static final String INIT_PARAM_LOG_WEB_CONTEXT_PARAMS = "org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS";
    public static final String INIT_PARAM_LOG_WEB_CONTEXT_PARAMS_DEFAULT ="auto";

    /**
     * Performs all necessary initialization tasks like configuring this JSF
     * application.
     */
    public void initFaces(ServletContext servletContext)
    {
        try
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Initializing MyFaces");
            }

            // Some parts of the following configuration tasks have been implemented 
            // by using an ExternalContext. However, that's no problem as long as no 
            // one tries to call methods depending on either the ServletRequest or 
            // the ServletResponse.
            // JSF 2.0: FacesInitializer now has some new methods to
            // use proper startup FacesContext and ExternalContext instances.
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            // Parse and validate the web.xml configuration file
            
            if (!WebConfigParamUtils.getBooleanInitParameter(externalContext, INITIALIZE_ALWAYS_STANDALONE, false))
            {
                WebConfigProvider webConfigProvider = WebConfigProviderFactory.getWebConfigProviderFactory(
                        facesContext.getExternalContext()).getWebConfigProvider(facesContext.getExternalContext());

                if (webConfigProvider.getFacesServletMappings(facesContext.getExternalContext()).isEmpty())
                {
                    // check if the FacesServlet has been added dynamically
                    // in a Servlet 3.0 environment by MyFacesContainerInitializer
                    Boolean mappingAdded = (Boolean) servletContext.getAttribute(FACES_SERVLET_ADDED_ATTRIBUTE);
                    if (mappingAdded == null || !mappingAdded)
                    {
                        if (log.isLoggable(Level.WARNING))
                        {
                            log.warning("No mappings of FacesServlet found. Abort initializing MyFaces.");
                        }
                        return;
                    }
                }
            }

            initContainerIntegration(servletContext, externalContext);

            String useEncryption = servletContext.getInitParameter(StateUtils.USE_ENCRYPTION);
            if (!"false".equals(useEncryption)) // the default value is true
            {
                StateUtils.initSecret(servletContext);
            }

            // initialize eager managed beans
            _createEagerBeans(facesContext);

            _dispatchApplicationEvent(servletContext, PostConstructApplicationEvent.class);

            if ( (facesContext.isProjectStage(ProjectStage.Development) || 
                  facesContext.isProjectStage(ProjectStage.Production)) &&
                 log.isLoggable(Level.INFO))
            {
                log.info("ServletContext initialized.");
            }

            WebConfigParamsLogger.logWebContextParams(facesContext);
            
            //Force output EL message
            ExternalSpecifications.isUnifiedELAvailable();
            ExternalSpecifications.isBeanValidationAvailable();

            // print out a very prominent log message if the project stage is != Production
            if (!facesContext.isProjectStage(ProjectStage.Production) &&
                !facesContext.isProjectStage(ProjectStage.UnitTest))
            {
                ProjectStage projectStage = facesContext.getApplication().getProjectStage();
                StringBuilder message = new StringBuilder("\n\n");
                message.append("*******************************************************************\n");
                message.append("*** WARNING: Apache MyFaces-2 is running in ");
                message.append(projectStage.name().toUpperCase());        
                message.append(" mode.");
                int length = projectStage.name().length();
                for (int i = 0; i < 11 - length; i++)
                {
                    message.append(" ");
                }
                message.append("   ***\n");
                message.append("***                                         ");
                for (int i = 0; i < length; i++)
                {
                    message.append("^");
                }
                for (int i = 0; i < 20 - length; i++)
                {
                    message.append(" ");
                }
                message.append("***\n");
                message.append("*** Do NOT deploy to your live server(s) without changing this. ***\n");
                message.append("*** See Application#getProjectStage() for more information.     ***\n");
                message.append("*******************************************************************\n");
                log.log(Level.WARNING, message.toString());
            }

        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, "An error occured while initializing MyFaces: "
                      + ex.getMessage(), ex);
        }
    }
    
    /**
     * Checks for application scoped managed-beans with eager=true,
     * creates them and stores them in the application map.
     * @param facesContext
     */
    private void _createEagerBeans(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
        List<ManagedBean> eagerBeans = new ArrayList<ManagedBean>();
        
        // check all registered managed-beans
        for (ManagedBean bean : runtimeConfig.getManagedBeans().values())
        {
            String eager = bean.getEager();
            if (eager != null && "true".equals(eager))
            {
                // eager beans are only allowed for application scope
                if (ManagedBeanBuilder.APPLICATION.equals(bean.getManagedBeanScope()))
                {
                    // add to eager beans
                    eagerBeans.add(bean);
                }
                else
                {
                    // log warning and continue (the bean will be lazy loaded)
                    log.log(Level.WARNING, "The managed-bean with name "
                            + bean.getManagedBeanName()
                            + " must be application scoped to support eager=true.");
                }
            }
        }
        
        // check if there are any eager beans
        if (!eagerBeans.isEmpty())
        {
            ManagedBeanBuilder managedBeanBuilder = new ManagedBeanBuilder();
            Map<String, Object> applicationMap = externalContext.getApplicationMap();
            
            for (ManagedBean bean : eagerBeans)
            {
                // check application scope for bean instance
                if (applicationMap.containsKey(bean.getManagedBeanName()))
                {
                    // do not build bean, because it already exists
                    // (e.g. @ManagedProperty from previous managed bean already created it)
                    continue;
                }

                // create instance
                Object beanInstance = managedBeanBuilder.buildManagedBean(facesContext, bean);
                
                // put in application scope
                applicationMap.put(bean.getManagedBeanName(), beanInstance);
            }
        }
    }

    /**
     * Eventually we can use our plugin infrastructure for this as well
     * it would be a cleaner interception point than the base class
     * but for now this position is valid as well
     * <p/>
     * Note we add it for now here because the application factory object
     * leaves no possibility to have a destroy interceptor
     * and applications are per web application singletons
     * Note if this does not work out
     * move the event handler into the application factory
     *
     * @param servletContext the servlet context to be passed down
     * @param eventClass     the class to be passed down into the dispatching
     *                       code
     */
    private void _dispatchApplicationEvent(ServletContext servletContext, Class<? extends SystemEvent> eventClass)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();
        application.publishEvent(facesContext, eventClass, Application.class, application);
    }
    
    /**
     * Cleans up all remaining resources (well, theoretically).
     */
    public void destroyFaces(ServletContext servletContext)
    {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (!WebConfigParamUtils.getBooleanInitParameter(facesContext.getExternalContext(),
                                                         INITIALIZE_ALWAYS_STANDALONE, false))
        {
            //We need to check if the current application was initialized by myfaces
            WebConfigProvider webConfigProvider = WebConfigProviderFactory.getWebConfigProviderFactory(
                    facesContext.getExternalContext()).getWebConfigProvider(facesContext.getExternalContext());

            if (webConfigProvider.getFacesServletMappings(facesContext.getExternalContext()).isEmpty())
            {
                // check if the FacesServlet has been added dynamically
                // in a Servlet 3.0 environment by MyFacesContainerInitializer
                Boolean mappingAdded = (Boolean) servletContext.getAttribute(FACES_SERVLET_ADDED_ATTRIBUTE);
                if (mappingAdded == null || !mappingAdded)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("No mappings of FacesServlet found. Abort destroy MyFaces.");
                    }
                    return;
                }
            }
        }

        _dispatchApplicationEvent(servletContext, PreDestroyApplicationEvent.class);

        // clear the cache of MetaRulesetImpl in order to prevent a memory leak
        MetaRulesetImpl.clearMetadataTargetCache();
        
        // clear UIViewParameter default renderer map
        try
        {
            Class<?> c = Class.forName("javax.faces.component.UIViewParameter");
            Method m = c.getDeclaredMethod("releaseRenderer");
            m.setAccessible(true);
            m.invoke(null);
        }
        catch(ClassNotFoundException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(NoSuchMethodException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(IllegalAccessException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(InvocationTargetException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

        // TODO is it possible to make a real cleanup?
    }

    /**
     * Configures this JSF application. It's required that every
     * FacesInitializer (i.e. every subclass) calls this method during
     * initialization.
     *
     * @param servletContext    the current ServletContext
     * @param externalContext   the current ExternalContext
     * @param expressionFactory the ExpressionFactory to use
     * @return the current runtime configuration
     */
    protected RuntimeConfig buildConfiguration(ServletContext servletContext,
                                               ExternalContext externalContext, ExpressionFactory expressionFactory)
    {
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
        runtimeConfig.setExpressionFactory(expressionFactory);

        // And configure everything
        new FacesConfigurator(externalContext).configure();

        validateFacesConfig(servletContext, externalContext);

        return runtimeConfig;
    }

    protected void validateFacesConfig(ServletContext servletContext, ExternalContext externalContext)
    {
        String validate = servletContext.getInitParameter(FacesConfigValidator.VALIDATE_CONTEXT_PARAM);
        if ("true".equals(validate) && log.isLoggable(Level.WARNING))
        { // the default value is false
            List<String> warnings = FacesConfigValidator.validate(
                    externalContext);

            for (String warning : warnings)
            {
                log.warning(warning);
            }
        }
    }

    /**
     * Try to load user-definied ExpressionFactory. Returns <code>null</code>,
     * if no custom ExpressionFactory was specified.
     *
     * @param externalContext the current ExternalContext
     * @return User-specified ExpressionFactory, or
     *         <code>null</code>, if no no custom implementation was specified
     */
    protected static ExpressionFactory getUserDefinedExpressionFactory(ExternalContext externalContext)
    {
        String expressionFactoryClassName
                = WebConfigParamUtils.getStringInitParameter(externalContext, EXPRESSION_FACTORY);
        if (expressionFactoryClassName != null
                && expressionFactoryClassName.trim().length() > 0)
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("Attempting to load the ExpressionFactory implementation "
                        + "you've specified: '" + expressionFactoryClassName + "'.");
            }

            return loadExpressionFactory(expressionFactoryClassName);
        }

        return null;
    }

    /**
     * Loads and instantiates the given ExpressionFactory implementation.
     *
     * @param expressionFactoryClassName the class name of the ExpressionFactory implementation
     * @return the newly created ExpressionFactory implementation, or
     *         <code>null</code>, if an error occurred
     */
    protected static ExpressionFactory loadExpressionFactory(String expressionFactoryClassName)
    {
        try
        {
            Class<?> expressionFactoryClass = Class.forName(expressionFactoryClassName);
            return (ExpressionFactory) expressionFactoryClass.newInstance();
        }
        catch (Exception ex)
        {
            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "An error occured while instantiating a new ExpressionFactory. "
                        + "Attempted to load class '" + expressionFactoryClassName + "'.", ex);
            }
        }

        return null;
    }

    public FacesContext initStartupFacesContext(ServletContext servletContext)
    {
        // We cannot use FacesContextFactory, because it is necessary to initialize 
        // before Application and RenderKit factories, so we should use different object. 
        return _createFacesContext(servletContext, true);
    }
        
    public void destroyStartupFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    public FacesContext initShutdownFacesContext(ServletContext servletContext)
    {
        return _createFacesContext(servletContext, false);
    }
        
    public void destroyShutdownFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    private FacesContext _createFacesContext(ServletContext servletContext, boolean startup)
    {
        ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, startup);
        ExceptionHandler exceptionHandler = new ExceptionHandlerImpl();
        FacesContext facesContext = new StartupFacesContextImpl(externalContext, 
                (ReleaseableExternalContext) externalContext, exceptionHandler, startup);
        
        // If getViewRoot() is called during application startup or shutdown, 
        // it should return a new UIViewRoot with its locale set to Locale.getDefault().
        UIViewRoot startupViewRoot = new UIViewRoot();
        startupViewRoot.setLocale(Locale.getDefault());
        facesContext.setViewRoot(startupViewRoot);
        
        return facesContext;
    }
    
    private void _releaseFacesContext(FacesContext facesContext)
    {        
        // make sure that the facesContext gets released.
        // This is important in an OSGi environment 
        if (facesContext != null)
        {
            facesContext.release();
        }        
    }
    
    /**
     * Performs initialization tasks depending on the current environment.
     *
     * @param servletContext  the current ServletContext
     * @param externalContext the current ExternalContext
     */
    protected abstract void initContainerIntegration(
            ServletContext servletContext, ExternalContext externalContext);

}
