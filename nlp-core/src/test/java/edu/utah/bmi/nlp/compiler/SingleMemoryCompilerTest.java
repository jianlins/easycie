/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.compiler;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class SingleMemoryCompilerTest {
    private String defaultSuperTypeName = "edu.utah.bmi.nlp.type.system.Concept";
    private static MemoryJavaCompiler compiler;

    @BeforeEach
    public void init() {
        compiler = new MemoryJavaCompiler(new File("target/classes"));
        MemoryClassLoader.CURRENT_LOADER_NAME=DeterminantValueSet.randomString();
    }

    @AfterAll
    public static void resetLoader(){
        MemoryClassLoader.resetLoaderName();
    }

    @Test
    public void batchCompileToSystemLoader() throws IOException {
        compiler = new MemoryJavaCompiler(new File("target/generated-test-sources"));
        String digit2 = FileUtils.readFileToString(new File("src/test/resources/source/Digit2.java"), StandardCharsets.UTF_8);
        String digit2type = FileUtils.readFileToString(new File("src/test/resources/source/Digit2_Type.java"), StandardCharsets.UTF_8);
        compiler.addClassSrc(digit2);
        compiler.addClassSrc(digit2type);
        compiler.compileBatchToSystem("testcompiler");
        SecurityManager sm = System.getSecurityManager();
        String name = "edu.utah.bmi.nlp.type.system.Digit2";
        if (sm != null) {
            int i = name.lastIndexOf('.');
            if (i != -1) {
                String sect = name.substring(0, i);
                System.out.println(sect);
                sm.checkPackageAccess(sect);
            }
        }
        try {
            Class digit2Class = Class.forName(defaultSuperTypeName);
            digit2Class = Class.forName("edu.utah.bmi.nlp.type.system.Digit2", true, MemoryClassLoader.getInstance("testcompiler"));
            System.out.println(digit2Class);
            digit2Class =MemoryClassLoader.getInstance("testcompiler").load("edu.utah.bmi.nlp.type.system.Digit2");
            System.out.println(digit2Class);
            Class typeClass = AnnotationOper.getUIMAClass("edu.utah.bmi.nlp.type.system.Digit2_Type");
            System.out.println(digit2Class);
            System.out.println(typeClass);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            assertFalse(true);
        }
        assertTrue(true);
    }
}
