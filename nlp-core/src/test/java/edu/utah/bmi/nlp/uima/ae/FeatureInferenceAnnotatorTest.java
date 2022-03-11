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
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static edu.utah.bmi.nlp.uima.ae.FeatureAnnotationInferencerEx.concatenateIntegerToDouble;
import static edu.utah.bmi.nlp.uima.ae.FeatureAnnotationInferencerEx.countNumberOfDigits;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * @author Jianlin Shi on 5/10/17.
 */
public class FeatureInferenceAnnotatorTest {
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine featureInferencer, printer;
    private String typeDescriptor;
    private static boolean printAnnos = false;

    @BeforeEach
    public void init() {
        typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        MemoryClassLoader.CURRENT_LOADER_NAME =DeterminantValueSet.randomString();
    }

    @AfterAll
    public static void resetLoader(){
        MemoryClassLoader.resetLoaderName();
    }

    private void init(String ruleStr, String printTypeName) {
        Object[] configurationData = new Object[]{FeatureInferenceAnnotator.PARAM_RULE_STR, ruleStr,
                FeatureInferenceAnnotator.PARAM_REMOVE_EVIDENCE_CONCEPT, true};
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new FeatureInferenceAnnotator().getTypeDefs(ruleStr);
        runner.addConceptTypes(typeDefinitions.values());
        runner.reInitTypeSystem("target/generated-test-sources/customized");
        jCas = runner.initJCas();
        try {
            featureInferencer = createEngine(FeatureInferenceAnnotator.class,
                    configurationData);
            printer = createEngine(AnnotationPrinter.class,
                    AnnotationPrinter.PARAM_TYPE_NAME,
                    printTypeName);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test1() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "NegatedConcept\tCertainty:$Certainty,Temporality:Temporality\tConcept\tNegation:negated\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 0);
        assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void test2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "NegatedConcept\tCertainty,Temporality\tConcept\tNegation:negated";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Token.class).size() == 1);
        System.out.println(JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size());
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void test3() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "NegatedConcept\tCertainty:Concept,Temporality\tConcept\tNegation:negated";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        targetWords = "month";
        begin = text.indexOf(targetWords);
        end = begin + targetWords.length();
        concept = new Concept(jCas, begin, end);
        concept.setNegation("affirmed");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
//		NegatedConcept is a subclass of Concept
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }


    @Test
    public void test4() throws AnalysisEngineProcessException {

        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "NegatedConcept\tCertainty,Temporality:Concept\tConcept\tNegation:negated\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();

        begin = text.indexOf(targetWords);
        end = begin + targetWords.length();
        concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void test5() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@HistoricalConcept\tConcept\tNegation:affirm\tCertainty:certain\n" +
                "NegatedConcept\tCertainty,Temporality\tConcept\tNegation:negated,Temporality:present,Certainty:certain\tSentence\n" +
                "HistoricalConcept\tCertainty,Negation\tConcept\tTemporality:historical\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class).size());
        assert (JCasUtil.select(jCas, Concept.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("HistoricalConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void test６() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@HistoricalConcept\tConcept\tNegation:affirm\tCertainty:certain\n" +
                "NegatedConcept\tCertainty,Temporality\tConcept\tNegation:negated,Certainty:certain\tSentence\n" +
                "HistoricalConcept\tCertainty,Negation\tConcept\tTemporality:historical\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void test65() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@HistoricalConcept\tConcept\tNegation:affirm\tCertainty:certain\n" +
                "NegatedConcept\tCertainty,Temporality\tConcept\tNegation:negated,Certainty:certain\tSentence\n" +
                "HistoricalConcept\tCertainty,Negation\tConcept\tTemporality:historical\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        System.out.println(runner.getTypeSystemDescription().getType("NegatedConcept"));
    }

    @Test
    public void test7() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\n" +
                "ConceptA\tCertainty:certain,Temporality:current\tConcept\tCertainty:certain,Temporality:present\tSentence\n" +
                "ConceptB\tCertainty:certain\tConcept\tCertainty:certain\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
//		if (printAnnos)
        printer.process(jCas);
//		System.out.println(JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size());
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;

        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptA",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,certain,Temporality,present"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptB",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,certain"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 2
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);

        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
//		assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }


    @Test
    public void testShortForm() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\n" +
                "ConceptA\tCertainty,Temporality\tConcept\tCertainty:certain,Temporality:present\tSentence\n" +
                "ConceptB\tCertainty\tConcept\tCertainty:certain\tSentence";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
//		assert (JCasUtil.select(jCas, Concept.class).size() == 2);
//		assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 1);
//		if (printAnnos)
        printer.process(jCas);
    }

    @Test
    public void testCopyAll() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\n" +
                "ConceptA\tCOPYALL\tConcept\tCertainty:certain,Temporality:present\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        Concept concept1 = JCasUtil.select(jCas, Concept.class).iterator().next();
        assert (concept1 != null);
        assert (concept1.getNegation().equals("negated"));
        assert (concept1.getCertainty().equals("certain"));
        assert (concept1.getTemporality().equals("present"));
    }

    @Test
    public void testCopyAllExcept() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tCertainty:certain,Temporality:present\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        Concept concept1 = JCasUtil.select(jCas, Concept.class).iterator().next();
        assert (concept1 != null);
        assert (concept1.getNegation().equals("negated"));
        assert (concept1.getCertainty().equals("done"));
        assert (concept1.getTemporality().equals("present"));
    }


    @Test
    public void testShortFormConditions() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tuncertain,present\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void testNoFeatureCondition() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\t\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void testNoFeatureCondition2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tNegation:affirm\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALL\tConcept\t\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("historical");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        targetWords = "month";

        new Sentence(jCas, 0, 26).addToIndexes();

        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void testNoFeatureCondition3() throws AnalysisEngineProcessException, ResourceInitializationException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tTemporality:his\tNote:note\tAnnotator:u\n" +
                "@ConceptB\tConcept\tTemporality:his\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALL\tConcept\t\tSentence\n" +
                "ConceptB\tCOPYALL\tConcept\t\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();

        AnalysisEngine evaluater1 = createEngine(AnnotationCountEvaluator.class,
                AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptA",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Temporality,his,Note,test concept1,Annotator,u"
        );
        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        evaluater1.process(jCas);
    }

    @Test
    public void testMultiMatchConditions() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tpresent\tSentence\n" +
                "ConceptB\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tuncertain\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;

        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptA",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptB",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 2
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        new Sentence(jCas, 0, 26).addToIndexes();
        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        printer.process(jCas);
    }


    @Test
    public void testMultiMatchConditions2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@ConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "@ConceptB\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tpresent\tSectionBody\n" +
                "ConceptB\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tuncertain\tSentenceOdd\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        SectionBody section = new SectionBody(jCas, 0, text.length());
        section.addToIndexes();
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;
        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptA",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptB",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 0,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);

