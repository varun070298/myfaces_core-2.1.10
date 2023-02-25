/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.apache.myfaces.context;

import junit.framework.TestCase;

/**
 * Regular Expression tests used within the faces context submodules
 *
 * @author Werner Punz(latest modification by $Author: struberg $)
 * @version $Revision: 1188694 $ $Date: 2011-10-25 10:07:44 -0500 (Tue, 25 Oct 2011) $
 */
public class ContextRegexpTest extends TestCase {

    static final String RE_SPLITTER = "[\\s\\t\\r\\n]*\\,[\\s\\t\\r\\n]*";


 

   
    /**
     * condition valid string
     */
    public void testCondition1() {
        String[] splitted = " hello ,world          \n ,bla ".split(RE_SPLITTER);
        splitted[0] = splitted[0].trim();
        int len = splitted.length-1;
        if(len > 0) {//all others trimmed by the re
            splitted[len] =  splitted[len].trim();
        }
        assertTrue("length assertion", splitted.length == 3);
        assertTrue(splitted[0].trim().equals("hello"));
        assertTrue(splitted[1].trim().equals("world"));
        assertTrue(splitted[2].trim().equals("bla"));
    }

    /**
     * test the condition 2
     * empty string
     */
    public void testCondition2() {
        String[] splitted = " ".split(RE_SPLITTER);
        assertTrue(splitted.length == 1);
        assertTrue(splitted[0] != null);
    }

    /**
     * test the condition 3
     * empty string no blanks
     */
    public void testCondition3() {
        String[] splitted = "".split(RE_SPLITTER);
        assertTrue(splitted.length == 1);
        assertTrue(splitted[0] != null);
    }
}
