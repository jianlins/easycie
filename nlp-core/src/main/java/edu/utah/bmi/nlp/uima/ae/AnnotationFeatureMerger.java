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

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.*;

/**
 * Created by Jianlin Shi on 7/4/17.
 */
public class AnnotationFeatureMerger {
    public static Logger logger = IOUtil.getLogger(AnnotationFeatureMergerAnnotator.class);
    private LinkedHashMap<String, ArrayList<String>> conceptFeaturesRuleMap = new LinkedHashMap<>();
    private LinkedHashMap<Type, ArrayList<Feature>> conceptFeaturesMap = new LinkedHashMap<>();
    private LinkedHashMap<String, HashMap<String, Integer>> featureValuesRulePriorities = new LinkedHashMap<>();
    private LinkedHashMap<Feature, HashMap<String, Integer>> featureValuesPriorities = new LinkedHashMap<>();
    private HashMap<String, Class> conceptMergedConceptMap = new HashMap<>();
    @Deprecated
    private boolean debug = false;
    private boolean insitu = false;

    @Deprecated
    public AnnotationFeatureMerger(String ruleStr, boolean debug, boolean insitu) {
        init(ruleStr, insitu);
    }

    public AnnotationFeatureMerger(String ruleStr, boolean insitu) {
        init(ruleStr, insitu);
    }


    @Deprecated
    public void init(String ruleStr, boolean debug, boolean insitu) {
        init(ruleStr, insitu);
    }

    public void init(String ruleStr, boolean insitu) {
        this.insitu=insitu;
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        for (ArrayList<String> initRow : ioUtil.getInitiations()) {
            String header = initRow.get(1).trim();
            switch (header) {
                case FEATURE_VALUES1:
                case FEATURE_VALUES2:
                    String featureName = initRow.get(2);
                    featureValuesRulePriorities.put(featureName, new HashMap<>());
                    for (int i = 3; i < initRow.size(); i++) {
                        featureValuesRulePriorities.get(featureName).put(initRow.get(i), i - 2);
                    }
                    break;
                case CONCEPT_FEATURES1:
                case CONCEPT_FEATURES2:
                    String sourceTypeName = initRow.get(3);
                    String mergedTypeName = initRow.get(2);
                    conceptMergedConceptMap.put(sourceTypeName,
                            AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(mergedTypeName)));
                    conceptFeaturesRuleMap.put(sourceTypeName, new ArrayList<>());
                    for (int i = 4; i < initRow.size(); i++) {
                        conceptFeaturesRuleMap.get(sourceTypeName).add(initRow.get(i));
                    }
                    break;
            }
        }
    }

    public void mergeAnnotations(JCas jCas) {
        CAS cas = jCas.getCas();
        int i = 0;
        for (String conceptName : conceptFeaturesRuleMap.keySet()) {
            Type type = CasUtil.getAnnotationType(cas, DeterminantValueSet.checkNameSpace(conceptName));
            Annotation mergedAnno = null;
            if (!conceptFeaturesMap.containsKey(type)) {
                conceptFeaturesMap.put(type, new ArrayList<>());
                for (String featureName : conceptFeaturesRuleMap.get(conceptName)) {
                    Feature fObj = type.getFeatureByBaseName(featureName);
                    conceptFeaturesMap.get(type).add(fObj);
                    if (!featureValuesPriorities.containsKey(fObj)) {
                        featureValuesPriorities.put(fObj, featureValuesRulePriorities.get(featureName));
                    }
                }
            }

            Iterator<AnnotationFS> iter = CasUtil.iterator(cas, type);
            while (iter.hasNext()) {
                AnnotationFS anno = iter.next();
                if (mergedAnno == null) {
                    if (insitu)
                        mergedAnno = AnnotationFactory.createAnnotation(jCas, anno.getBegin(), anno.getEnd(), conceptMergedConceptMap.get(conceptName));
                    else {
                        mergedAnno = AnnotationFactory.createAnnotation(jCas, i, i + 3, conceptMergedConceptMap.get(conceptName));
                        i += 3;
                    }
                }
                for (Feature feature : conceptFeaturesMap.get(type)) {
                    String currentValue = anno.getFeatureValueAsString(feature);
                    if (currentValue == null)
                        continue;
                    String mergedValue = mergedAnno.getFeatureValueAsString(feature);
                    if (!featureValuesPriorities.containsKey(feature)) {
                        logger.info("featureValuesPriorities doesn't have feature: " + feature);
                    } else if (!featureValuesPriorities.get(feature).containsKey(currentValue)) {
                        logger.info("Feature " + feature + " doesn't have value: " + currentValue);
                    }
                    if (mergedValue == null || featureValuesPriorities.get(feature).getOrDefault(currentValue,-1) >
                            featureValuesPriorities.get(feature).getOrDefault(mergedValue,-1)) {
                        mergedAnno.setFeatureValueFromString(feature, currentValue);
                    }
                }
            }
            if (mergedAnno != null)
                mergedAnno.addToIndexes();
        }

    }

    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleStr) {
        LinkedHashMap<String, TypeDefinition> typeDefinitionLinkedHashMap = new LinkedHashMap<>();
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        for (ArrayList<String> initRow : ioUtil.getInitiations()) {
            String header = initRow.get(1).trim();
            switch (header) {
                case CONCEPT_FEATURES1:
                case CONCEPT_FEATURES2:
                    String typeName = initRow.get(2);
                    String sourceTypeName = initRow.get(3);
                    typeDefinitionLinkedHashMap.put(typeName, new TypeDefinition(typeName, sourceTypeName));
                    break;
            }
        }
        return typeDefinitionLinkedHashMap;
    }
}
