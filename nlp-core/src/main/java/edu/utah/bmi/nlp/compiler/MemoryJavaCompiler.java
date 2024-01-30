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

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple interface to Java compiler using JSR 199 Compiler API.
 */
public class MemoryJavaCompiler {
    private final JavaCompiler tool;
    private final StandardJavaFileManager stdManager;
    private File outputDir = null;
    private final MemoryJavaFileManager fileManager;
    private final List<JavaFileObject> compUnits;
    private Map<String, Class> compiledClasses;

    /**
     * Allows to optionally output compiled class into class file.
     * See example code in MemoryJavaCompilerTest
     *
     * @param paras An array of Objects
     *              1st object is a File to define where to output the class files
     *              2nd object is a Map
     */
    public MemoryJavaCompiler(Object... paras) {
        outputDir = new File("target/classes");
        if (paras != null && paras.length > 0)
            outputDir = (File) paras[0];
        if (paras != null && paras.length > 1 && paras[1] instanceof Map) {
            compiledClasses = (Map<String, Class>) paras[1];
        }
        tool = ToolProvider.getSystemJavaCompiler();
        if (tool == null) {
            throw new RuntimeException("Could not get Java compiler. Please, ensure that JDK is used instead of JRE.");
        }
        stdManager = tool.getStandardFileManager(null, null, null);
        if (outputDir != null)
            fileManager = new MemoryJavaFileManager(stdManager, outputDir);
        else
            fileManager = new MemoryJavaFileManager(stdManager);
        compUnits = new ArrayList<>();
        compiledClasses = new HashMap<>();
    }


    public Class compileClass(final String className,
                              final String source)
            throws ClassNotFoundException {
        final Map<String, byte[]> classBytes = compile(className + ".java", source);
        final MemoryClassLoader classLoader = new MemoryClassLoader(classBytes);
        final Class clazz = classLoader.loadClass(className);
        return clazz;
    }

    public Method compileStaticMethod(final String methodName, final String className,
                                      final String source)
            throws ClassNotFoundException {
        final Map<String, byte[]> classBytes = compile(className + ".java", source);
        final MemoryClassLoader classLoader = new MemoryClassLoader(classBytes);
        final Class clazz = classLoader.loadClass(className);
        final Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods) {
            if (method.getName().equals(methodName)) {
                if (!method.isAccessible()) method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodError(methodName);
    }


    public Map<String, byte[]> compile(String fileName, String source) {
        return compile(fileName, source, new PrintWriter(System.err), null, null);
    }

    private String readPackageName(String src) {
        int begin = src.indexOf("\npackage ") + 8;
        int end = src.indexOf(";", begin);
        return src.substring(begin, end).trim();
    }

    private String readClassName(String src) {
        int begin = src.indexOf("\npublic class ") + 13;
        int end = src.indexOf("\n", begin);
        src = src.substring(begin, end).trim();
        end = src.indexOf(" ");
        return src.substring(0, end);
    }

    public void addClassSrc(String source) {
        String classCanonicalName = "";
        String packageName = readPackageName(source);
        classCanonicalName = readClassName(source);
        if (packageName.length() > 0)
            classCanonicalName = packageName + "." + classCanonicalName;
        compUnits.add(MemoryJavaFileManager.makeStringSource(classCanonicalName + ".java", source));
    }


    public void addClassSrc(String classCanonicalName, String source) {
        compUnits.add(MemoryJavaFileManager.makeStringSource(classCanonicalName + ".java", source));
    }

    public Map<String, Class> compileBatch() {
        return compileBatch("uima");
    }

    public Map<String, Class> compileBatch(String compilerName) {
        Map<String, byte[]> batchClassBytes = compile(compUnits, fileManager, new PrintWriter(System.err), null, null);
        MemoryClassLoader classLoader =  MemoryClassLoader.getInstance(compilerName, batchClassBytes);
        try {
            classLoader.addURL(outputDir.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        for (String className : batchClassBytes.keySet()) {
            try {
                if (!compiledClasses.containsKey(className))
                    compiledClasses.put(className, classLoader.load(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return compiledClasses;
    }

    public Map<String, Class> compileBatchToSystem(){
        return compileBatchToSystem(MemoryClassLoader.CURRENT_LOADER_NAME);
    }

    public Map<String, Class> compileBatchToSystem(String compilerName) {
        Map<String, byte[]> batchClassBytes = compile(compUnits, fileManager, new PrintWriter(System.err), null, null);
//        ClassLoader currentThreadClassLoader
//                = Thread.currentThread().getContextClassLoader();
//        MemoryClassLoader classLoader = new MemoryClassLoader(batchClassBytes, outputDir.getPath(), currentThreadClassLoader);
//        Thread.currentThread().setContextClassLoader(classLoader);

        ClassLoader systemLoader
                = ClassLoader.getSystemClassLoader();

        MemoryClassLoader classLoader = MemoryClassLoader.getInstance(compilerName, batchClassBytes, outputDir.getPath(), systemLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
        for (String className : batchClassBytes.keySet()) {
            try {
                if (!compiledClasses.containsKey(className)) {
                    compiledClasses.put(className, classLoader.load(className));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return compiledClasses;
    }

    public void addClassPath() {
        ClassLoader systemLoader
                = Thread.currentThread().getContextClassLoader();
        MemoryClassLoader classLoader = new MemoryClassLoader(new HashMap<String, byte[]>(), outputDir.getPath(), systemLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * compile given String source and return bytecodes as a Map.
     *
     * @param fileName   source fileName to be used for error messages etc.
     * @param source     Java source as String
     * @param err        error writer where diagnostic messages are written
     * @param sourcePath location of additional .java source files
     * @param classPath  location of additional .class files
     */
    private Map<String, byte[]> compile(String fileName, String source,
                                        Writer err, String sourcePath, String classPath) {
        // to collect errors, warnings etc.
        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();


        // prepare the compilation unit
        List<JavaFileObject> compUnits = new ArrayList<JavaFileObject>(1);
        compUnits.add(MemoryJavaFileManager.makeStringSource(fileName, source));

        return compile(compUnits, fileManager, err, sourcePath, classPath);
    }

    private Map<String, byte[]> compile(final List<JavaFileObject> compUnits,
                                        final MemoryJavaFileManager fileManager,
                                        Writer err, String sourcePath, String classPath) {
        // to collect errors, warnings etc.
        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();

        // javac options
        List<String> options = new ArrayList<String>();
        options.add("-Xlint:all");
        //       options.add("-g:none");
        options.add("-deprecation");
        if (sourcePath != null) {
            options.add("-sourcepath");
            options.add(sourcePath);
        }

        if (classPath != null) {
            options.add("-classpath");
            options.add(classPath);
        }

        // create a compilation task
        JavaCompiler.CompilationTask task =
                tool.getTask(err, fileManager, diagnostics,
                        options, null, compUnits);

        if (!task.call()) {
            PrintWriter perr = new PrintWriter(err);
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                perr.println(diagnostic);
            }
            perr.flush();
            return null;
        }

        Map<String, byte[]> classBytes = fileManager.getClassBytes();
        try {
            fileManager.close();
        } catch (IOException exp) {
        }

        return classBytes;
    }
}