//        printer.process(jCas);
//        System.out.println("\n\n\n\n");
        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        section.removeFromIndexes();

        Sentence sentence = new SentenceOdd(jCas, 0, text.length());
        sentence.addToIndexes();
        concept.addToIndexes();
        featureInferencer.process(jCas);
        evaluater1.process(jCas);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == false);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == false);
    }

    @Test
    public void testMultiMatchConditions3() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tTemporality\thistorical\tpresent\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@CONCEPT_FEATURES\tConceptA\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "@CONCEPT_FEATURES\tConceptB\tConcept\tCertainty:certain\tTemporality:present\tNote:note\tAnnotator:u\n" +
                "ConceptA\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tpresent\tSectionBody\n" +
                "ConceptB\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConcept\tuncertain\tSentenceOdd\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("uncertain");
        concept.setTemporality("present");
        concept.setAnnotator("org/apache/uima");
        concept.setNote("test concept1");
        concept.addToIndexes();
        SectionBody section = new SectionBody(jCas, 0, text.length());
        section.addToIndexes();
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;
        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptA",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptB",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 0,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        featureInferencer.process(jCas);

//        printer.process(jCas);
//        System.out.println("\n\n\n\n");
        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        section.removeFromIndexes();

        Sentence sentence = new SentenceOdd(jCas, 0, text.length());
        sentence.addToIndexes();
        concept.addToIndexes();
        featureInferencer.process(jCas);
        evaluater1.process(jCas);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == false);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == false);


    }


    @Test
    public void testMultiFeatureTypes2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tNValue:uima.cas.Integer\t11\t12\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@CONCEPT_FEATURES\tConceptD\tConcept\tCertainty:certain\tNValue\tNote:note\tAnnotator:u\n" +
                "@ConceptE\tConcept\tCertainty:certain\tNValue\tNote:note\tAnnotator:u\n" +
                "ConceptE\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConceptD\tuncertain\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        try {
            Class<? extends Concept> conceptDClass = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ConceptD")).asSubclass(Concept.class);
            System.out.println(conceptDClass);
            Constructor<? extends Concept> constructor = conceptDClass.getConstructor(JCas.class, int.class, int.class);
            AnnotationOper.getTypeClass("");
            Concept anno = AnnotationFactory.createAnnotation(jCas, begin, end, conceptDClass);
            Method setMethod = AnnotationOper.getDefaultSetMethod(anno.getClass(), "NValue");
            anno.setNegation("negated");
            anno.setCertainty("uncertain");
            anno.setTemporality("present");
            anno.setAnnotator("org/apache/uima");
            anno.setNote("test concept1");
            FSUtil.setFeature(anno, "NValue", 13);
            anno.addToIndexes();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;

        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptD",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 0,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptE",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);

        if (printAnnos)
            printer.process(jCas);
    }


    /**
     * Only supported in JDK<=1.8
     * @throws AnalysisEngineProcessException
     */
    @Disabled
    @Test
    public void testMultiFeatureTypes() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@casesensitive:\ttrue\n" +
                "@FEATURE_VALUES\tNValue:uima.cas.Integer\t11\t12\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@CONCEPT_FEATURES\tConceptD\tConcept\tCertainty:certain\tNValue\tNote:note\tAnnotator:u\n" +
                "@ConceptE\tConcept\tCertainty:certain\tNValue\tNote:note\tAnnotator:u\n" +
                "ConceptE\tCOPYALLEXCEPT,Certainty:done,Note:no note\tConceptD\tuncertain\tSentence\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        try {
            Class<? extends Concept> conceptDClass = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ConceptD")).asSubclass(Concept.class);
            System.out.println(conceptDClass);
            Constructor<? extends Concept> constructor = conceptDClass.getConstructor(JCas.class, int.class, int.class);
            AnnotationOper.getTypeClass("");

            Concept anno = constructor.newInstance(jCas, begin, end);

            Method setMethod = AnnotationOper.getDefaultSetMethod(anno.getClass(), "NValue");
            anno.setNegation("negated");
            anno.setCertainty("uncertain");
            anno.setTemporality("present");
            anno.setAnnotator("org/apache/uima");
            anno.setNote("test concept1");
            setMethod.invoke(anno, 13);
            anno.addToIndexes();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        AnalysisEngine evaluater1 = null, evaluater2 = null, evaluater3 = null;

        try {
            evaluater1 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptD",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 0,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater2 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptE",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1,
                    AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Certainty,done,Note,no note"
            );
            evaluater3 = createEngine(AnnotationCountEvaluator.class,
                    AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                    AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1
            );
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        new Sentence(jCas, 0, 26).addToIndexes();


        Token token = new Token(jCas, 3, 7);
        token.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        evaluater1.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater2.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);
        evaluater3.process(jCas);
        assert (AnnotationCountEvaluator.pass == true);

        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void test12() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE\tuima.tcas.Annotation\tNegation:affirm\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "ADE\tNegation:negated\tConcept\t\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 0);
        assert (JCasUtil.select(jCas, Annotation.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE"))).size() == 1);
    }

    @Test
    public void testCaptialValues() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE\tuima.tcas.Annotation\tNegation:AFFIRM\n" +
                "@FEATURE_VALUES\tNegation\tAFFIRM\tNEGATED\n" +
                "ADE\tNegation:NEGATED\tConcept\t\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 0);
        assert (JCasUtil.select(jCas, Annotation.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE"))).size() == 1);
    }

    @Test
    public void testNewFeatures() throws AnalysisEngineProcessException, NoSuchMethodException, ClassNotFoundException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tAnnotation\tNegation:affirm\tTemporality:current\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tTemporality\tcurrent\thistorical\thypothetical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE3\tCOPYALL\tConcept\tnegated\tDocumentAnnotation\n";
        init(ruleStr, DeterminantValueSet.checkNameSpace("ADE3"));
        Class cls = null;
//        cls = AnnotationOper.getTypeClass("edu.utah.bmi.nlp.type.system.ADE3");
        ClassLoader uloader = featureInferencer.getUimaContextAdmin().getResourceManager().getExtensionClassLoader();
        cls = uloader.loadClass(DeterminantValueSet.checkNameSpace("ADE3"));

        assert (cls != null);
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        if (printAnnos)
            printer.process(jCas);
        featureInferencer.process(jCas);
//        if (printAnnos)
        printer.process(jCas);
        Thread.currentThread().setContextClassLoader(uloader);
        Collection<? extends Annotation> ades = JCasUtil.select(jCas, cls);

//        ModuleFinder sm = ModuleFinder.ofSystem();
//        Set<ModuleReference> sms = sm.findAll();




        assert (ades.size() == 1);
        Annotation ade = ades.iterator().next();
        System.out.println(AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3")).isInstance(ade));
        System.out.println(ade.getClass());
//        assert (ade.getType().getName().equals("edu.utah.bmi.nlp.type.system.ADE3"));
        Method m2 = AnnotationOper.getDefaultGetMethod(ade.getClass(), "TestFeature");
//        System.out.println(ade.getClass().getMethod("getTestFeature"));
//        Object tf = FSUtil.getFeature(ade, "TestFeature", null);
        Feature f1 = ade.getType().getFeatureByBaseName("TestFeature");
        System.out.println(f1.getRange());
        Object v = FSUtil.getFeature(ade, f1, Object.class);
        System.out.println(v);
        System.out.println(v.getClass());


        System.out.println(uloader);
//        System.out.println(FeatureStructure.class.isInstance(ade));

        FSUtil.setFeature(ade, "TestFeature", "t3");
        v = FSUtil.getFeature(ade, f1, Object.class);
        System.out.println(v);

        List<Feature> fs = ade.getType().getFeatures();
        for (Feature f : fs) {
            System.out.println(f);
        }


        System.out.println(ade);
        System.out.println(Concept.class.isInstance(ade));
        System.out.println(cls.isInstance(ade));
        System.out.println(ade.getClass());
        System.out.println(ade.getClass().getClassLoader());
//        System.out.println(ade.getType());

        ;
//        ModuleFinder finder = ModuleFinder.of(new File("classes").toPath(),
//                new File("src/main/java").toPath(),
//                new File("src/test/java").toPath(),
//                new File("target/generated-test-sources").toPath());




//        Method m = null;
//        try {
//            m = ade.getClass().getMethod("getTestFeature");
//        } catch (NoSuchMethodException e) {
//
//        }
//        assert (m != null);
//        Object value = null;
//        try {
//            value = m.invoke(ade);
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//        assert (value != null && value.equals("f1"));

    }

    /**
     * Rules are grouped by topics. For each topic, if multiple rules are matched,
     * only the rule with the highest weights (the topest one) can be concluded.
     * <p>
     * For the example below, both ADE3 and ADE4 can be matched, but only ADE3 will be concluded.
     *
     * @throws AnalysisEngineProcessException
     */
    @Test
    public void testWeightedRules() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@version:2\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:AFFIRM\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:AFFIRM\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\tAFFIRM\tNEGATED\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\t\tDocumentAnnotation\n" +
                "ADE\tADE4\tCOPYALL\tConcept\t\tDocumentAnnotation\n" +
                "ADE\tADE5\tCOPYALL\tConcept\t\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE4"))).size() == 0);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE5"))).size() == 0);
    }


    @Test
    public void testWeightedRules2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:AFFIRM\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:AFFIRM\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADB\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\tAFFIRM\tNEGATED\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\t\tDefaultSection\n" +
                "ADE\tADE4\tCOPYALL\tConcept\t\tDefaultSection\n" +
                "ADB\tADB\tCOPYALL\tConcept\t\tDefaultSection\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, begin, end);
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE4"))).size() == 0);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADB"))).size() == 1);
    }

    @Test
    public void testWeightedRulesMultipleCoditions() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDefaultSection\n" +
                "ADE\tADE4\tCOPYALL\tConcept\taffirm,certain\tDefaultSection\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical,certain\tDefaultSection\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, begin, end);
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE4"))).size() == 0);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE5"))).size() == 0);
    }

    @Test
    public void testWeightedRulesMultipleEvidenceTypes() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDefaultSection\n" +
                "ADE\tADE4\tCOPYALL\tEntityBASE\t\tDefaultSection\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical,certain\tDefaultSection\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, text.length());
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE4"))).size() == 1);
    }

    @Test
    public void testWeightedRulesMultipleTopicOnSameEvidenceType() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDefaultSection\n" +
                "BDE\tADE3\tCOPYALL\tConcept\thistorical\tDefaultSection\n" +
                "ADE\tADE4\tCOPYALL\tEntityBASE\t\tDefaultSection\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical,certain\tDefaultSection\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, text.length());
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ADE4"))).size() == 1);
    }

    @Test
    public void testWeightedRulesMultipleTopicOnSameEvidenceType2() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDocumentAnnotation\n" +
                "ADE\tADE4\tCOPYALL\tConcept\t\tDocumentAnnotation\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, text.length());
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void testWeightedRulesMultipleTopicOnSameEvidenceType3() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDocumentAnnotation\n" +
                "ADE\tADE4\tCOPYALL\tConcept\taffirm\tDocumentAnnotation\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, text.length());
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();

        concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();

        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }


    @Test
    public void testGreedyFeatureReading() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tADE3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tADE5\tuima.tcas.Annotation\tCertainty:certain\tTestFeature:f1\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "ADE\tADE3\tCOPYALL\tConcept\tnegated\tDocumentAnnotation\n" +
                "ADE\tADE4\tCOPYALL\tEntityBASE\taffirm\tDocumentAnnotation\n" +
                "ADE\tADE5\tCOPYALL\tConcept\thistorical\tDocumentAnnotation\n";
        init(ruleStr, "uima.tcas.Annotation");
        String text = "He does not have any fever one month ago.";
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, text.length());
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("negated");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();

        concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("historical");
        concept.addToIndexes();

        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
    }

    @Test
    public void testWeightedRulesOnScopes() throws AnalysisEngineProcessException {
        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tFEVER3\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tFEVER4\tuima.tcas.Annotation\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tDISORDER1\tuima.tcas.Annotation\tNegation:affirm\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "DISORDER\tDISORDER1\tCOPYALL\tConcept\taffirm,present\tDefaultSection\n" +
                "FEVER\tFEVER3\tCOPYALL\tConcept\taffirm\tDefaultSection\n" +
                "FEVER\tFEVER4\tCOPYALL\tConcept\t\t\n";
        init(ruleStr, "uima.tcas.Annotation");
        String sentence1 = "He had a fever 2 days ago. ";
        String sentence2 = "His brother also had a fever last week.";
        String text = sentence1 + sentence2;
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, sentence1.length() - 1);
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();

        begin = text.indexOf(targetWords, end);
        end = begin + targetWords.length();

        concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER3"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("DISORDER1"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER4"))).size() == 1);
    }


    @Test
    public void testNoteRuleId() throws AnalysisEngineProcessException, ResourceInitializationException {

        String ruleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "&CONCEPT_FEATURES\tFEVER3\tConcept\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tFEVER4\tConcept\tNegation:affirm\tTestFeature:f1\n" +
                "&CONCEPT_FEATURES\tDISORDER1\tConcept\tNegation:affirm\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\n" +
                "@FEATURE_VALUES\tTestFeature\tf1\tf2\n" +
                "DISORDER\tDISORDER1\tCOPYALL\tConcept\taffirm,present\tDefaultSection\n" +
                "FEVER\tFEVER3\tCOPYALL\tConcept\taffirm\tDefaultSection\n" +
                "FEVER\tFEVER4\tCOPYALL\tConcept\t\t\n";
        init(ruleStr, "uima.tcas.Annotation");
        Object[] configurationData = new Object[]{FeatureInferenceAnnotator.PARAM_RULE_STR, ruleStr,
                FeatureInferenceAnnotator.PARAM_REMOVE_EVIDENCE_CONCEPT, true, FeatureInferenceAnnotator.PARAM_NOTE_RULE_ID, true};
        featureInferencer = createEngine(FeatureInferenceAnnotator.class,
                configurationData);
        String sentence1 = "He had a fever 2 days ago. ";
        String sentence2 = "His brother also had a fever last week.";
        String text = sentence1 + sentence2;
        jCas.setDocumentText(text);
        String targetWords = "fever";
        int begin = text.indexOf(targetWords);
        int end = begin + targetWords.length();
        DefaultSection section = new DefaultSection(jCas, 0, sentence1.length() - 1);
        section.addToIndexes();
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();

        begin = text.indexOf(targetWords, end);
        end = begin + targetWords.length();

        concept = new Concept(jCas, begin, end);
        concept.setNegation("affirm");
        concept.setCertainty("certain");
        concept.setTemporality("present");
        concept.addToIndexes();
        featureInferencer.process(jCas);
        if (printAnnos)
            printer.process(jCas);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER3"))).size() == 1);
        Concept con = (Concept) JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER3"))).iterator().next();
        assert (con.getNote().equals("	MachedId:	12"));
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("DISORDER1"))).size() == 1);
        con = (Concept) JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("DISORDER1"))).iterator().next();
        assert (con.getNote().equals("	MachedId:	11"));
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER4"))).size() == 1);
        con = (Concept) JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("FEVER4"))).iterator().next();
        assert (con.getNote().equals("	MachedId:	13"));
    }

    @Test
    public void testDigitConvert() {
        assert (10.5 == concatenateIntegerToDouble(10, 5));
        assert (1.15 == concatenateIntegerToDouble(1, 15));
        assert (132.145 == concatenateIntegerToDouble(132, 145));
        assert (100.11345 == concatenateIntegerToDouble(100, 11345));
    }

    @Test
    public void testArrayHash() {
        HashMap<Integer[], String> map = new HashMap<>();
        map.put(new Integer[]{1, 2}, "1,2");
        map.put(new Integer[]{100, 200}, "100,200");
        map.put(new Integer[]{13, 32}, "13,32");
        map.put(new Integer[]{31, 52}, "31,52");
        System.out.println(map.get(new Integer[]{100, 200}));
        HashMap<List<Integer>, String> map2 = new HashMap<>();
        map2.put(Arrays.asList(new Integer[]{1, 2}), "1,2");
        System.out.println(map2.get(Arrays.asList(new Integer[]{1, 2})));
    }

    @Test
    public void testHashSpeed() {
        int k = 1000000;
        long begin = System.nanoTime();
        int code = hashCode(new int[]{32, 56});
        for (int i = 0; i < k; i++) {
            if (hashCode(new int[]{32, 56}) != code)
                System.out.println("false");
        }
        long end = System.nanoTime();
        System.out.println("Hashing time" + (end - begin) / 1000.0);

        begin = System.nanoTime();
        for (int i = 0; i < k; i++) {
            concatenateIntegerToDouble(32, 56);
        }
        end = System.nanoTime();
        System.out.println("convert time" + (end - begin) / 1000.0);

        begin = System.nanoTime();
        for (int i = 0; i < k; i++) {
            concatenateIntegerToDoubleLocal(32, 56);
        }
        end = System.nanoTime();
        System.out.println("convert time" + (end - begin) / 1000.0);

    }

    public static int hashCode(int[] a) {
        return (a[0] + 31) * 31 + a[1];
    }

    public static double concatenateIntegerToDoubleLocal(int begin, int end) {
        int count = countNumberOfDigits(end);
        double denominator = Math.pow(10, count);
        double res = begin + (1.0 * end / denominator);
        return res;
    }
}
