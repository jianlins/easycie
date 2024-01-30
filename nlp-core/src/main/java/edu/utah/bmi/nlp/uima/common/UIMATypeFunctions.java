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

package edu.utah.bmi.nlp.uima.common;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.*;

/**
 * Created by Jianlin Shi on 9/27/16.
 */
public class UIMATypeFunctions {

    private static final HashSet<String> baseMethods = new HashSet<>(Arrays.asList("TypeIndexID", "Sofa", "CASImpl", "LowLevelCas", "CAS", "Address", "Type", "Class",
            "View", "avoidcollisionTypeCode", "Begin", "Start", "End", "CoveredText", "Text"));
    public static Logger logger = IOUtil.getLogger(UIMATypeFunctions.class);


    @Deprecated
    public static boolean classLoaded(String className) {
        return AnnotationOper.classLoaded(className);
    }


    public static HashMap<String, String> getMethodUIMATypesFromLoadedClass(String evidenceTypeName) {
        HashMap<String, String> methodTypes = getMethodTypesFromLoadedClass(evidenceTypeName);
        for (String methodName : methodTypes.keySet()) {
            String type = methodTypes.get(methodName);
            if (type.startsWith("java.lang."))
                type = "uima.cas." + type.substring(10);
            methodTypes.put(methodName, type);
        }
        return methodTypes;
    }

