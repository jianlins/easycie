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

import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.uima.common.AnnotationOper.initGetReflections;
import static edu.utah.bmi.nlp.uima.common.AnnotationOper.initSetReflections;
import static java.lang.Character.isUpperCase;

/**
 * Given a definition of an annotation using Type name, and feature value pairs, find the matched annotations,
 * and map to the corresponding inference rule
 *
 * @author Jianlin Shi on 5/9/17.
 */
public class FeatureAnnotationInferencer implements FeatureInferencerInf {
    public static Logger logger = IOUtil.getLogger(FeatureInferenceAnnotator.class);
    private LinkedHashMap<Class<? extends Annotation>, Object> ruleMap = new LinkedHashMap<>();
    private HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures = new HashMap<>();
    private HashMap<Class, HashMap<String, Method>> conclusionConceptSetFeatures = new HashMap<>();
    private HashMap<String, Class<? extends Annotation>> conceptClassMap = new HashMap<>();
    private HashMap<String, Constructor<? extends Annotation>> conceptTypeConstructors = new HashMap<>();
    private LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();
    private HashMap<String, AnnotationDefinition> conclusionAnnotationDefinitions = new HashMap<>();
    private HashMap<String, String> uniqueFeatureClassMap = new HashMap<>();
    //  save this map to support short form specification of evidence feature values
    private HashMap<String, String> valueFeatureMap = new HashMap<>();
    //	lineNumber -- scope type name
    private HashMap<String, Class<? extends Annotation>> scopeIndex = new HashMap<>();
    private HashMap<Class, IntervalST<Annotation>> scopes = new HashMap<>();
    private boolean removeEvidenceConcept = true;
    private boolean noteRuleId=false;
    private ArrayList<ArrayList<String>> ruleCells = new ArrayList<>();
    @Deprecated
    public boolean debug = false;
    private boolean strictNameMatch = false;
    private static String END = "<END>";


    /**
     * This constructor is only used to read type definitions.
     * It won't save rules and initiate rule Map.
     */
    public FeatureAnnotationInferencer() {

    }

    public FeatureAnnotationInferencer(String ruleStr, boolean removeEvidenceConcept, boolean strictNameMatch,boolean noteRuleId) {
        typeDefinitions = getTypeDefinitions(ruleStr);
        this.removeEvidenceConcept = removeEvidenceConcept;
        this.strictNameMatch = strictNameMatch;
        this.noteRuleId=noteRuleId;
    }

    public FeatureAnnotationInferencer(IOUtil ioUtil, boolean removeEvidenceConcept, boolean strictNameMatch,boolean noteRuleId) {
        typeDefinitions = getTypeDefinitions(ioUtil);
        this.removeEvidenceConcept = removeEvidenceConcept;
        this.strictNameMatch = strictNameMatch;
        this.noteRuleId=noteRuleId;
    }

    public LinkedHashMap<String, TypeDefinition> getTypeDefinitions() {
        return typeDefinitions;
    }


