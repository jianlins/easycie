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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassLoader that loads .class bytes from memory.
 */
public class MemoryClassLoader extends URLClassLoader {
    public static String CURRENT_LOADER_NAME = "uima";
    public static final String DEFAULT_LOADER_NAME = "uima";
    private Map<String, byte[]> classBytes;
    private static final ConcurrentHashMap<String, MemoryClassLoader> mcLoaders = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Class> classCache=new ConcurrentHashMap<>();


    public static MemoryClassLoader getInstance(String name) {
        if (mcLoaders.containsKey(name)) {
//            System.out.println("read MemoryClassLoader");
            return mcLoaders.get(name);
        } else {
            return null;
        }
    }

    public static MemoryClassLoader getInstance(String name, Map<String, byte[]> classBytes,
                                                String classPath, ClassLoader parent) {
        if (!mcLoaders.containsKey(name)) {
            mcLoaders.put(name, new MemoryClassLoader(classBytes, classPath, parent));
        }
        return mcLoaders.get(name);
    }

    public static MemoryClassLoader getInstance(String name, Map<String, byte[]> classBytes,
                                                String classPath) {
        return getInstance(name, classBytes, classPath, ClassLoader.getSystemClassLoader());
    }

    public static MemoryClassLoader getInstance(String name, Map<String, byte[]> classBytes) {
        return getInstance(name, classBytes, null, ClassLoader.getSystemClassLoader());
    }

    public MemoryClassLoader(Map<String, byte[]> classBytes,
                             String classPath, ClassLoader parent) {
        super(toURLs(classPath, parent), parent);
        this.classBytes = classBytes;
    }

    public MemoryClassLoader(Map<String, byte[]> classBytes, String classPath) {
        this(classBytes, classPath, ClassLoader.getSystemClassLoader());
    }

    public MemoryClassLoader(Map<String, byte[]> classBytes) {
        this(classBytes, null, ClassLoader.getSystemClassLoader());
    }

    public MemoryClassLoader(Map<String, byte[]> classBytes, ClassLoader parent) {
        this(classBytes, null, parent);
    }

    public Class load(String className) throws ClassNotFoundException {
            if (!classCache.containsKey(className))
                classCache.put(className, loadClass(className));
            return classCache.get(className);
    }

    public Iterable<Class> loadAll() throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>(classBytes.size());
        for (String name : classBytes.keySet()) {
            classes.add(loadClass(name));
        }
        return classes;
    }

    protected Class findClass(String className) throws ClassNotFoundException {
        byte[] buf = classBytes.get(className);
        if (buf != null) {
            // clear the bytes in map -- we don't need it anymore
            classBytes.put(className, null);
            return defineClass(className, buf, 0, buf.length);
        } else {
            return super.findClass(className);
        }
    }

    private static URL[] toURLs(String classPath, ClassLoader parent) {
        if (classPath == null) {
            return new URL[0];
        }

//        List<URL> list = new ArrayList<>();
//        if (parent instanceof URLClassLoader) {
//            list.addAll(Arrays.asList(((URLClassLoader) parent).getURLs()));
//        }
//        StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
//        while (st.hasMoreTokens()) {
//            String token = st.nextToken();
//            File file = new File(token);
//            if (file.exists()) {
//                try {
//                    list.add(file.toURI().toURL());
//                } catch (MalformedURLException mue) {
//                }
//            } else {
//                try {
//                    list.add(new URL(token));
//                } catch (MalformedURLException mue) {
//                }
//            }
//        }
//        URL[] res = new URL[list.size()];
//        list.toArray(res);

        URL url = null;
        URL[] res = null;
        try {
            url = new File(classPath).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        res = new URL[]{url};
        return res;
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public static void resetLoaderName(){
        MemoryClassLoader.CURRENT_LOADER_NAME =MemoryClassLoader.DEFAULT_LOADER_NAME;
    }
}
