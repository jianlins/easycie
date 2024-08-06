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
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.uima.common.AnnotationOper.initGetReflections;
import static edu.utah.bmi.nlp.uima.common.AnnotationOper.initSetReflections;
import static java.lang.Character.isUpperCase;

/**
 * Extend previous version FeatureAnnotationInferencer to allow priority setting among multiple matched rules.
 *
 * @author Jianlin Shi on 4/19/19.
 */
public class FeatureAnnotationInferencerEx implements FeatureInferencerInf {
    public static Logger logger = IOUtil.getLogger(FeatureInferenceAnnotator.class);
    private LinkedHashMap<Class, Object> ruleMap = new LinkedHashMap<>();
    private HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures = new HashMap<>();
    private HashMap<Class, HashMap<String, Method>> conclusionConceptSetFeatures = new HashMap<>();
    private HashMap<String, Class<? extends Annotation>> conceptClassMap = new HashMap<>();
    private HashMap<String, Constructor<? extends Annotation>> conceptTypeConstructors = new HashMap<>();
    private LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();
    private HashMap<Integer, AnnotationDefinition> conclusionAnnotationDefinitions = new HashMap<>();
    private HashMap<String, String> uniqueFeatureClassMap = new HashMap<>();
    //  save this map to support short form specification of evidence feature values
    private HashMap<String, String> valueFeatureMap = new HashMap<>();
    //	scope type simple name -- scope type class
    private HashMap<String, Class<? extends Annotation>> scopeIndex = new HashMap<>();
    private HashMap<Class, IntervalST<Annotation>> scopes = new HashMap<>();
    private boolean removeEvidenceConcept = true;
    private boolean noteRuleId = true;
    private ArrayList<ArrayList<String>> ruleCells = new ArrayList<>();
    @Deprecated
    public boolean debug = false;
    private boolean strictNameMatch = false;
    private static String END = "<END>";
    public HashMap<Integer, ArrayList<String>> ruleStore = new HashMap<>();


    /**
     * This constructor is only used to read type definitions.
     * It won't save rules and initiate rule Map.
     */
    public FeatureAnnotationInferencerEx() {

    }

    public FeatureAnnotationInferencerEx(String ruleStr, boolean removeEvidenceConcept, boolean strictNameMatch, boolean noteRuleId) {
        typeDefinitions = getTypeDefinitions(ruleStr);
        this.removeEvidenceConcept = removeEvidenceConcept;
        this.strictNameMatch = strictNameMatch;
        this.noteRuleId = noteRuleId;
    }

    public FeatureAnnotationInferencerEx(IOUtil ioUtil, boolean removeEvidenceConcept, boolean strictNameMatch, boolean noteRuleId) {
        typeDefinitions = getTypeDefinitions(ioUtil);
        this.removeEvidenceConcept = removeEvidenceConcept;
        this.strictNameMatch = strictNameMatch;
        this.noteRuleId = noteRuleId;
    }

