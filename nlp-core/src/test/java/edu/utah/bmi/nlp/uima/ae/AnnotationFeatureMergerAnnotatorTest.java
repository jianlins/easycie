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

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * Created by Jianlin Shi on 7/4/17.
 */
public class AnnotationFeatureMergerAnnotatorTest {
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine featureMerger, printer;
    private String typeDescriptor;

    @BeforeAll
    public static void initLoader(){
        MemoryClassLoader.CURRENT_LOADER_NAME ="tesetMerger";
    }
    @AfterAll
    public static void resetLoader(){
        MemoryClassLoader.resetLoaderName();
    }


    @BeforeEach
    public void init() {
        typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
    }

    private void init(String ruleStr, String printTypeName) {
        Object[] configurationData = new Object[]{FeatureInferenceAnnotator.PARAM_RULE_STR, ruleStr,
                FeatureInferenceAnnotator.PARAM_REMOVE_EVIDENCE_CONCEPT, true};
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new AnnotationFeatureMergerAnnotator().getTypeDefs(ruleStr);
        runner.addConceptTypes(typeDefinitions.values());
        runner.reInitTypeSystem("target/generated-test-sources/customized");
        jCas = runner.initJCas();
        try {
            featureMerger = createEngine(AnnotationFeatureMergerAnnotator.class,
                    configurationData);
            printer = createEngine(AnnotationPrinter.class,
                    AnnotationPrinter.PARAM_TYPE_NAME,
                    printTypeName);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test1() throws Exception {
        String ruleStr = "@splitter:|\n" +
                "@CONCEPT_FEATURES|Concept_Doc|Concept|Negation|Certainty\n" +
                "@FEATURE_VALUES|Negation|affirm|negated\n" +
                "@FEATURE_VALUES|Certainty|certain|uncertain";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        Concept concept = new Concept(jCas, 1, 3);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.addToIndexes();

        concept = new Concept(jCas, 5, 6);
        concept.setNegation("affirm");
        concept.setCertainty("uncertain");
        concept.addToIndexes();

        featureMerger.process(jCas);
        printer.process(jCas);
        Collection<? extends Concept> annos = JCasUtil.select(jCas, AnnotationOper.getTypeClass("Concept_Doc").asSubclass(Concept.class));
        assert (annos.size()==1);
        System.out.println(annos.iterator().next().getClass());

    }

    @Test
    public void test2() throws Exception {
        String ruleStr = "@splitter:|\n" +
                "@CONCEPT_FEATURES|Concept_Doc|Concept|Negation|Certainty\n" +
                "@FEATURE_VALUES|Negation|affirm|negated\n" +
                "@FEATURE_VALUES|Certainty|certain|uncertain";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        Concept concept = new Concept(jCas, 1, 3);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setCategory("cat1");
        concept.addToIndexes();

        concept = new Concept(jCas, 1, 4);
        concept.setNegation("affirm");
        concept.setCertainty("uncertain");
        concept.addToIndexes();

        featureMerger.process(jCas);
        printer.process(jCas);


    }

}