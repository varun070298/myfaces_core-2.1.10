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
package org.apache.myfaces.view.facelets.compiler;

import java.util.ArrayList;
import java.util.List;
import javax.faces.context.ResponseWriter;
import org.apache.myfaces.view.facelets.FaceletTestCase;
import org.apache.myfaces.view.facelets.util.FastWriter;
import org.junit.Test;
import org.testng.Assert;

/**
 *
 * @author lu4242
 */
public class CompressSpacesTextUnitTestCase extends FaceletTestCase
{
    
    @Test
    public void testSimpleCompress1() throws Exception {
    
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        instructions.add(new LiteralTextInstruction("   "));
        instructions.add(new StartElementInstruction("p"));
        instructions.add(new LiteralTextInstruction(" hello "));
        instructions.add(new EndElementInstruction("p"));
        instructions.add(new StartElementInstruction("tr"));
        instructions.add(new StartElementInstruction("td"));
        instructions.add(new LiteralTextInstruction("   "));
        instructions.add(new EndElementInstruction("td"));
        instructions.add(new EndElementInstruction("tr"));
        instructions.add(new LiteralTextInstruction("   "));
        
        int size = instructions.size();
        size = TextUnit.compressSpaces(instructions, size);
        
        FastWriter fw = new FastWriter();
        ResponseWriter rw = facesContext.getResponseWriter();
        rw = rw.cloneWithWriter(fw);
        facesContext.setResponseWriter(rw);

        for (Instruction i : instructions)
        {
            i.write(facesContext);
        }
        
        Assert.assertEquals(fw.toString(), " <p> hello </p><tr><td/></tr> ");
    }

    @Test
    public void testSimpleCompress2() throws Exception {
        
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        instructions.add(new StartElementInstruction("p"));
        instructions.add(new LiteralTextInstruction("    hello   "));
        instructions.add(new EndElementInstruction("p"));

        int size = instructions.size();
        size = TextUnit.compressSpaces(instructions, size);

        FastWriter fw = new FastWriter();
        ResponseWriter rw = facesContext.getResponseWriter();
        rw = rw.cloneWithWriter(fw);
        facesContext.setResponseWriter(rw);

        for (Instruction i : instructions)
        {
            i.write(facesContext);
        }
        
        Assert.assertEquals(fw.toString(), "<p> hello </p>");

    }

    @Test
    public void testSimpleCompress3() throws Exception {
    
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        instructions.add(new LiteralTextInstruction("   "));
        instructions.add(new StartElementInstruction("p"));
        instructions.add(new LiteralTextInstruction("     hello    "));
        instructions.add(new EndElementInstruction("p"));
        instructions.add(new StartElementInstruction("tr"));
        instructions.add(new StartElementInstruction("td"));
        instructions.add(new LiteralTextInstruction("\n  "));
        instructions.add(new EndElementInstruction("td"));
        instructions.add(new EndElementInstruction("tr"));
        instructions.add(new LiteralTextInstruction("\n   "));
        
        int size = instructions.size();
        size = TextUnit.compressSpaces(instructions, size);
        
        FastWriter fw = new FastWriter();
        ResponseWriter rw = facesContext.getResponseWriter();
        rw = rw.cloneWithWriter(fw);
        facesContext.setResponseWriter(rw);

        for (Instruction i : instructions)
        {
            i.write(facesContext);
        }
        
        Assert.assertEquals(fw.toString(), " <p> hello </p><tr><td/></tr>\n");
    }
    
    @Test
    public void testSimpleCompress4() throws Exception {
        
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        instructions.add(new LiteralTextInstruction("  \n     "));

        int size = instructions.size();
        size = TextUnit.compressSpaces(instructions, size);

        FastWriter fw = new FastWriter();
        ResponseWriter rw = facesContext.getResponseWriter();
        rw = rw.cloneWithWriter(fw);
        facesContext.setResponseWriter(rw);

        for (Instruction i : instructions)
        {
            i.write(facesContext);
        }
        
        Assert.assertEquals(fw.toString(), " ");

    }

    @Test
    public void testSimpleCompress5() throws Exception {
        
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        instructions.add(new LiteralTextInstruction("&#160;\n     "));

        int size = instructions.size();
        size = TextUnit.compressSpaces(instructions, size);

        FastWriter fw = new FastWriter();
        ResponseWriter rw = facesContext.getResponseWriter();
        rw = rw.cloneWithWriter(fw);
        facesContext.setResponseWriter(rw);

        for (Instruction i : instructions)
        {
            i.write(facesContext);
        }
        
        Assert.assertEquals(fw.toString(), "&amp;#160;\n");

    }

    @Test
    public void testCompressOnELExpressions() throws Exception
    {
        Assert.assertEquals(tryCompress("#{bean.name}"), "#{bean.name}");
        Assert.assertEquals(tryCompress(" #{bean.name}"), " #{bean.name}");
        Assert.assertEquals(tryCompress("#{bean.name} "), "#{bean.name} ");
        String text = tryCompress("  #{bean.name}  ");
        Assert.assertEquals(tryCompress("  #{bean.name}  "), " #{bean.name} ");
        Assert.assertEquals(tryCompress("\n #{bean.name}\n "), "\n#{bean.name}\n");
    }
    
    public String tryCompress(String value)
    {
        //return TextUnit.compressELText(ELText.parse(value),value).toString();
        return TextUnit.compressELText(value);
    }
}