    @Deprecated
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
            try {
                String conclusionTypeName = row.get(2).trim();
                String evidenceTypeName = row.get(4);
                HashMap<String, String> evidenceTypeMethodTypes = getMethodTypes(evidenceTypeName);
                TypeDefinition typeDefinition;
                if (!typeDefinitions.containsKey(conclusionTypeName)) {
                    typeDefinition = new TypeDefinition(conclusionTypeName, Concept.class.getCanonicalName(), new ArrayList<>());
                } else {
                    typeDefinition = typeDefinitions.get(conclusionTypeName);
                }
//            in case some feature names are missed in previous concept type definition.
                for (String featureValuePairString : row.get(3).split(",")) {
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
            } catch (Exception e) {
                logger.warning("Error to parse row: " + row);
            }
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
            try {
                ruleStore.put(Integer.parseInt(row.get(0)), row);
                String conclusionTypeName = row.get(2);
//				get conclusion annotation definition
                ArrayList<String> needEvidenceFeatures = new ArrayList<>();
                AnnotationDefinition annotationDefinition = new AnnotationDefinition(typeDefinitions.get(conclusionTypeName));

                String evidenceTypeName = row.get(4);
                Class<? extends Annotation> evidenceTypeClass = AnnotationOper.getTypeClass(evidenceTypeName);
                if (evidenceTypeClass == null) {
                    System.out.println(row);
                    logger.warning("Class " + evidenceTypeName + " has not been loaded to memory, check the Class definition for rule: " + row);
                    continue;
                }
                String evidenceTypeShortName = DeterminantValueSet.getShortName(evidenceTypeName);

                augmentRule(row.get(2), row.get(3), evidenceTypeClass, evidenceTypeShortName, annotationDefinition, needEvidenceFeatures);


//				TODO check if "put" is needed
                conceptClassMap.put(evidenceTypeShortName, evidenceTypeClass);
//				if evidence annotation specified certain feature values
                int ruleId = Integer.parseInt(row.get(0));
                if (row.size() > 5 && row.get(5).trim().length() > 0) {
                    if (!ruleMap.containsKey(evidenceTypeClass)) {
                        ruleMap.put(evidenceTypeClass, new LinkedHashMap<>());
                    }
                    LinkedHashMap<String, Object> tmp = (LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass);
                    String[] featureValueMatchRule = augmentConditionRule(row.get(5));
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
                            tmp.put(END, new ArrayList<Integer>());
                        }
//                  map topic name to rule id.
                        ((ArrayList<Integer>) tmp.get(END)).add(ruleId);
                        conclusionAnnotationDefinitions.put(ruleId, annotationDefinition);
                    } else {
                        logger.info("Rule error: " + row.get(4) + "\n\tin rule: " + row);
                        logger.info("Feature and value need to be paired.");
                    }

                } else {
                    if (row.size() == 5)
                        ruleStore.get(Integer.parseInt(row.get(0))).add("");
//						not feature value is specified
                    if (!ruleMap.containsKey(evidenceTypeClass)) {
                        ruleMap.put(evidenceTypeClass, new LinkedHashMap<>());
                        ((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).put(END, new ArrayList());
                    } else if (!((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).containsKey(END)) {
                        ((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).put(END, new ArrayList());
                    }
                    ((ArrayList<Integer>) ((LinkedHashMap<String, Object>) ruleMap.get(evidenceTypeClass)).get(END)).add(ruleId);
                    conclusionAnnotationDefinitions.put(ruleId, annotationDefinition);
                    initGetReflections(evidenceConceptGetFeatures, evidenceTypeShortName, evidenceTypeClass, new String[]{}, needEvidenceFeatures);
                }
                if (row.size() > 6 && row.get(6).trim().length() > 0) {
                    String scopeShortName = row.get(6).trim();
                    if (scopeShortName.equals("DocumentAnnotation") || scopeShortName.equals("SourceDocumentInformation")
                            || scopeShortName.equals(SourceDocumentInformation.class.getCanonicalName())
                            || scopeShortName.equals(DocumentAnnotation.class.getCanonicalName()))
                        continue;
                    Class scopeClass = AnnotationOper.getTypeClass(scopeShortName);
                    if (scopeClass == null) {
                        logger.warning("Class " + scopeShortName + "' has not been loaded to memory. Check section rules see if this section has been defined.");
                    }
                    scopeIndex.put(scopeClass.getSimpleName(), scopeClass);
                } else if (row.size() == 6) {
//              padding cells.
                    ruleStore.get(Integer.parseInt(row.get(0))).add("");
                }
            } catch (Exception e) {
                logger.warning("Rule error: " + row.get(4) + "\n\tin rule: " + row);
                logger.warning("Feature and value need to be paired.");
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
        scopes.clear();
        LinkedHashMap<String, LinkedHashMap<Double, Object[]>> allMatches = new LinkedHashMap<>();
        ArrayList<Annotation> scheduledRemoval = new ArrayList<>();
        for (Class evidenceTypeClass : ruleMap.keySet()) {
            findNAddMatches(jcas, evidenceTypeClass, evidenceTypeClass.getSimpleName(), allMatches, scheduledRemoval);
        }
//      need to merge duplicated conclusion annotations.
        LinkedHashMap<String, LinkedHashMap<Double, Annotation>> scheduledSaving = new LinkedHashMap<>();

        for (String topic : allMatches.keySet()) {
            LinkedHashMap<Double, Object[]> matches = allMatches.get(topic);
            for (double offset : matches.keySet()) {
                Object[] matched = matches.get(offset);
                addToScheduledSaving(jcas, matched[0], conclusionAnnotationDefinitions.get(matched[0]), (Annotation) matched[1], scheduledSaving);
//              remove the evidence after all inferences are made, because one evidence could be matched to different conclusion topics.
            }
        }
        for (Annotation annotation : scheduledRemoval) {
            annotation.removeFromIndexes();
        }
        for (LinkedHashMap<Double, Annotation> entries : scheduledSaving.values()) {
            for (Annotation annotation : entries.values()) {
                logger.finest("Add annotation: " + annotation);
                annotation.addToIndexes();
            }
        }
    }

    private void indexScopes(JCas jCas, Class<? extends Annotation> scopeClass) {
        IntervalST<Annotation> scopeTree = new IntervalST<>();
        for (Annotation scopeAnnotation : JCasUtil.select(jCas, scopeClass)) {
            scopeTree.put(new Interval1D(scopeAnnotation.getBegin(), scopeAnnotation.getEnd()), scopeAnnotation);
        }
        scopes.put(scopeClass, scopeTree);
    }

    public void findNAddMatches(JCas jcas, Class<? extends Annotation> evidenceTypeClass, String evidenceTypeShortName,
                                LinkedHashMap<String, LinkedHashMap<Double, Object[]>> allMatches, ArrayList<Annotation> scheduledRemoval) {

        logger.finest("\n\nIterate evidence type: " + evidenceTypeClass.getSimpleName());
        ArrayList<Annotation> scheduledSaving = new ArrayList<>();
        for (Annotation evidenceAnnotation : JCasUtil.select(jcas, evidenceTypeClass)) {
            if (strictNameMatch && !evidenceAnnotation.getClass().getSimpleName().equals(evidenceTypeShortName))
                continue;
            boolean matched = findMatches(jcas, evidenceAnnotation, ruleMap.get(evidenceTypeClass), allMatches);

            if (matched && removeEvidenceConcept)
                scheduledRemoval.add(evidenceAnnotation);
        }
    }

    private void addToScheduledSaving(JCas jcas, Object matchedRuleId, AnnotationDefinition conclusionAnnotationDefinition,
                                      Annotation evidenceAnnotation, LinkedHashMap<String, LinkedHashMap<Double, Annotation>> scheduledSaving) {
        Annotation anno;
        AnnotationDefinition conclusionDef = AnnotationOper.createConclusionAnnotationDefinition(conclusionAnnotationDefinition,
                evidenceConceptGetFeatures, uniqueFeatureClassMap, Arrays.asList(new Annotation[]{evidenceAnnotation}), typeDefinitions);
        String resultTypeShortName = conclusionAnnotationDefinition.getShortTypeName();

        if (!scheduledSaving.containsKey(resultTypeShortName))
            scheduledSaving.put(resultTypeShortName, new LinkedHashMap<>());
        double offsets = concatenateIntegerToDouble(evidenceAnnotation.getBegin(), evidenceAnnotation.getEnd());
        if (!scheduledSaving.get(resultTypeShortName).containsKey(offsets)) {
            anno = AnnotationOper.createAnnotation(jcas, conclusionDef, conceptClassMap.get(resultTypeShortName),
                    evidenceAnnotation.getBegin(), evidenceAnnotation.getEnd());
            if (noteRuleId && anno instanceof Concept) {
                Concept con = (Concept) anno;
                String note = con.getNote();
                if (note == null || note.length() == 0)
                    con.setNote("\tMachedId:\t" + matchedRuleId);
                else
                    con.setNote(con.getNote() + "\n\tMachedId:\t" + matchedRuleId);

            }
            scheduledSaving.get(resultTypeShortName).put(offsets, anno);
        }
    }


    private boolean findMatches(JCas jCas, Annotation annotation, Object value, LinkedHashMap<String, LinkedHashMap<Double, Object[]>> allMatches) {

        logger.finest("\nAnalyze evidence annotation: " + annotation.getClass().getSimpleName()
                + " [" + annotation.getBegin() + "-" + annotation.getEnd() + "]");
        double offsets = concatenateIntegerToDouble(annotation.getBegin(), annotation.getEnd());
        return findMatchedRuleIds(jCas, annotation, offsets, value, allMatches);
    }

    private boolean findMatchedRuleIds(JCas jCas, Annotation annotation, double offsets, Object value, LinkedHashMap<String, LinkedHashMap<Double, Object[]>> allMatches) {
        boolean found = false;
        LinkedHashMap<String, Object> ruleMaps = (LinkedHashMap<String, Object>) value;
        for (String featureName : ruleMaps.keySet()) {
            if (featureName.equals(END)) {
                for (Integer ruleId : (ArrayList<Integer>) ruleMaps.get(END))
                    if (checkScope(jCas, ruleId, annotation)) {
                        String topic = ruleStore.get(ruleId).get(1);
                        if (logger.isLoggable(Level.FINEST)) {
                            if (!allMatches.containsKey(topic) || !allMatches.get(topic).containsKey(offsets))
                                logger.finest("Add matched rule: " + topic + " -- " + ruleId);
                            else if ((int) allMatches.get(topic).get(offsets)[0] > ruleId)
                                logger.finest("Update matched rule: " + topic + " --- from " + toString(allMatches.get(topic)) + " to [ruleId: " + ruleId + ", " + annotation.getClass().getSimpleName() + "]");
                            else
                                logger.finest("Keep existing matched rule: " + topic + " --- " + toString(allMatches.get(topic)) + " rather than updating to [ruleId: " + ruleId + ", " + annotation.getClass().getSimpleName() + "]");
                        }
                        if (!allMatches.containsKey(topic)) {
                            allMatches.put(topic, new LinkedHashMap<>());
                        }
                        if (!allMatches.get(topic).containsKey(offsets) || (int) allMatches.get(topic).get(offsets)[0] > ruleId) {
                            allMatches.get(topic).put(offsets, new Object[]{ruleId, annotation});
                        }
                        found = true;
                    }
                continue;
            }

            String annotationFeatureValue = "" + AnnotationOper.getFeatureValue(featureName, annotation);


            Object nextStep = ruleMaps.get(featureName);
            if (((LinkedHashMap<String, Object>) nextStep).containsKey(annotationFeatureValue)) {
                logger.finest("Matched rule: " + featureName + ":" + annotationFeatureValue);
                nextStep = ((LinkedHashMap<String, Object>) nextStep).get(annotationFeatureValue);
                if (findMatchedRuleIds(jCas, annotation, offsets, nextStep, allMatches))
                    found = true;
            } else {
                continue;
            }
        }
        return found;
    }

    private boolean checkScope(JCas jCas, Integer ruleId, Annotation annotation) {
        ArrayList<String> rule = ruleStore.get(ruleId);
        String scopeType = rule.get(6);
        if (scopeType.length() == 0 || scopeType.equals("DocumentAnnotation") || scopeType.equals("SourceDocumentInformation")
                || scopeType.equals(SourceDocumentInformation.class.getCanonicalName())
                || scopeType.equals(DocumentAnnotation.class.getCanonicalName()))
            return true;
        if (!scopeIndex.containsKey(scopeType) || !scopes.containsKey(scopeIndex.get(scopeType)))
            indexScopes(jCas, scopeIndex.get(scopeType));
        if (scopeIndex.containsKey(scopeType)) {
            if (scopes.get(scopeIndex.get(scopeType)).search(new Interval1D(annotation.getBegin(), annotation.getEnd())) != null)
                return true;
        }
        return false;
    }

    private void addConclusion(Integer ruleId, Annotation annotation, HashMap<String, AnnotationDefinition> conclusions) {

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

    /**
     * Convert two integer into a single float number:
     * begin value + the converted end value (put the digits on the right side of the decimal)
     *
     * @param begin annotation begin
     * @param end   annotation end
     * @return concatenated annotation position information
     */
    public static double concatenateIntegerToDouble(int begin, int end) {
        int count = countNumberOfDigits(end);
        double denominator = Math.pow(10, count);
        double res = begin + (1.0 * end / denominator);
        return res;
    }

    /**
     * According to https://www.baeldung.com/java-number-of-digits-in-int
     * This is dumbest but surprisingly fastest.
     * Lean towards greatest probability of digit value in annotation---between 10~1000 (guarantee to solve in two comparisons)
     *
     * @param digit an input integer number
     * @return how many digits does this integer have
     */
    public static int countNumberOfDigits(int digit) {
        if (digit > 100) {
            if (digit < 1000)
                return 3;
            else if (digit > 10000)
                return 5;
            else
                return 4;
        } else {
            if (digit > 10)
                return 2;
            else
                return 1;
        }
    }


    public String toString(LinkedHashMap<Double, Object[]> match) {
        StringBuilder sb = new StringBuilder();
        for (double key : match.keySet()) {
            sb.append("{");
            sb.append(key);
            sb.append("=[ruleId: ");
            sb.append(match.get(key)[0]);
            sb.append(", ");
            sb.append(((Annotation) match.get(key)[1]).getClass().getSimpleName());
            sb.append("]}");
        }
        return sb.toString();
    }
}
