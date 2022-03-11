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

package edu.utah.bmi.nlp.uima.jcas;

public class TypeInfo {

    String xmlName;

    String javaNameWithPkg;

    String javaName; // name without package prefix if in this package

    boolean isArray = false;

    String arrayElNameWithPkg;

    boolean used = false;

    TypeInfo(String xmlName, String javaName) {
        this.xmlName = xmlName;
        this.javaNameWithPkg = javaName;
        this.javaName = org.apache.uima.tools.jcasgen.Jg.removePkg(javaName);
        this.isArray = false;
        this.arrayElNameWithPkg = "";
    }

    TypeInfo(String xmlName, String javaName, String arrayElNameWithPkg) {
        this(xmlName, javaName);
        if (null != arrayElNameWithPkg) {
            this.isArray = true;
            this.arrayElNameWithPkg = arrayElNameWithPkg;
        }
    }
}