    public static HashMap<String, String> getMethodTypesFromLoadedClass(String evidenceTypeName) {
        HashMap<String, String> methodTypes = new LinkedHashMap<>();
        Class<? extends AnnotationBase> evidenceTypeClass = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(evidenceTypeName));
        if (evidenceTypeClass != null) {
            for (Method method : evidenceTypeClass.getMethods()) {
//                  only take getXXX() methods, no parameters.
                if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                    String type = method.getReturnType().getCanonicalName();
                    String featureName = method.getName().substring(3);
                    if (baseMethods.contains(featureName))
                        continue;
                    if (type.indexOf(".") == -1) {
                        type = "uima.cas." + type;
                    } else {
                        type = type.replaceAll("org.apache.uima.jcas.cas", "uima.cas");
                    }
                    methodTypes.put(featureName, type);
                }
            }
        } else {
            logger.fine("Class: " + DeterminantValueSet.checkNameSpace(evidenceTypeName) + " has not been loaded yet. Skip initiate getMethods.");
        }
        return methodTypes;
    }

    /**
     * Parse rule files, initiate type definitions, feature value pairs, and value weights (priorites based on orders)
     *
     * @param ruleStr         input rule string or rule file location
     * @param ruleCells       elements of rules, ordered in rows and columns
     * @param valueFeatureMap values and corresponding features (need to make sure there is no value that is shared in different features)
     * @param valueWeightMap  values and corresponding weights
     * @param defaultDocMap   default document conclusion types, one conclusion per question.
     * @param typeDefinitions type short names and the corresponding definition object.
     */
    public static void getTypeDefinitions(String ruleStr,
                                          ArrayList<ArrayList<String>> ruleCells,
                                          HashMap<String, String> valueFeatureMap,
                                          HashMap<String, Integer> valueWeightMap,
                                          HashMap<String, String> defaultDocMap,
                                          LinkedHashMap<String, TypeDefinition> typeDefinitions) {
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        getTypeDefinitions(ioUtil, ruleCells,
                valueFeatureMap, valueWeightMap, defaultDocMap, typeDefinitions);
    }

    /**
     * Parse rule files, initiate type definitions, feature value pairs, and value weights (priorites based on orders)
     *
     * @param ioUtil          Initiated IOUtil Object
     * @param ruleCells       elements of rules, ordered in rows and columns
     * @param valueFeatureMap values and corresponding features (need to make sure there is no value that is shared in different features)
     * @param valueWeightMap  values and corresponding weights
     * @param defaultTypeMap   default document conclusion types, one conclusion per question.
     * @param typeDefinitions type short names and the corresponding definition object.
     */
    public static void getTypeDefinitions(IOUtil ioUtil,
                                          ArrayList<ArrayList<String>> ruleCells,
                                          HashMap<String, String> valueFeatureMap,
                                          HashMap<String, Integer> valueWeightMap,
                                          HashMap<String, String> defaultTypeMap,
                                          LinkedHashMap<String, TypeDefinition> typeDefinitions) {
        ruleCells.addAll(ioUtil.getRuleCells());
        HashMap<String, String> featureNameTypes = new HashMap<>();
        LinkedHashMap<String, String> featureDefaultValueMap = new LinkedHashMap<>();
        ArrayList<String> newTypes = new ArrayList<>();
        for (ArrayList<String> initRow : ioUtil.getInitiations()) {
            String header = initRow.get(1).trim();
            switch (header) {
                case FEATURE_VALUES1:
                case FEATURE_VALUES2:
//          @FEATURE_VALUES	Temporality	historical	present
//          @FEATURE_VALUES	Temporality:String	historical	present
//          Mapping the values to feature names, so that later rules can reference the values directly without telling the feature name.
//          In this case, the values have to be unique.
                    String featureName = initRow.get(2);
                    if (featureName.indexOf(":") != -1) {
                        int separator = featureName.indexOf(":");
                        featureNameTypes.put(featureName.substring(0, separator).trim(), featureName.substring(separator + 1));
                    }
                    for (int i = 3; i < initRow.size(); i++) {
                        String value = initRow.get(i).trim();
                        if (value.length() == 0)
                            continue;
                        if (valueFeatureMap.containsKey(value)) {
                            logger.info("You have more than one features have the value: " + value +
                                    ". You won't be able to use the value alone to define the conditions." +
                                    "Instead, you will need to use 'FeatureName:value' format.");
                        } else {
                            valueFeatureMap.put(value, featureName);
                            valueWeightMap.put(value, i - 3);
                            if (i == 3)
                                featureDefaultValueMap.put(featureName, value);
                        }
                    }
                    break;
                case CONCEPT_FEATURES1:
                case CONCEPT_FEATURES2:
//                  support new type definition format.
//             if default value is omitted: if the feature is definted in the rule file, the default value will be set to the most left value,
//             otherwise, it assumes that the feature will read values in running time from evidence classes.
//        @CONCEPT_FEATURES Negated_PE	Concept	Section:DocumentAnnotation	Certainty:certain	ImpressionCertainty:imcertain	Temporality:present	ImpressionCertainty:imuncertain	DiagnosticMention:yes
//        &CONCEPT_FEATURES Negated_PE	Concept	Section:DocumentAnnotation	Certainty:certain	ImpressionCertainty:imcertain	Temporality:present	ImpressionCertainty:imuncertain	DiagnosticMention:yes
                    String conclusionTypeName = initRow.get(2).trim();
                    String superTypeName = initRow.get(3);
                    TypeDefinition typeDefinition = new TypeDefinition(conclusionTypeName, superTypeName, new ArrayList<>());
                    if (initRow.size() > 4) {
                        for (int i = 4; i < initRow.size(); i++) {
                            String[] featureValuePair = initRow.get(i).split(":");
                            if (featureValuePair.length == 1 || featureValuePair[1].trim().length() == 0) {
//                              keep default value as null for later direct copy
                                typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), null);
                            } else {
                                typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), featureValuePair[1].trim());
                            }
                        }
                    }//					enhance with feature Type

                    typeDefinitions.put(typeDefinition.shortTypeName, typeDefinition);
                    newTypes.add(typeDefinition.shortTypeName);
                    break;
                case DEFAULT_DOC_TYPE1:
                case DEFAULT_DOC_TYPE2:
                case DEFAULT_BUNCH_TYPE1:
                case DEFAULT_BUNCH_TYPE2:
                    defaultTypeMap.put(initRow.get(2).trim(), initRow.get(3).trim());
                    break;
                case RELATION_DEFINITION1:
                case RELATION_DEFINITION2:
                    break;
                default:
                    // support legacy format, directly start with conclusion annotation type name,
                    //        @Negated_PE	Concept	Section:DocumentAnnotation	Certainty:certain	ImpressionCertainty:imcertain	Temporality:present	ImpressionCertainty:imuncertain	DiagnosticMention:yes
                    conclusionTypeName = initRow.get(1).substring(1);
                    if (conclusionTypeName.toLowerCase().endsWith("_doc")) {
                        defaultTypeMap.put(conclusionTypeName, initRow.get(2).trim());
                        continue;
                    }
                    superTypeName = initRow.get(2);
                    typeDefinition = new TypeDefinition(conclusionTypeName, superTypeName, new ArrayList<>());
                    if (initRow.size() > 3) {
                        for (int i = 3; i < initRow.size(); i++) {
                            String[] featureValuePair = initRow.get(i).split(":");
                            if (featureValuePair[0].startsWith("COPYALL")) {
                                logger.info("You need to define the Concept '" + conclusionTypeName + "' first before using 'COPYALL' or 'COPYALLEXCEPT' syntax.");
                                logger.info("For instance: @Negated_PE\tConcept\tSection:DocumentAnnotation\n" +
                                        "This rule define a 'Negated_PE' concept derived from 'Concept' type, and has one feature 'Section', with default value 'DocumentAnnotation'");
                            }
                            if (featureValuePair.length == 1 || featureValuePair[1].trim().length() == 0) {
                                typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), null);
                            } else {
                                typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), featureValuePair[1].trim());
                            }
                        }
                    }


                    typeDefinitions.put(typeDefinition.shortTypeName, typeDefinition);
                    break;
            }

        }
        //	enhance with feature Type, add default value is not set in CONCEPT_FEATURES setting
        for (String typeName : newTypes) {
            TypeDefinition typeDefinition = typeDefinitions.get(typeName);
            for (String featureName : typeDefinition.getFeatureValuePairs().keySet()) {
                if (featureNameTypes.containsKey(featureName)) {
                    typeDefinition.setFeatureType(featureName, featureNameTypes.get(featureName));
                }
                if (featureDefaultValueMap.containsKey(featureName) && !typeDefinition.getFeatureValuePairs().containsKey(featureName))
                    typeDefinition.addFeatureDefaultValue(featureName, featureDefaultValueMap.get(featureName));
            }
        }
    }

    public HashMap<String, String> getMethodTypes(String evidenceTypeName, LinkedHashMap<String, TypeDefinition> typeDefinitions) {
        HashMap<String, String> methodTypes;
        if (typeDefinitions.containsKey(evidenceTypeName)) {
            return typeDefinitions.get(evidenceTypeName).featureTypes;
        } else {
            methodTypes = UIMATypeFunctions.getMethodTypesFromLoadedClass(evidenceTypeName);
        }
        return methodTypes;
    }

}
