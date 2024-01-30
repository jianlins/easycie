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

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.util.Map;

import static edu.utah.bmi.nlp.compiler.MemoryJavaFileManager.toURI;

/**
 * Created by u0876964 on 11/3/16.
 */
public class ClassOutputBuffer extends SimpleJavaFileObject {
    private final String name;
    private final Map<String, byte[]> classBytes;
    private final File rootDir;

    ClassOutputBuffer(String name, Map<String, byte[]> classBytes, File rootDir) {
        super(toURI(name), Kind.CLASS);
        this.name = name;
        this.classBytes = classBytes;
        this.rootDir = rootDir;
    }

    public OutputStream openOutputStream() {
        return new FilterOutputStream(new ByteArrayOutputStream()) {
            public void close() throws IOException {
                out.close();
                ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                classBytes.put(name, bos.toByteArray());
                if (rootDir != null) {
                    File outputFile = new File(rootDir, name.replace(".", "/") + ".class");
                    if (!outputFile.getParentFile().exists())
                        FileUtils.forceMkdir(outputFile.getParentFile());
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    bos.writeTo(outputStream);
                    outputStream.close();
                }
            }
        };
    }

    public Map<String, byte[]> getClassBytes() {
        return classBytes;
    }

}