    public LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleStr) {
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        return getTypeDefinitions(ioUtil);
    }


    public LinkedHashMap<String, TypeDefinition> getTypeDefinitions(IOUtil ioUtil) {
        UIMATypeFunctions.getTypeDefinitions(ioUtil, ruleCells,
                valueFeatureMap, new HashMap<>(), new HashMap<>(), typeDefinitions);
//        double check the rule, to see if there is any conclusion type that has not been defined in initiation rows. If so,define them using best guess.
//        Negated_PE	Section,Certainty,ImpressionCertainty,ArteryMain,Left,Right,ArteryInterlobar,ArteryLobar,ArterySegmental,ArterySubSegmental	PE	Negation:negated,DiagnosticMention:yes	DocumentAnnotation
        for (ArrayList<String> row : ruleCells) {
            String conclusionTypeName = row.get(1).trim();
            String evidenceTypeName = row.get(3);
            HashMap<String, String> evidenceTypeMethodTypes = getMethodTypes(evidenceTypeName);
            TypeDefinition typeDefinition;
            if (!typeDefinitions.containsKey(conclusionTypeName)) {
                typeDefinition = new TypeDefinition(conclusionTypeName, Concept.class.getCanonicalName(), new ArrayList<>());
            } else {
                typeDefinition = typeDefinitions.get(conclusionTypeName);
            }
//            in case some feature names are missed in previous concept type definition.
            for (String featureValuePairString : row.get(2).split(",")) {
                String[] featureValuePair = featureValuePairString.split(":");
                String featureName = featureValuePair[0];
                if (featureValuePair[0].startsWith("COPYALL")) {
                    if (!typeDefinitions.containsKey(conclusionTypeName)) {
                        logger.info("You need to define the Concept '" + conclusionTypeName + "' first before using 'COPYALL' or 'COPYALLEXCEPT' syntax.");
                        logger.info("For instance: @CONCEPT_FEATURES\tNegated_PE\tConcept\tSection:DocumentAnnotation\n" +
                                "This rule define a 'Negated_PE' concept derived from 'Concept' type, and has one feature 'Section', with default value 'DocumentAnnotation'");
                    } else {
                        ArrayList<String> newFeatureNames = new ArrayList<>(typeDefinition.getNewFeatureNames());
                        for (String name : newFeatureNames) {
                            if (evidenceTypeMethodTypes.containsKey(name)) {
                                typeDefinition.setFeatureType(name, evidenceTypeMethodTypes.get(name));
                                if (!typeDefinition.getFeatureValuePairs().containsKey(name))
                                    typeDefinition.addFeatureDefaultValue(name, null);
                            }
                        }
                        Class<? extends Annotation> superClass = AnnotationOper.getTypeClass(typeDefinition.fullSuperTypeName);
                        if (superClass != null) {
                            LinkedHashSet<Method> methods = new LinkedHashSet<>();
                            AnnotationOper.getMethods(superClass, methods);
                            for (Method m : methods) {
                                featureName = m.getName().substring(3);
                                if (evidenceTypeMethodTypes.containsKey(featureName)) {
                                    typeDefinition.setFeatureType(featureName, evidenceTypeMethodTypes.get(featureName));
                                    if (!typeDefinition.getFeatureValuePairs().containsKey(featureName))
                                        typeDefinition.addFeatureDefaultValue(featureName, null);
                                }
                            }
                        }

                    }
                } else if (!typeDefinition.featureDefaultValues.containsKey(featureName)) {
                    typeDefinition.addFeatureDefaultValue(featureName, "");
                }
            }
            typeDefinitions.put(typeDefinition.shortTypeName, typeDefinition);
        }
        return typeDefinitions;
    }

    public HashMap<String, String> getMethodTypes(String evidenceTypeName) {
        HashMap<String, String> methodTypes;
        if (typeDefinitions.containsKey(evidenceTypeName)) {
            return typeDefinitions.get(evidenceTypeName).featureTypes;
        } else {
            methodTypes = UIMATypeFunctions.getMethodUIMATypesFromLoadedClass(evidenceTypeName);
        }
        return methodTypes;
    }

    public void init() {
        ruleMap.clear();
        conceptClassMap.clear();
        conceptTypeConstructors.clear();
        evidenceConceptGetFeatures.clear();
        conclusionConceptSetFeatures.clear();
        initSetReflections(typeDefinitions, conceptClassMap, conceptTypeConstructors, conclusionConceptSetFeatures);
        initRuleMap();
    }


    private void initRuleMap() {
        for (ArrayList<String> row : ruleCells) {
            String conclusionTypeName = row.get(1);
//				get conclusion annotation definition
            AnnotationDefinition annotationDefinition = new AnnotationDefinition(typeDefinitions.get(conclusionTypeName));
            ArrayList<String> needEvidenceFeatures = new ArrayList<>();

            String evidenceTypeName = row.get(3);
            Class<? extends Annotation> evidenceTypeClass = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(evidenceTypeName));
            String evidenceTypeShortName = DeterminantValueSet.getShortName(evidenceTypeName);

            augmentRule(row.get(1), row.get(2), evidenceTypeClass, evidenceTypeShortName, annotationDefinition, needEvidenceFeatures);


//				TODO check if "put" is needed
            conceptClassMap.put(evidenceTypeShortName, evidenceTypeClass);
//				if evidence annotation specified certain feature values

            if (row.get(4).trim().length() > 0) {
                if (!ruleMap.containsKey(evidenceTypeClass)) {
                    ruleMap.put(evidenceTypeClass, new LinkedHashMap<>());
                }
                LinkedHashMap<String, Object> tmp = (LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass);
                String[] featureValueMatchRule = augmentConditionRule(row.get(4));
                initGetReflections(evidenceConceptGetFeatures, evidenceTypeShortName, evidenceTypeClass, featureValueMatchRule, needEvidenceFeatures);

                if (featureValueMatchRule.length % 2 == 0) {
//						some feature values need to be met
                    for (int i = 0; i < featureValueMatchRule.length; i++) {
                        String key = featureValueMatchRule[i].trim();
                        if (key.indexOf(":") != -1)
                            key = key.split(":")[0].trim();
                        if (!tmp.containsKey(key)) {
                            tmp.put(key, new LinkedHashMap<String, Object>());
                        }
                        tmp = (LinkedHashMap<String, Object>) tmp.get(key);
                    }
                    if (!tmp.containsKey(END)) {
                        tmp.put(END, new ArrayList());
                    }
                    ((ArrayList<String>) tmp.get(END)).add(row.get(0));
                    conclusionAnnotationDefinitions.put(row.get(0), annotationDefinition);
                } else {
                    logger.info("Rule error: " + row.get(4) + "\n\tin rule: " + row);
                    logger.info("Feature and value need to be paired.");
                }

            } else {
//						not feature value is specified
                if (!ruleMap.containsKey(evidenceTypeClass)) {
                    ruleMap.put(evidenceTypeClass, new LinkedHashMap<>());
                    ((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).put(END, new ArrayList());
                }
                ((ArrayList<String>) ((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).get(END)).add(row.get(0));
                conclusionAnnotationDefinitions.put(row.get(0), annotationDefinition);
                initGetReflections(evidenceConceptGetFeatures, evidenceTypeShortName, evidenceTypeClass, new String[]{}, needEvidenceFeatures);
            }
            if (row.size() > 5 && row.get(5).trim().length() > 0) {
                String scopeShortName = row.get(5).trim();
                if (scopeShortName.equals("DocumentAnnotation") || scopeShortName.equals("SourceDocumentInformation"))
                    continue;
                Class<? extends Annotation> scopeClass = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(scopeShortName));
                if (scopeClass != DocumentAnnotation.class && scopeClass != SourceDocumentInformation.class)
//						no need to index document scope
                    scopeIndex.put(row.get(0), scopeClass);
            }
        }
    }

    private String[] augmentConditionRule(String conditionString) {
        String[] baseRules = conditionString.split(",");
        String[] featureValueMatchRule = new String[baseRules.length * 2];
        for (int i = 0; i < baseRules.length; i++) {
            String ele = baseRules[i];
            String value, featureName = null;
            String[] valueFeatureName = ele.split(":");
            if (valueFeatureName.length == 1) {
                value = valueFeatureName[0];
                if (valueFeatureMap.containsKey(value))
                    featureName = valueFeatureMap.get(value);
                else
                    logger.info("You cannot use short form to define conditions, when the value: '" + value + "' hasn't been paired up with its feature name. \n" +
                            " Set it up using '@FEATURE_VALUES\tfeaturename\tvalue1\tvalue2...'");
            } else {
                featureName = valueFeatureName[0];
                value = valueFeatureName[1];
            }
            featureValueMatchRule[i * 2] = featureName;
            featureValueMatchRule[i * 2 + 1] = value;
        }

        return featureValueMatchRule;
    }

    private void augmentRule(String conclusionType, String featureValues, Class<? extends Annotation> evidenceTypeClass,
                             String evidenceTypeShortName, AnnotationDefinition annotationDefinition,
                             ArrayList<String> needEvidenceFeatures) {
        if (featureValues.trim().length() == 0) {
            return;
        } else {
            String[] featureValuePairs = featureValues.split(",");
            switch (featureValuePairs[0]) {
//                Possible_Encephalopathy	COPYALL	Concept	Negation:affirmed,Certainty:uncertain,Temporality:present	DocumentAnnotation
                case "COPYALL":
                    for (String featureName : this.conclusionConceptSetFeatures.get(conceptClassMap.get(conclusionType)).keySet()) {
                        if (!evidenceConceptGetFeatures.containsKey(evidenceTypeShortName))
                            evidenceConceptGetFeatures.put(evidenceTypeShortName, new HashMap<>());
                        if (evidenceConceptGetFeatures.get(evidenceTypeShortName).containsKey(featureName) &&
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).get(featureName) != null) {
                            annotationDefinition.addFeatureDefaultValue(featureName, evidenceTypeShortName);
                            needEvidenceFeatures.add(featureName);
                        } else {
                            Method m = AnnotationOper.getDefaultGetMethod(evidenceTypeClass, featureName);
                            if (m != null) {
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName, m);
                                annotationDefinition.addFeatureDefaultValue(featureName, evidenceTypeShortName);
                                needEvidenceFeatures.add(featureName);
                            } else {
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName, m);
                            }
                        }
                    }
                    break;
                case "COPYALLEXCEPT":
