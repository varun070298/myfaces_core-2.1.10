/*
 * Copyright 2004-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.myfaces.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.myfaces.shared.test.ClassElementHandler;

/**
 * This test makes sure all of our components, tags, renderers, validators, converters, action listeners, phase
 * listeners and core implementation classes are in the build.
 * 
 * This class has been copy and pasted into both tomahawk and core in order to avoid a compile scoped dependency on
 * junit in shared.
 * 
 * @see ClassElementHandler
 * @author Dennis Byrne
 */

public abstract class AbstractClassElementTestCase extends TestCase
{

    //private Log log = LogFactory.getLog(AbstractClassElementTestCase.class);
    private Logger log = Logger.getLogger(AbstractClassElementTestCase.class.getName());

    protected List<String> resource = new ArrayList<String>();
    private List<String> className = new ArrayList<String>();

    protected void setUp() throws Exception
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);

        SAXParser parser = factory.newSAXParser();
        ClassElementHandler handler = new ClassElementHandler();

        for (String resourceName : resource)
        {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);

            if (is == null)
            {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            }

            if (is == null)
            {
                throw new Exception("Could not locate resource :" + resourceName);
            }

            parser.parse(is, handler);

        }

        className.addAll(handler.getClassName());
    }

    public void testClassPath()
    {
        int i = -1;
        for (String clazz : className)
        {
            try
            {
                i++;
                getClass().getClassLoader().loadClass(clazz);
            }
            catch (ClassNotFoundException e)
            {
                try
                {
                    Thread.currentThread().getContextClassLoader().loadClass(clazz);
                }
                catch (ClassNotFoundException e2)
                {
                    assertFalse("Could not load " + clazz, true);
                }
            }
        }

        log.fine((i + 1) + " class found ");
    }
}
