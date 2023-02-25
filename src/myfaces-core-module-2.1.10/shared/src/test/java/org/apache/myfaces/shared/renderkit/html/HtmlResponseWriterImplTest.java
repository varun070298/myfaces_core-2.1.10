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
package org.apache.myfaces.shared.renderkit.html;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;

import org.apache.myfaces.shared.util.CommentUtils;
import org.apache.myfaces.test.base.AbstractJsfTestCase;

/**
 * Test class for HtmlResponseWriterImpl.
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 962932 $ $Date: 2010-07-10 17:37:50 -0500 (Sat, 10 Jul 2010) $
 */
public class HtmlResponseWriterImplTest extends AbstractJsfTestCase
{
    
    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END = "//-->";

    private StringWriter _stringWriter;
    private HtmlResponseWriterImpl _writer;
    
    public HtmlResponseWriterImplTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        _stringWriter = new StringWriter();
        _writer = new HtmlResponseWriterImpl(_stringWriter, "text/html", "ISO-8859-1");
    }

    @Override
    protected void tearDown() throws Exception
    {
        _writer = null;
        _stringWriter = null;
        
        super.tearDown();
    }
    
    /**
     * This test tests if it is possible to render HTML elements inside
     * a script section without confusing the HtmlResponseWriterImpl.
     * The related issue to this test is MYFACES-2668.
     * 
     * @throws IOException
     * @throws NoSuchFieldException 
     * @throws SecurityException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public void testHtmlElementsInsideScript() throws IOException, SecurityException, 
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        // use reflection to get the field _isInsideScript to verify
        // the internal behavior of HtmlResponseWriterImpl
        Field insideScriptField = _writer.getClass().getDeclaredField("_isInsideScript");
        insideScriptField.setAccessible(true);
        
        _writer.startDocument();
        _writer.startElement("head", null);
        
        assertFalse("We have not entered a script element yet, so _isInsideScript should be " +
                "false (or null).", getFieldBooleanValue(insideScriptField, _writer, false));
        
        _writer.startElement("script", null);
        
        assertTrue("We have now entered a script element, so _isInsideScript should be " +
                "true.", getFieldBooleanValue(insideScriptField, _writer, false));
        
        _writer.startElement("table", null);
        _writer.startElement("tr", null);
        _writer.startElement("td", null);
        
        assertTrue("We have now opened various elements inside a script element, "+
                "but _isInsideScript should still be true.",
                getFieldBooleanValue(insideScriptField, _writer, false));
        
        _writer.write("column value");
        
        assertTrue("We have now written some text inside a script element, "+
                "but _isInsideScript should still be true.",
                getFieldBooleanValue(insideScriptField, _writer, false));
        
        _writer.endElement("td");
        _writer.endElement("tr");
        _writer.endElement("table");
        _writer.endElement("script");
        
        assertFalse("We have now closed the script element, so _isInsideScript should be " +
                "false (or null).", getFieldBooleanValue(insideScriptField, _writer, false));
        
        _writer.endElement("head");
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertTrue("A script start was rendered, so the output has to " +
                "contain " + COMMENT_START, output.contains(COMMENT_START));
        assertTrue("A script end was rendered so the output has to " + 
                "contain " + COMMENT_END, output.contains(COMMENT_END));
    }
    
    /**
     * Utility method to get the value of the given Field, which is of
     * type java.lang.Boolean. If it is null, the given defaulValue will
     * be returned.
     * 
     * @param field
     * @param instance
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private boolean getFieldBooleanValue(Field field, Object instance, boolean defaultValue) 
            throws IllegalArgumentException, IllegalAccessException
    {
        Boolean b = (Boolean) field.get(instance);
        return b == null ? defaultValue : b;
    }

    public void testScriptOnHtmlIsoEncodingAndScriptXhmlComments() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "text/html", "ISO-8859-1", true);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start comment <!-- ", output.contains(CommentUtils.COMMENT_SIMPLE_START));
        assertTrue("script does not have end comment --> ", output.contains("//"+CommentUtils.COMMENT_SIMPLE_END));
    }
    
    public void testScriptOnHtmlIsoEncodingAndNoScriptXhmlComments() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "text/html", "ISO-8859-1", false);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertFalse("script have start comment <!-- ", output.contains(CommentUtils.COMMENT_SIMPLE_START));
        assertFalse("script have end comment --> ", output.contains("//"+CommentUtils.COMMENT_SIMPLE_END));
    }

    public void testScriptOnHtmlUTF8AndScriptXhmlComments() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "text/html", "UTF-8", true);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start comment <!-- ", output.contains(CommentUtils.COMMENT_SIMPLE_START));
        assertTrue("script does not have end comment --> ", output.contains("//"+CommentUtils.COMMENT_SIMPLE_END));
    }
    
    public void testScriptOnHtmlUTF8AndNoScriptXhmlComments() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "text/html", "UTF-8", false);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertFalse("script have start comment <!-- ", output.contains(CommentUtils.COMMENT_SIMPLE_START));
        assertFalse("script have end comment --> ", output.contains("//"+CommentUtils.COMMENT_SIMPLE_END));
    }

    public void testScriptOnXhtmlIsoEncoding() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xhtml+xml", "ISO-8859-1", true);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start <![CDATA[ ", output.contains(CommentUtils.INLINE_SCRIPT_COMMENT+CommentUtils.CDATA_SIMPLE_START));
        assertTrue("script does not have end ]]> ", output.contains(CommentUtils.INLINE_SCRIPT_COMMENT+CommentUtils.CDATA_SIMPLE_END));
    }

    public void testScriptOnXhtmlUTF8Encoding() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xhtml+xml", "UTF-8", false);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.SCRIPT_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.SCRIPT_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start <![CDATA[ ", output.contains(CommentUtils.INLINE_SCRIPT_COMMENT+CommentUtils.CDATA_SIMPLE_START));
        assertTrue("script does not have end ]]> ", output.contains(CommentUtils.INLINE_SCRIPT_COMMENT+CommentUtils.CDATA_SIMPLE_END));
    }
    
    public void testStyleOnXhtmlIsoEncoding() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xhtml+xml", "ISO-8859-1", true);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.STYLE_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.STYLE_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start <![CDATA[ ", output.contains(CommentUtils.CDATA_SIMPLE_START));
        assertTrue("script does not have end ]]> ", output.contains(CommentUtils.CDATA_SIMPLE_END));
    }

    public void testStyleOnXhtmlUTF8Encoding() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xhtml+xml", "UTF-8", false);
        String innerScript = "document.write('HELLO');"; 
        _writer.startDocument();
        _writer.startElement(HTML.STYLE_ELEM, null);
        _writer.write(innerScript);
        _writer.endElement(HTML.STYLE_ELEM);
        _writer.endDocument();
        
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue("script does not contain body:" + innerScript, output.contains(innerScript));
        assertTrue("script does not have start <![CDATA[ ", output.contains(CommentUtils.CDATA_SIMPLE_START));
        assertTrue("script does not have end ]]> ", output.contains(CommentUtils.CDATA_SIMPLE_END));
    }
    
    /**
     * In html, it is not valid to have an empty tag with content
     * 
     * @throws IOException
     */
    public void testEmptyTagNotRenderEnd() throws IOException
    {
        _writer.startDocument();
        _writer.startElement("body", null);
        _writer.startElement("br", null);
        _writer.writeText("hello", null);
        _writer.endElement("br");
        _writer.endElement("body");
        _writer.endDocument();
       
        // the following should render <br />hello
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue(output.contains("<br />"));
        assertFalse(output.contains("</br>"));
    }
    
    /**
     * In xhtml, it is valid to have an html empty tag with content.
     * 
     * @throws IOException
     */
    public void testEmptyTagNotRenderEndOnXml() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xml", "UTF-8", false);
        
        _writer.startDocument();
        _writer.startElement("body", null);
        _writer.startElement("br", null);
        _writer.writeText("hello", null);
        _writer.endElement("br");
        _writer.endElement("body");
        _writer.endDocument();
        
     // the following should render <br>hello</br>
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue(output.contains("<br>"));
        assertTrue(output.contains("</br>"));
    }
    
    /**
     * In html, it is not valid to have an empty tag with content
     * 
     * @throws IOException
     */
    public void testEmptyTagNotRenderEndUppercase() throws IOException
    {
        _writer.startDocument();
        _writer.startElement("body", null);
        _writer.startElement("BR", null);
        _writer.writeText("hello", null);
        _writer.endElement("BR");
        _writer.endElement("body");
        _writer.endDocument();
       
        // the following should render <br />hello
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue(output.contains("<BR />"));
        assertFalse(output.contains("</BR>"));
    }
    
    /**
     * In xhtml, it is valid to have an html empty tag with content.
     * 
     * @throws IOException
     */
    public void testEmptyTagNotRenderEndOnXhtmlUppercase() throws IOException
    {
        _writer = new HtmlResponseWriterImpl(_stringWriter, "application/xml", "UTF-8", false);
        
        _writer.startDocument();
        _writer.startElement("body", null);
        _writer.startElement("BR", null);
        _writer.writeText("hello", null);
        _writer.endElement("BR");
        _writer.endElement("body");
        _writer.endDocument();
        
     // the following should render <br>hello</br>
        String output = _stringWriter.toString();
        assertNotNull(output);
        assertTrue(output.contains("<BR>"));
        assertTrue(output.contains("</BR>"));
    }
}