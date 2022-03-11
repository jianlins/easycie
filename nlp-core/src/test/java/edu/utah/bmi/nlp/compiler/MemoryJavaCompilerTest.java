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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class MemoryJavaCompilerTest {
    private String defaultSuperTypeName = "edu.utah.bmi.nlp.type.system.Concept";
    private static MemoryJavaCompiler compiler;

    @BeforeAll
    public static void initLoader(){
        MemoryClassLoader.CURRENT_LOADER_NAME ="testComipler";
    }
    @AfterAll
    public static void resetLoader(){
        MemoryClassLoader.resetLoaderName();
    }

    @BeforeEach
    public void init() {
        compiler = new MemoryJavaCompiler(new File("target/generated-test-sources"));
    }

    @Test
    public void compileStaticMethodTest()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        final String source = "package edu.utah.bmi.compiler;\n" +
                "public final class Solution {\n"
                + "public static String greeting(String name) {\n"
                + "\treturn \"Hello \" + name;\n" + "}\n}\n";
        final Method greeting = compiler.compileStaticMethod("greeting", "edu.utah.bmi.compiler.Solution", source);
        final Object result = greeting.invoke(null, "soulmachine");
        assertEquals("Hello soulmachine", result.toString());
    }

    @Test
    public void compileClassTest()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        final String source = "package edu.utah.bmi.compiler;\n" +
                "public final class Solution {\n"
                + "public static String greeting(String name) {\n"
                + "\treturn \"Hello \" + name;\n" + "}\n}\n";
        Class greeting = compiler.compileClass("edu.utah.bmi.compiler.Solution", source);
        assertEquals(greeting.getCanonicalName(), "edu.utah.bmi.compiler.Solution");
    }

    @Test
    public void batchCompileTest() throws IOException {
        compiler = new MemoryJavaCompiler(new File("target/generated-test-sources"));
        String digit2 = FileUtils.readFileToString(new File("src/test/resources/source/Digit2.java"), StandardCharsets.UTF_8);
        String digit2type = FileUtils.readFileToString(new File("src/test/resources/source/Digit2_Type.java"), StandardCharsets.UTF_8);
        compiler.addClassSrc("edu.utah.bmi.nlp.type.system.Digit2", digit2);
        compiler.addClassSrc("edu.utah.bmi.nlp.type.system.Digit2_Type", digit2type);
        Map<String, Class> classes = compiler.compileBatch();
        Class digit2Class = classes.get("edu.utah.bmi.nlp.type.system.Digit2");
        for (Method method : digit2Class.getMethods()) {
            System.out.println(method.getName());
        }
    }

    @Test
    public void batchCompileTest2() throws IOException {
        compiler = new MemoryJavaCompiler();
        String digit2 = FileUtils.readFileToString(new File("src/test/resources/source/Digit2.java"), StandardCharsets.UTF_8);
        String digit2type = FileUtils.readFileToString(new File("src/test/resources/source/Digit2_Type.java"), StandardCharsets.UTF_8);
        compiler.addClassSrc(digit2);
        compiler.addClassSrc(digit2type);
        Map<String, Class> classes = compiler.compileBatch();
        Class digit2Class = classes.get("edu.utah.bmi.nlp.type.system.Digit2");
        for (Method method : digit2Class.getMethods()) {
            System.out.println(method.getName());
        }
        try {
            Class cls=Class.forName("edu.utah.bmi.nlp.type.system.Digit2");
            System.out.println(cls);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            assertFalse(true);
        }
    }

    @Test
    public void batchCompileToSystemLoader() throws IOException {
        compiler = new MemoryJavaCompiler(new File("target/generated-test-sources"));
        String digit2 = FileUtils.readFileToString(new File("src/test/resources/source/Digit2.java"), StandardCharsets.UTF_8);
        String digit2type = FileUtils.readFileToString(new File("src/test/resources/source/Digit2_Type.java"), StandardCharsets.UTF_8);
        compiler.addClassSrc(digit2);
        compiler.addClassSrc(digit2type);
        Map<String, Class> classes =compiler.compileBatchToSystem(MemoryClassLoader.CURRENT_LOADER_NAME);
        Class digit2Class = classes.get("edu.utah.bmi.nlp.type.system.Digit2");
        System.out.println(digit2Class);

        try {
            digit2Class = Class.forName("edu.utah.bmi.nlp.type.system.Digit2", true, MemoryClassLoader.getInstance(MemoryClassLoader.CURRENT_LOADER_NAME));
            System.out.println(digit2Class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            assertFalse(true);
        }
    }
}