//                 Possible_Encephalopathy	COPYALLEXCEPT,Certainty:uncertain,Temporality:$Temporality	Concept	Negation:affirmed,Certainty:uncertain,Temporality:present	DocumentAnnotation
                    HashSet<String> skipFeatures = new HashSet<>();
                    for (int i = 1; i < featureValuePairs.length; i++) {
                        String featureValue = featureValuePairs[i];
                        String[] fvpair = featureValue.split(":");
                        skipFeatures.add(fvpair[0].trim());
                        if (fvpair.length > 1) {
                            annotationDefinition.addFeatureDefaultValue(fvpair[0], fvpair[1]);
                            if (isUpperCase(fvpair[1].charAt(0)))
                                needEvidenceFeatures.add(fvpair[1].trim());
                        }
                    }
                    for (String featureName : this.conclusionConceptSetFeatures.get(conceptClassMap.get(conclusionType)).keySet()) {
                        if (skipFeatures.contains(featureName))
                            continue;
                        if (!evidenceConceptGetFeatures.containsKey(evidenceTypeShortName))
                            evidenceConceptGetFeatures.put(evidenceTypeShortName, new HashMap<>());
                        if (evidenceConceptGetFeatures.get(evidenceTypeShortName).containsKey(featureName) &&
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).get(featureName) != null) {
                            annotationDefinition.addFeatureDefaultValue(featureName, evidenceTypeShortName);
                            needEvidenceFeatures.add(featureName);
                        } else {
                            Method m = AnnotationOper.getMethod(evidenceTypeClass, AnnotationOper.inferGetMethodName(featureName));
                            if (m != null) {
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName, m);
                                annotationDefinition.addFeatureDefaultValue(featureName, evidenceTypeShortName);
                                needEvidenceFeatures.add(featureName);
                            } else {
                                evidenceConceptGetFeatures.get(evidenceTypeShortName).put(featureName, m);
                            }
                        }
                    }

                    break;
                default:
