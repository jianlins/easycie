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

package edu.utah.bmi.nlp.uima;

import edu.utah.bmi.nlp.uima.jcas.JcasGen;

import java.util.HashSet;

/**
 * This java class is to use UIMA tools (CPE, CPE-GUI, or DocumentAnalyzer) conveniently.
 * - Use CpmFrame to modify the CPE descriptor.xml (In the menu, open file, locate your local FastContext_General_CPEdesc.xml, and load it)
 * - Use SimpleRunCPE to run CPE more quickly without any configuration
 * - Use DocumentAnalyzer to launch UIMA DocumentAnalyzer.
 * Created by Jianlin Shi on 5/30/2015.
 */
public class GenUIMAClass {
    public static void main(String[] args) throws Exception{
        new JcasGen().main("desc/type/All_Types.xml", new HashSet<>(), "src/main/java/");  }
}
