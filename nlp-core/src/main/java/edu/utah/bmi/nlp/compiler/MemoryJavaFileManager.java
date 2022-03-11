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

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * JavaFileManager that keeps compiled .class bytes in memory.
 */
@SuppressWarnings("unchecked")
final class MemoryJavaFileManager extends ForwardingJavaFileManager {

    /**
     * Java source file extension.
     */
    private final static String EXT = ".java";
    private static File rootDir = null;

    private Map<String, byte[]> classBytes;

    public MemoryJavaFileManager(JavaFileManager fileManager, Object... paras) {
        super(fileManager);
        classBytes = new HashMap<>();
        if (paras.length > 0) {
            if (paras[0] instanceof File)
                rootDir = (File) paras[0];
            else
                throw new RuntimeException("The 1st parameter should be a File class");
        }
        if (paras.length > 1) {
            if (paras[1] instanceof Map)
                classBytes = (Map<String, byte[]>) paras[1];
            else
                throw new RuntimeException("The 2nd parameter should be a Map<String, byte[]>");
        }
    }

    public Map<String, byte[]> getClassBytes() {
        return classBytes;
    }

    public void close() throws IOException {
        classBytes = null;
    }

    public void flush() throws IOException {
    }

    public JavaFileObject getJavaFileForOutput(Location location,
                                               String className,
                                               Kind kind,
                                               FileObject sibling) throws IOException {
        if (kind == Kind.CLASS) {
            ClassOutputBuffer classOutputBuffer = new ClassOutputBuffer(className, classBytes, rootDir);
            return classOutputBuffer;
        } else {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
    }

    static JavaFileObject makeStringSource(String fileName, String code) {
        return new StringInputBuffer(fileName, code);
    }

    static URI toURI(String name) {
        File file = new File(name);
        if (file.exists()) {
            return file.toURI();
        } else {
            try {
                final StringBuilder newUri = new StringBuilder();
                newUri.append("mfm:///");
                newUri.append(name.replace('.', '/'));
                if (name.endsWith(EXT)) newUri.replace(newUri.length() - EXT.length(), newUri.length(), EXT);
                return URI.create(newUri.toString());
            } catch (Exception exp) {
                return URI.create("mfm:///com/sun/script/java/java_source");
            }
        }
    }
}
