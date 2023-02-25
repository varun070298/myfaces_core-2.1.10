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
package javax.faces;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provide utility methods used by FactoryFinder class to lookup for SPI interface FactoryFinderProvider.
 *
 * @author Leonardo Uribe
 * @since 2.0.5
 */
class _FactoryFinderProviderFactory
{

    public static final String FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME = "org.apache.myfaces.spi" +
            ".FactoryFinderProviderFactory";

    public static final String FACTORY_FINDER_PROVIDER_CLASS_NAME = "org.apache.myfaces.spi.FactoryFinderProvider";

    public static final Class<?> FACTORY_FINDER_PROVIDER_FACTORY_CLASS;

    public static final Method FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD;

    public static final Method FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD;
    public static final Class<?> FACTORY_FINDER_PROVIDER_CLASS;
    public static final Method FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD;

    static
    {
        Class factoryFinderFactoryClass = null;
        Method factoryFinderproviderFactoryGetMethod = null;
        Method factoryFinderproviderFactoryGetFactoryFinderMethod = null;
        Class<?> factoryFinderProviderClass = null;

        Method factoryFinderProviderGetFactoryMethod = null;
        Method factoryFinderProviderSetFactoryMethod = null;
        Method factoryFinderProviderReleaseFactoriesMethod = null;

        try
        {
            factoryFinderFactoryClass = classForName(FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME);


            if (factoryFinderFactoryClass != null)
            {
                factoryFinderproviderFactoryGetMethod = factoryFinderFactoryClass.getMethod
                        ("getInstance", null);
                factoryFinderproviderFactoryGetFactoryFinderMethod = factoryFinderFactoryClass
                        .getMethod("getFactoryFinderProvider", null);
            }

            factoryFinderProviderClass = classForName(FACTORY_FINDER_PROVIDER_CLASS_NAME);
            if (factoryFinderProviderClass != null)
            {
                factoryFinderProviderGetFactoryMethod = factoryFinderProviderClass.getMethod("getFactory",
                        new Class[]{String.class});
                factoryFinderProviderSetFactoryMethod = factoryFinderProviderClass.getMethod("setFactory",
                        new Class[]{String.class, String.class});
                factoryFinderProviderReleaseFactoriesMethod = factoryFinderProviderClass.getMethod
                        ("releaseFactories", null);
            }
        }
        catch (Exception e)
        {
            // no op
        }

        FACTORY_FINDER_PROVIDER_FACTORY_CLASS = factoryFinderFactoryClass;
        FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD = factoryFinderproviderFactoryGetMethod;
        FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD = factoryFinderproviderFactoryGetFactoryFinderMethod;
        FACTORY_FINDER_PROVIDER_CLASS = factoryFinderProviderClass;

        FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD = factoryFinderProviderGetFactoryMethod;
        FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD = factoryFinderProviderSetFactoryMethod;
        FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD = factoryFinderProviderReleaseFactoriesMethod;
    }

    public static Object getInstance()
    {
        if (FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD != null)
        {
            try
            {
                return FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD.invoke(FACTORY_FINDER_PROVIDER_FACTORY_CLASS, null);
            }
            catch (Exception e)
            {
                //No op
                Logger log = Logger.getLogger(_FactoryFinderProviderFactory.class.getName());
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "Cannot retrieve current FactoryFinder instance from " +
                            "FactoryFinderProviderFactory." +
                            " Default strategy using thread context class loader will be used.", e);
                }
            }
        }
        return null;
    }

    // ~ Methods Copied from _ClassUtils
    // ------------------------------------------------------------------------------------

    /**
     * Tries a Class.loadClass with the context class loader of the current thread first and automatically falls back
     * to
     * the ClassUtils class loader (i.e. the loader of the myfaces.jar lib) if necessary.
     *
     * @param type fully qualified name of a non-primitive non-array class
     * @return the corresponding Class
     * @throws NullPointerException   if type is null
     * @throws ClassNotFoundException
     */
    public static Class<?> classForName(String type) throws ClassNotFoundException
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }
        try
        {
            // Try WebApp ClassLoader first
            return Class.forName(type, false, // do not initialize for faster startup
                    getContextClassLoader());
        }
        catch (ClassNotFoundException ignore)
        {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar lib)
            return Class.forName(type, false, // do not initialize for faster startup
                    _FactoryFinderProviderFactory.class.getClassLoader());
        }
    }

    /**
     * Gets the ClassLoader associated with the current thread. Returns the class loader associated with the specified
     * default object if no context loader is associated with the current thread.
     *
     * @return ClassLoader
     */
    protected static ClassLoader getContextClassLoader()
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Object cl = AccessController.doPrivileged(new PrivilegedExceptionAction()
                {
                    public Object run() throws PrivilegedActionException
                    {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
                return (ClassLoader) cl;
            }
            catch (PrivilegedActionException pae)
            {
                throw new FacesException(pae);
            }
        }
        else
        {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
