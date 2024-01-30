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

package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.AnnotationDefinition;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * @author Jianlin Shi on 6/9/17.
 */
public class DocInferenceFeatureReader {
    public static Logger logger = IOUtil.getLogger(DocInferenceAnnotator.class);
    private final LinkedHashMap<String, String> featureEvidenceConceptPair = new LinkedHashMap<>();

    public DocInferenceFeatureReader(String featureReadRule,
                                     HashMap<String, Class<? extends Annotation>> conceptClassMap,
                                     HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures, ArrayList<Class> evidences) {
        init(featureReadRule, conceptClassMap, evidenceConceptGetFeatures, evidences);
    }

    private void init(String featureReadRule,
                      HashMap<String, Class<? extends Annotation>> conceptClassMap,
                      HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures, ArrayList<Class> evidences) {
        featureReadRule = featureReadRule.trim();
        featureEvidenceConceptPair.clear();
        if (featureReadRule.length() > 0)
            for (String featureValuePair : featureReadRule.split(",")) {
                featureValuePair = featureValuePair.trim();
                String[] featureValueArray = featureValuePair.split(":");
                if (featureValueArray.length == 1) {
//					if only feature name is mentioned without a value source, infer the source from evidences by
//					looking for available features
                    addShortFormPair(featureValueArray[0], featureEvidenceConceptPair, evidenceConceptGetFeatures, evidences);
                } else {
                    String featureName = featureValueArray[0];
                    String value = featureValueArray[1];
                    String evidenceTypeShortName = DeterminantValueSet.getShortName(value);
                    featureEvidenceConceptPair.put(featureName, value);
                    if (evidenceConceptGetFeatures.containsKey(value)) {
                        if (!evidenceConceptGetFeatures.containsKey(evidenceTypeShortName)) {
                            evidenceConceptGetFeatures.put(evidenceTypeShortName, new HashMap<>());
                        }
                        if (!evidenceConceptGetFeatures.get(evidenceTypeShortName).containsKey(featureName)) {
                            evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName,
                                    getFeature(conceptClassMap.get(evidenceTypeShortName), AnnotationOper.inferGetMethodName(featureName)));

                        }
                    } else {
                        if (conceptClassMap.containsKey(value)) {
                            if (!evidenceConceptGetFeatures.containsKey(value))
                                evidenceConceptGetFeatures.put(value, new HashMap<>());
                            evidenceConceptGetFeatures.get(value).put(featureName, AnnotationOper.getDefaultGetMethod(conceptClassMap.get(value), featureName));
                        } else if (value.charAt(0) == '$' && conceptClassMap.containsKey(value.substring(1))) {
                            value = value.substring(1);
                            if (!evidenceConceptGetFeatures.containsKey(value))
                                evidenceConceptGetFeatures.put(value, new HashMap<>());
                            evidenceConceptGetFeatures.get(value).put(featureName, AnnotationOper.getDefaultGetMethod(conceptClassMap.get(value), featureName));
                        }
                    }
                }
            }
    }

    private void addShortFormPair(String featureName, LinkedHashMap<String, String> featureEvidenceConceptPair,
                                  HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures,
                                  ArrayList<Class> evidences) {
        int uniqueMatch = -1;
        Class evidence = null;
        Method feature = null;
        Method currentfeature = null;
        StringBuilder listAllEvidenceClass = new StringBuilder();
        StringBuilder listMatchedEvidenceClass = new StringBuilder();
        for (Class evidenceClass : evidences) {
            String evidenceShortName = evidenceClass.getSimpleName();
            listAllEvidenceClass.append(",");
            listAllEvidenceClass.append(evidenceShortName);
            if (evidenceConceptGetFeatures.containsKey(evidenceShortName)) {
                if (evidenceConceptGetFeatures.get(evidenceShortName).containsKey(featureName)) {
                    if (uniqueMatch == -1) {
                        uniqueMatch = 1;
                        evidence = evidenceClass;
                        listMatchedEvidenceClass.append(",");
                        listMatchedEvidenceClass.append(evidenceShortName);
                    } else if (uniqueMatch == 1)
                        uniqueMatch = 0;
                } else {
                    currentfeature = getFeature(evidenceClass, featureName);
                    if (currentfeature != null) {
                        if (uniqueMatch == -1) {
                            uniqueMatch = 1;
                            evidence = evidenceClass;
                            feature = currentfeature;
                        } else if (uniqueMatch == 1)
                            uniqueMatch = 0;
                        listMatchedEvidenceClass.append(",");
                        listMatchedEvidenceClass.append(evidenceShortName);
                    }
                }
            } else {
                currentfeature = getFeature(evidenceClass, featureName);
                if (currentfeature != null) {
                    if (uniqueMatch == -1) {
                        uniqueMatch = 1;
                        evidence = evidenceClass;
                        feature = currentfeature;
                    } else if (uniqueMatch == 1)
                        uniqueMatch = 0;
                    listMatchedEvidenceClass.append(",");
                    listMatchedEvidenceClass.append(evidenceShortName);
                }
            }
        }
        if (uniqueMatch == -1) {
            logger.warning("DocInference Rule error: " + " Feature '" + featureName + "' doesn't exist in Annotation Types: " + listAllEvidenceClass.substring(1));
//            System.exit(0)
        } else {
            if (uniqueMatch == 0) {
                logger.info("Notice: " + " Feature '" + featureName + "' exists in multiple Annotation Types: " + listMatchedEvidenceClass.substring(1));
                logger.info("\tYou need specify which evidence class's feature value need to be used for the feature: " + featureName);
                logger.info("\tAs the default, only the 1st evidence annotation: '" + listMatchedEvidenceClass.substring(1).split(",")[0] + "' is used to assign the feature value");
            } else {
                logger.info("Short form feature value assignment rule found: assign feature '" + featureName + "' a value from evidence annotation: '" + listAllEvidenceClass.substring(1) + "'");
            }
            String evidenceTypeShortName = evidence.getSimpleName();
            if (!evidenceConceptGetFeatures.containsKey(evidenceTypeShortName)) {
                evidenceConceptGetFeatures.put(evidenceTypeShortName, new HashMap<>());
            }
            if (!evidenceConceptGetFeatures.get(evidenceTypeShortName).containsKey(featureName)) {
                evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName, feature);
            }
            featureEvidenceConceptPair.put(featureName, evidenceTypeShortName);
        }
    }


    private Method getFeature(Class annotationClass, String featureName) {
        Method getFeatureValueMethod = null;
        try {
            getFeatureValueMethod = annotationClass.getMethod(AnnotationOper.inferGetMethodName(featureName));
        } catch (NoSuchMethodException e) {
        }
        return getFeatureValueMethod;
    }

    public AnnotationDefinition getAnnotationDef(TypeDefinition conclusionType, ArrayList<Annotation> evidenceAnnotations,
                                                 HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures) {
        AnnotationDefinition conclusionDef = new AnnotationDefinition(conclusionType);
        HashMap<String, Annotation> evidenceAnnotationMap = new HashMap<>();
        for (Annotation annotation : evidenceAnnotations) {
            evidenceAnnotationMap.put(annotation.getType().getShortName(), annotation);
        }
        for (String featureName : featureEvidenceConceptPair.keySet()) {
            Object valueVariable = featureEvidenceConceptPair.get(featureName);
            if (valueVariable instanceof String && ((String)valueVariable).charAt(0)=='$')
                valueVariable=((String)valueVariable).substring(1);
            if (evidenceConceptGetFeatures.containsKey(valueVariable) ||
                    (valueVariable == null && evidenceAnnotations.size() == 1)
                            && evidenceConceptGetFeatures.containsKey(evidenceAnnotations.get(0).getClass().getSimpleName())
                            && evidenceConceptGetFeatures.get(evidenceAnnotations.get(0).getClass().getSimpleName()).containsKey(featureName)) {
                String evidenceConceptName = DeterminantValueSet.getShortName((String) valueVariable);
                if (!evidenceAnnotationMap.containsKey(evidenceConceptName))
                    continue;
                try {
                    valueVariable= FSUtil.getFeature(evidenceAnnotationMap.get(evidenceConceptName), featureName, Object.class);
//                    valueVariable = evidenceConceptGetFeatures.get(evidenceConceptName)
//                            .get(featureName).invoke(evidenceAnnotationMap.get(evidenceConceptName));
                } catch (Exception e) {
                    valueVariable=null;
                    e.printStackTrace();
                }
            }
            conclusionDef.setFeatureValue(featureName, valueVariable);
        }
        return conclusionDef;
    }

    public String getFeaturesString(ArrayList<Annotation> evidenceAnnotations,
                                    HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures) {
        if (featureEvidenceConceptPair.size() == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        String output = "";
        HashMap<String, Annotation> evidenceAnnotationMap = new HashMap<>();
        for (Annotation annotation : evidenceAnnotations) {
            evidenceAnnotationMap.put(annotation.getType().getShortName(), annotation);
        }
        for (String featureName : featureEvidenceConceptPair.keySet()) {
            String valueVariable = featureEvidenceConceptPair.get(featureName);
            sb.append("\t\t");
            sb.append(featureName);
            sb.append(":\t");
//			if the value variable points to a Concept name, then read the value of the same feature of the evidence concept.
            if (evidenceConceptGetFeatures.containsKey(valueVariable) ||
                    (valueVariable == null && evidenceAnnotations.size() == 1)
                            && evidenceConceptGetFeatures.containsKey(evidenceAnnotations.get(0).getClass().getSimpleName())
                            && evidenceConceptGetFeatures.get(evidenceAnnotations.get(0).getClass().getSimpleName()).containsKey(featureName)) {
                String evidenceConceptName = DeterminantValueSet.getShortName(valueVariable);
                if (!evidenceAnnotationMap.containsKey(evidenceConceptName))
                    continue;
                try {
                    String value = evidenceConceptGetFeatures.get(evidenceConceptName)
                            .get(featureName).invoke(evidenceAnnotationMap.get(evidenceConceptName)) + "";
                    sb.append(value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else if (valueVariable.charAt(0) == '$') {
                String evidenceClass = valueVariable.substring(1);
                if (evidenceAnnotationMap.containsKey(evidenceClass) && evidenceConceptGetFeatures.containsKey(evidenceClass)
                        && evidenceConceptGetFeatures.get(evidenceClass).containsKey(featureName)) {
                    Object value = AnnotationOper.getFeatureValue(featureName, evidenceAnnotationMap.get(evidenceClass));
                    if (value != null)
                        sb.append(value);
                } else {
                    logger.info(evidenceClass + " cannot be found from the evidences defined in the rule for classes:  " + evidenceAnnotationMap.keySet() + ". Or feature: " + featureName + " cannot be found in Class: " + evidenceClass);
                }
            } else {
                sb.append(valueVariable);
            }
            sb.append("\n");
        }
        if (sb.length() > 0) {
            output = sb.substring(0, sb.length() - 1);
        }
        return output;
    }
}