//                    Possible_Encephalopathy	Certainty,Temporality:Concept	Concept	Negation:affirmed,Certainty:uncertain,Temporality:present	DocumentAnnotation
                    for (String featureValue : featureValuePairs) {
                        String[] fvpair = augmentRules(featureValue);
                        annotationDefinition.addFeatureDefaultValue(fvpair[0], fvpair[1]);
                        if (fvpair.length == 1 || fvpair[1] == null || isUpperCase(fvpair[1].charAt(0)))
                            needEvidenceFeatures.add(fvpair[0].trim());
                    }
            }
        }


    }

    private String[] augmentRules(String ruleString) {
        String featureName, featureValue;
        if (ruleString.indexOf(":") != -1) {
            String[] pairArray = ruleString.split(":");
            featureName = pairArray[0].trim();
            featureValue = pairArray[1].trim();
        } else {
            featureName = ruleString.trim();
            featureValue = null;
        }
        return new String[]{featureName, featureValue};
    }


    public void processInferences(JCas jcas) {
        indexScopes(jcas);
        for (Class evidenceTypeClass : ruleMap.keySet()) {
            findNAddMatches(jcas, evidenceTypeClass, evidenceTypeClass.getSimpleName());
        }
    }

    private void indexScopes(JCas jCas) {
        scopes.clear();
        for (Class<? extends Annotation> scopeClass : scopeIndex.values()) {
            IntervalST<Annotation> scopeTree = new IntervalST<>();
            for( Annotation scopeAnnotation: JCasUtil.select(jCas, scopeClass)) {
                scopeTree.put(new Interval1D(scopeAnnotation.getBegin(), scopeAnnotation.getEnd()), scopeAnnotation);
            }
            scopes.put(scopeClass, scopeTree);
        }

    }

    public void findNAddMatches(JCas jcas, Class<? extends Annotation> evidenceTypeClass, String evidenceTypeShortName) {
        ArrayList<Annotation> scheduledRemoval = new ArrayList<>();
        ArrayList<Annotation> scheduledSaving = new ArrayList<>();
        for( Annotation evidenceAnnotation: JCasUtil.select(jcas, evidenceTypeClass)) {
            if (strictNameMatch && !evidenceAnnotation.getClass().getSimpleName().equals(evidenceTypeShortName))
                continue;
            HashMap<String, AnnotationDefinition> conclusions = findMatches(evidenceAnnotation, evidenceTypeClass,
                    evidenceConceptGetFeatures.get(evidenceTypeShortName), ruleMap.get(evidenceTypeClass), new HashMap<>());
            if (conclusions.size() > 0) {
                if (removeEvidenceConcept)
                    scheduledRemoval.add(evidenceAnnotation);
                addToScheduledSaving(jcas, evidenceTypeShortName, conclusions, evidenceAnnotation, scheduledSaving);
            }
        }
        for (Annotation annotation : scheduledRemoval) {
            annotation.removeFromIndexes();
        }
        for (Annotation annotation : scheduledSaving) {
            annotation.addToIndexes();
        }
        return;
    }

    private void addToScheduledSaving(JCas jcas, String evidenceTypeShortName, HashMap<String, AnnotationDefinition> conclusions,
                                      Annotation evidenceAnnotation, ArrayList<Annotation> scheduledSaving) {
        Annotation anno = null;

        for (AnnotationDefinition conclusionAnnotationDefinition : conclusions.values()) {
            AnnotationDefinition conclusionDef = AnnotationOper.createConclusionAnnotationDefinition(conclusionAnnotationDefinition,
                    evidenceConceptGetFeatures, uniqueFeatureClassMap, Arrays.asList(new Annotation[]{evidenceAnnotation}), typeDefinitions);
            String resultTypeShortName = conclusionAnnotationDefinition.getShortTypeName();
            anno = AnnotationOper.createAnnotation(jcas, conclusionDef, conceptTypeConstructors.get(resultTypeShortName),
                    evidenceAnnotation.getBegin(), evidenceAnnotation.getEnd(), conclusionConceptSetFeatures.get(conceptClassMap.get(resultTypeShortName)));
//            System.out.println(anno.getClass());
//            System.out.println(conceptClassMap.get(resultTypeShortName).isInstance(anno));
//            System.out.println(AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3")).isInstance(anno));
            scheduledSaving.add(anno);
        }
    }

    private HashMap<String, AnnotationDefinition> findMatches(Annotation annotation, Class typeClass, HashMap<String, Method> methodHashMap,
                                                              Object value, HashMap<String, AnnotationDefinition> conclusions) {
        AnnotationDefinition conclusionType;
        if (value instanceof AnnotationDefinition) {
            conclusionType = (AnnotationDefinition) value;
            conclusions.put(conclusionType.getShortTypeName(), conclusionType);
            return conclusions;
        }
//      TODO solve conflict results (Same input annotation, match to multiple results, where some results are mutual exclusive.)
        LinkedHashMap<String, Object> ruleMaps = (LinkedHashMap<String, Object>) value;
        for (String featureName : ruleMaps.keySet()) {
            if (featureName.equals(END)) {
                for (String ruleId : (ArrayList<String>) ruleMaps.get(END))
                    addConclusion(ruleId, annotation, conclusions);
                break;
            }
            try {
                if (!methodHashMap.containsKey(featureName)) {
                    logger.warning("Feature Name: " + featureName + " hasn't been initiated.");
                }
                String annotationFeatureValue = (String) methodHashMap.get(featureName).invoke(annotation);
                Object nextStep = ruleMaps.get(featureName);
                if (((LinkedHashMap<String, Object>) nextStep).containsKey(annotationFeatureValue)) {
                    nextStep = ((LinkedHashMap<String, Object>) nextStep).get(annotationFeatureValue);
                    if (((LinkedHashMap<String, Object>) nextStep).containsKey(END)) {
                        for (String ruleId : (ArrayList<String>) ((LinkedHashMap<String, Object>) nextStep).get(END))
                            addConclusion(ruleId, annotation, conclusions);
//						break;
                    }
                    findMatches(annotation, typeClass, methodHashMap, nextStep, conclusions);
                } else {
                    continue;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return conclusions;
    }

    private void addConclusion(String ruleId, Annotation annotation, HashMap<String, AnnotationDefinition> conclusions) {

        AnnotationDefinition conclusionType;
        if (scopeIndex.containsKey(ruleId)) {
//							a scope is specified in the rule
            if (scopes.get(scopeIndex.get(ruleId)).search(new Interval1D(annotation.getBegin(), annotation.getEnd())) != null) {
//								the annotation is within the specified scope
                conclusionType = conclusionAnnotationDefinitions.get(ruleId);
                conclusions.put(conclusionType.getShortTypeName(), conclusionType);
            }
        } else {
//							TODO need test if scope is specified but not matched
//							no scope is specificed in the rule
            conclusionType = conclusionAnnotationDefinitions.get(ruleId);
            conclusions.put(conclusionType.getShortTypeName(), conclusionType);
        }

    }

}
