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
package edu.utah.bmi.nlp.core;

import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * This Determinants is going to be initiated dynamically in edu.utah.FastRule.java
 *
 * @author Jianlin Shi
 */

public class DeterminantValueSet {
    public enum Determinants {
        END, ACTUAL, PSEUDO
    }

    public static String uimaTypeShortDomain = "uima.tcas.";
    public static String uimaTypeFullDomain = "org.apache.uima.jcas.tcas.";

    public static String defaultNameSpace = "edu.utah.bmi.nlp.type.system.";
    public static String defaultSuperTypeName = defaultNameSpace + "Concept";


    public static String checkNameSpace(String typeName) {
        if (typeName.equals("DocumentAnnotation")) {
            typeName = DocumentAnnotation.class.getCanonicalName();
        } else if (typeName.equals("SourceDocumentInformation")) {
            typeName = SourceDocumentInformation.class.getCanonicalName();
        } else if (typeName.equals("Annotation")) {
            typeName = uimaTypeShortDomain+"Annotation";
        } else if (typeName.indexOf(".") == -1) {
            typeName = defaultNameSpace + typeName;
        }
//        else if (typeName.startsWith(uimaTypeShortDomain)) {
//            typeName = uimaTypeFullDomain + typeName.substring(10);
//        }
        return typeName;
    }

    public static String getRealClassTypeName(String typeName) {
        if (typeName.startsWith(uimaTypeShortDomain)) {
            typeName = uimaTypeFullDomain + typeName.substring(10);
        }
        return typeName;
    }

    public static String getShortName(String typeName) {
        int dot = typeName.lastIndexOf(".");
        if (dot > -1) {
            typeName = typeName.substring(dot + 1);
        }
        return typeName;
    }


    public static final String PARAM_RULE_STR = "RuleFileOrStr";
    public static final String PARAM_CASE_SENSITIVE = "CaseSensitive";
    public static final String PARAM_VERSION = "Version";
    public static final String PARAM_DB_CONFIG_FILE = "DBConfigFile";
    public static final String PARAM_ANNOTATOR = "Annotator";


    public static final String FEATURE_VALUES1 = "@FEATURE_VALUES";
    public static final String FEATURE_VALUES2 = "&FEATURE_VALUES";
    public static final String CONCEPT_FEATURES1 = "@CONCEPT_FEATURES";
    public static final String CONCEPT_FEATURES2 = "&CONCEPT_FEATURES";

    public static final String DEFAULT_DOC_TYPE1="@DEFAULT_DOC_TYPE";
    public static final String DEFAULT_DOC_TYPE2="&DEFAULT_DOC_TYPE";

    public static final String DEFAULT_BUNCH_TYPE1="@DEFAULT_BUNCH_TYPE";
    public static final String DEFAULT_BUNCH_TYPE2="&DEFAULT_BUNCH_TYPE";

    public static final String RELATION_DEFINITION1="@RELATION_DEFINITION";
    public static final String RELATION_DEFINITION2="&RELATION_DEFINITION";

    public static final String TEMPORAL_CATEGORIES1="@CATEGORY_VALUES";
    public static final String TEMPORAL_CATEGORIES2="&CATEGORY_VALUES";

    public static String randomString(int length){
        byte[] array = new byte[length]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
    public static String randomString(){
        return randomString(7);
    }
}
