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
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.Doc_Base;
import edu.utah.bmi.nlp.type.system.EntityBASE;
import edu.utah.bmi.nlp.type.system.Sentence;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * @author Jianlin Shi on 5/17/17.
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class DocInferenceAnnotatorTest {
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine featureInferencer, docInferencer, printer;
    private String typeDescriptor;
    private String docRuleStr, docRuleStr2, featureRuleStr;

    @BeforeEach
    public void init() {
        typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        MemoryClassLoader.CURRENT_LOADER_NAME = DeterminantValueSet.randomString();
        featureRuleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\t\n" +
                "@casesensitive:\ttrue\n" +
                "@Encephalopathy\tConcept\tNegation:affirmed\tCertainty:certain\tTemporality:present\n" +
                "@Negated_Encephalopathy\tConcept\tCertainty:certain\tTemporality:present\n" +
                "@Possible_Encephalopathy\tConcept\tNegation:affirmed\tCertainty:certain\tTemporality:present\n" +
                "@Historical_Encephalopathy\tConcept\tCertainty:certain\tTemporality:historical\n" +
                "Encephalopathy\tNegation:affirmed,Certainty,Temporality\tConcept\tNegation:affirmed,Certainty:certain,Temporality:present\tDocumentAnnotation\n" +
                "Negated_Encephalopathy\tNegation:negated,Certainty,Temporality\tConcept\tNegation:negated\tDocumentAnnotation\n" +
                "Possible_Encephalopathy\tNegation,Certainty,Temporality\tConcept\tNegation:affirmed,Certainty:uncertain,Temporality:present\tDocumentAnnotation\n" +
                "Historical_Encephalopathy\tNegation,Certainty,Temporality\tConcept\tNegation:affirmed,Temporality:historical\tDocumentAnnotation\n";


        docRuleStr = "EP_Doc\tEncephalopathy_Doc	Certainty:Encephalopathy,Negation:Encephalopathy,Temporality:Encephalopathy	Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc	Certainty:Possible_Encephalopathy,Negation:Possible_Encephalopathy,Temporality:Possible_Encephalopathy	Possible_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc	Certainty:Negated_Encephalopathy	Negated_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tRec_Encephalopathy_Doc	Certainty:Negated_Encephalopathy	Negated_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc\tCertainty:Historical_Encephalopathy	Historical_Encephalopathy\tSourceDocumentInformation";

        docRuleStr2 = "@aggregate:false\n" +
                "@EP_Doc\tNeg_Encephalopathy_Doc2\n" +
                "@CONCEPT_FEATURES\tEncephalopathy_Doc2\tDoc_Base\tCertainty:certain\tTemporality:present\n" +
                "@CONCEPT_FEATURES\tNeg_Encephalopathy_Doc2\tDoc_Base\tCertainty:certain\tTemporality:present\n" +
                "@CONCEPT_FEATURES\tPossible_Encephalopathy_Doc2\tDoc_Base\tNegation:unknown\tCertainty:certain\tTemporality:present\n" +
                "EP_Doc\tEncephalopathy_Doc2	Certainty:Encephalopathy,Negation:Encephalopathy,Temporality:Encephalopathy	Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc2	Certainty,Negation,Temporality	Possible_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc2	Certainty:Negated_Encephalopathy	Negated_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc2\tCertainty:Historical_Encephalopathy,Temporality	Historical_Encephalopathy\tSourceDocumentInformation";
//        System.out.println(featureRuleStr);

//        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");

    }

    @AfterAll
    public static void resetLoader() {
        MemoryClassLoader.resetLoaderName();
    }

    private void init(String featureRuleStr, String docRuleStr, String printTypeName) {

        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new FeatureInferenceAnnotator().getTypeDefs(featureRuleStr);
        runner.addConceptTypes(typeDefinitions.values());
        typeDefinitions = new DocInferenceAnnotator().getTypeDefs(docRuleStr);
        runner.addConceptTypes(typeDefinitions.values());
        runner.reInitTypeSystem("target/generated-test-sources/customized.xml");
        jCas = runner.initJCas();
        try {
            featureInferencer = createEngine(FeatureInferenceAnnotator.class,
                    FeatureInferenceAnnotator.PARAM_RULE_STR, featureRuleStr);
            docInferencer = createEngine(DocInferenceAnnotator.class,
                    DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);
            printer = createEngine(AnnotationPrinter.class,
                    AnnotationPrinter.PARAM_INDICATION, "",
                    AnnotationPrinter.PARAM_TYPE_NAME,
                    printTypeName);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

    }

    private void addConcept(String text, String targetWord, String negation, String certainty, String temporality) {
        int begin = text.indexOf(targetWord);
        int end = begin + targetWord.length();
        addConcept(begin, end, negation, certainty, temporality);
    }

    private void addConcept(int begin, int end, String negation, String certainty, String temporality) {
        Concept concept = new Concept(jCas, begin, end);
        concept.setNegation(negation);
        concept.setCertainty(certainty);
        concept.setTemporality(temporality);
        concept.addToIndexes();
    }

    @Test
    public void test1() throws AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        addConcept(text, "month", "affirmed", "certain", "historical");

        featureInferencer.process(jCas);
        docInferencer.process(jCas);
//        printer.process(jCas);

        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Negated_Encephalopathy"))).size() == 0);
        assert (JCasUtil.select(jCas, Concept.class).iterator().next().getTemporality().equals("present"));
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getTopic().equals("EP_Doc"));
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getNote().equals("Encephalopathy()"));
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getCoveredText().equals("He"));

    }

    @Test
    public void test2() throws AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "negated", "certain", "present");
        addConcept(text, "month", "negated", "certain", "historical");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Negated_Encephalopathy"))).size() == 2);
        assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
//        printer.process(jCas);
    }

    @Test
    public void test3() throws AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        addConcept(text, "month", "affirmed", "uncertain", "present");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc"))).size() == 1);
//        printer.process(jCas);
    }

    @Test
    public void test4() throws AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "negated", "uncertain", "present");
        addConcept(text, "month", "affirmed", "uncertain", "present");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
//        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Negated_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy_Doc"))).size() == 1);

    }

    @Test
    public void test5() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "EP_Doc\tEncephalopathy_Doc		Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc		Negated_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc		Historical_Encephalopathy\tSourceDocumentInformation";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "uncertain", "present");
        addConcept(text, "month", "affirmed", "uncertain", "present");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy"))).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy_Doc"))).size() == 1);
//		assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
//        printer.process(jCas);
    }

    @Test
    public void test6() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc		Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "negated", "uncertain", "present");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Negated_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Neg_Encephalopathy_Doc"))).size() == 1);
//		assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
//        printer.process(jCas);
    }

    /**
     * Test if scope specification applied by the document inferencer
     *
     * @throws AnalysisEngineProcessException
     * @throws ResourceInitializationException
     */
    @Test
    public void testScope() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc		Encephalopathy\tSentence\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        addConcept(50, 56, "affirmed", "uncertain", "present");
        Sentence sentence = new Sentence(jCas, 37, 61);
        sentence.addToIndexes();
        featureInferencer.process(jCas);
        printer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy_Doc"))).size() == 1);
//        printer.process(jCas);
    }


    /**
     * Test if scope window size
     *
     * @throws AnalysisEngineProcessException
     * @throws ResourceInitializationException
     */
    @Test
    public void testWindowSize() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tRec_Encephalopathy_Doc		Encephalopathy,Historical_Encephalopathy\tSentence\t1\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "historical");
        addConcept(50, 56, "affirmed", "certain", "present");
        Sentence sentence = new Sentence(jCas, 0, 37);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 37, 61);
        sentence.addToIndexes();
        featureInferencer.process(jCas);

        docInferencer.process(jCas);
//        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Historical_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Rec_Encephalopathy_Doc"))).size() == 1);
    }

    @Test
    public void testWindowSize2() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tRec_Encephalopathy_Doc		Encephalopathy,Historical_Encephalopathy\tSentence\t0\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "historical");
        addConcept(50, 56, "affirmed", "certain", "present");
        Sentence sentence = new Sentence(jCas, 0, 37);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 37, 61);
        sentence.addToIndexes();
        featureInferencer.process(jCas);

        docInferencer.process(jCas);
        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Historical_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Neg_Encephalopathy_Doc"))).size() == 1);
    }

    @Test
    public void testWindowSize3() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tRec_Encephalopathy_Doc		Encephalopathy,Historical_Encephalopathy\tSentence\t1\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        System.out.println("He had encephalopathy one month ago. He does have some break.".length());

        String text = "He had encephalopathy one month ago. He does have some break. He does have encephs now.";
        System.out.println(text.length());
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "historical");
        addConcept(text, "encephs", "affirmed", "certain", "present");
        Sentence sentence = new Sentence(jCas, 0, 37);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 37, 61);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 62, 87);
        sentence.addToIndexes();
        featureInferencer.process(jCas);

        docInferencer.process(jCas);
//        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Historical_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Neg_Encephalopathy_Doc"))).size() == 1);
    }

    @Test
    public void testWindowSize4() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tRec_Encephalopathy_Doc		Encephalopathy,Historical_Encephalopathy\tSentence\t2\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc		Possible_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        System.out.println("He had encephalopathy one month ago. He does have some break.".length());

        String text = "He had encephalopathy one month ago. He does have some break. He does have encephs now.";
        System.out.println(text.length());
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "historical");
        addConcept(text, "encephs", "affirmed", "certain", "present");
        Sentence sentence = new Sentence(jCas, 0, 37);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 37, 61);
        sentence.addToIndexes();
        sentence = new Sentence(jCas, 62, 87);
        sentence.addToIndexes();
        featureInferencer.process(jCas);

        docInferencer.process(jCas);
//        printer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Historical_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Rec_Encephalopathy_Doc"))).size() == 1);
    }

    /**
     * Test add features to document level annotations
     *
     * @throws AnalysisEngineProcessException
     * @throws ResourceInitializationException
     */
    @Test
    public void testFeatureAssignment() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc	Certainty,Temporality:Encephalopathy\tEncephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        printer.process(jCas);
        featureInferencer.process(jCas);
        printer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc"))).size() == 1);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures().equals("\t\tCertainty:\tcertain\n" +
                "\t\tTemporality:\tpresent"));
//        printer.process(jCas);
    }


    @Test
    public void testFeatureAssignment2() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc	Status:true,State:zero	Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        featureInferencer.process(jCas);
        printer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc"))).size() == 1);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures().equals("\t\tStatus:\ttrue\n" +
                "\t\tState:\tzero"));
//        printer.process(jCas);
    }

    @Test
    public void testDefaultFeatureAssignment() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "@CONCEPT_FEATURES\tNeg_Encephalopathy_Doc\tDoc_Base\tStatus:false\tState:one\n" +
                "EP_Doc\tEncephalopathy_Doc	Status:true,State:zero	Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Neg_Encephalopathy_Doc"))).size() == 1);
        printer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures().equals("\t\tStatus:\tfalse\n" +
                "\t\tState:\tone"));

    }

    /**
     * Use short form to assign feature values for document level annotations
     *
     * @throws AnalysisEngineProcessException
     * @throws ResourceInitializationException
     */
    @Test
    public void testShortFormFeatureAssignment() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc	Certainty,Temporality	Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc"))).size() == 1);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures().equals("\t\tCertainty:\tcertain\n" +
                "\t\tTemporality:\tpresent"));
//        printer.process(jCas);
    }

    @Test
    public void testShortFormFeatureAutoPick() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc	Certainty,Temporality	Encephalopathy,EntityBASE\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "encephalopathy", "affirmed", "certain", "present");
        EntityBASE entity = new EntityBASE(jCas, 2, 3);
        entity.addToIndexes();
        featureInferencer.process(jCas);
        printer.process(jCas);
        docInferencer.process(jCas);
//		assert (JCasUtil.select(jCas, Concept.class).size() == 1);
//		assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NegatedConcept"))).size() == 0);
//		assert (JCasUtil.select(jCas, Concept.class).iterator().next().getNegation().equals("negated"));
        printer.process(jCas);
    }

    @Test
    public void testShortFormFeatureAmbiguous() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc	Negation,Certainty	Possible_Encephalopathy,Negated_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);

        String text = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(text);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, text.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(text, "month", "affirmed", "uncertain", "present");
        addConcept(text, "encephalopathy", "negated", "certain", "present");

        featureInferencer.process(jCas);
//        printer.process(jCas);
        docInferencer.process(jCas);
        assert (JCasUtil.select(jCas, Concept.class).size() == 2);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Possible_Encephalopathy"))).size() == 1);
        assert (JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("Negated_Encephalopathy"))).size() == 1);
//		assert ();
        System.out.println(JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures());
//        printer.process(jCas);

    }

    @Test
    public void testShortFormFeatureError() throws AnalysisEngineProcessException, ResourceInitializationException {
        init(featureRuleStr, docRuleStr, "uima.tcas.Annotation");
        String docRuleStr = "@EP_Doc\tNeg_Encephalopathy_Doc\n" +
                "EP_Doc\tEncephalopathy_Doc\tNegation,Certainty\tEncephalopathy,Negated_Encephalopathy\tSourceDocumentInformation\n";

        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr);


    }

    @Test
    public void testNewRuleFormatWithInitiations() {
        LinkedHashMap<String, TypeDefinition> typeDefs = new DocInferenceAnnotator().getTypeDefs(docRuleStr2);
        assert (typeDefs.size() == 3 && typeDefs.containsKey("Encephalopathy_Doc2"));
        assert (typeDefs.get("Encephalopathy_Doc2").getFeatureValuePairs().containsKey("Certainty"));
        assert (typeDefs.get("Neg_Encephalopathy_Doc2").getFeatureValuePairs().get("Certainty").equals("certain"));
    }

    @Test
    public void testDefaultSeparateFeatureValues() throws ResourceInitializationException, AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr2, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr2);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();

        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        AnalysisEngine annotationEvaluator = createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Neg_Encephalopathy_Doc2",
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Certainty",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "certain");
        annotationEvaluator.process(jCas);
        assert (AnnotationEvaluator.pass);
    }

    @Test
    public void testCopyFeatureValues() throws ResourceInitializationException, AnalysisEngineProcessException {
        String rule = docRuleStr2.replaceAll("false", "true");
        init(featureRuleStr, rule, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, rule, DocInferenceAnnotator.PARAM_ANNO_POSITION, DocInferenceAnnotator.FIRSTEVIDENCE);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        Class<? extends Annotation> aCls = AnnotationOper.getTypeClass("Possible_Encephalopathy_Doc2", Doc_Base.class, true);

        assert (Doc_Base.class.isAssignableFrom(aCls));
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "uncertain", "present");
        featureInferencer.process(jCas);
        printer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        assert (JCasUtil.select(jCas, Doc_Base.class).size() == 1);
        assert (JCasUtil.select(jCas, Doc_Base.class).iterator().next().getFeatures().equals("\t\tCertainty:\tuncertain\n" +
                "\t\tNegation:\taffirmed\n" +
                "\t\tTemporality:\tpresent"));

    }


    @Test
    public void test0() throws ResourceInitializationException, AnalysisEngineProcessException {
        init(featureRuleStr, docRuleStr2, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr2);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "certain", "historical");
        featureInferencer.process(jCas);
        assert (AnnotationOper.classLoaded(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc2")));
        docInferencer.process(jCas);
        printer.process(jCas);
        AnalysisEngine annotationEvaluator = createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Neg_Encephalopathy_Doc2",
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Temporality",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "historical");
        annotationEvaluator.process(jCas);
        assert (AnnotationEvaluator.pass);
    }


    @Test
    public void testSeparateFeatureValues2() throws ResourceInitializationException, AnalysisEngineProcessException {
        String docRuleStr3 = "@aggregate:false\n" +
                "@EP_Doc\tNeg_Encephalopathy_Doc2\n" +
                "@CONCEPT_FEATURES\tEncephalopathy_Doc2\tEntityBASE\tCertainty:certain\tTemporality:present\n" +
                "@CONCEPT_FEATURES\tNeg_Encephalopathy_Doc2\tEntityBASE\tCertainty:certain\tTemporality:present\n" +
                "@CONCEPT_FEATURES\tPossible_Encephalopathy_Doc2\tEntityBASE\tNegation:unknown\tCertainty:certain\tTemporality:present\n" +
                "EP_Doc\tEncephalopathy_Doc2	Certainty:Encephalopathy,Negation:Encephalopathy,Temporality:Encephalopathy	Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tPossible_Encephalopathy_Doc2	Certainty,Negation,Temporality	Possible_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc2	Certainty:Negated_Encephalopathy	Negated_Encephalopathy\tSourceDocumentInformation\n" +
                "EP_Doc\tNeg_Encephalopathy_Doc2\tCertainty:Historical_Encephalopathy,Temporality	Historical_Encephalopathy\tSourceDocumentInformation";
        init(featureRuleStr, docRuleStr3, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr3);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "certain", "historical");
        featureInferencer.process(jCas);
        assert (AnnotationOper.classLoaded(DeterminantValueSet.checkNameSpace("Encephalopathy_Doc2")));
        docInferencer.process(jCas);
        printer.process(jCas);
        AnalysisEngine annotationEvaluator = createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Neg_Encephalopathy_Doc2",
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Temporality",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "historical");
        annotationEvaluator.process(jCas);
        assert (AnnotationEvaluator.pass);
    }

    @Test
    public void testAggregateFeatureValuesFromRuleSetting() throws ResourceInitializationException, AnalysisEngineProcessException {
        docRuleStr2 = docRuleStr2.replaceAll("false", "true");
        docRuleStr2 = docRuleStr2.replaceAll("2", "3");
        init(featureRuleStr, docRuleStr2, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr2);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "certain", "historical");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        AnalysisEngine annotationEvaluator = createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Neg_Encephalopathy_Doc3",
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Features",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "\t\tCertainty:\tcertain\n" +
                        "\t\tTemporality:\thistorical");
        annotationEvaluator.process(jCas);
        assert (AnnotationEvaluator.pass);
    }


    @Test
    public void testCreateAnnotationINSITUEvidence() throws ResourceInitializationException, AnalysisEngineProcessException {
        docRuleStr2 = docRuleStr2.replaceAll("false", "true");
        docRuleStr2 = docRuleStr2.replaceAll("2", "3");
        init(featureRuleStr, docRuleStr2, "uima.tcas.Annotation");
        docInferencer = createEngine(DocInferenceAnnotator.class,
                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr2,
                DocInferenceAnnotator.PARAM_ANNO_POSITION, DocInferenceAnnotator.FIRSTEVIDENCE);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "certain", "historical");
        featureInferencer.process(jCas);
        docInferencer.process(jCas);
        printer.process(jCas);
        AnalysisEngine annotationEvaluator = createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Neg_Encephalopathy_Doc3",
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Features",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "\t\tCertainty:\tcertain\n" +
                        "\t\tTemporality:\thistorical");
        annotationEvaluator.process(jCas);
        assert (AnnotationEvaluator.pass);
    }


    @Test
    public void testFeatureOptions() throws ResourceInitializationException, AnalysisEngineProcessException {
        String featureRuleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "@CONCEPT_FEATURES\tMerged_PE\tConcept\tSection:DocumentAnnotation\tCertainty:certain\tExperiencer:patient\tAcute:acute\n" +
                "@CONCEPT_FEATURES\tMerged_Uncertain_PE\tConcept\tSection:DocumentAnnotation\tCertainty:uncertain\tExperiencer:patient\tAcute:acute\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\tpatos\tplan\thypothetical\tin30d\tdayone\tafter30d\tcurrent\n" +
                "@FEATURE_VALUES\tExperiencer\tpatient\tnonpatient\n" +
                "@FEATURE_VALUES\tAcute\tacute\tchronic\n" +
                "PE\tMerged_PE\tNegation:affirmed,Certainty,Temporality,Acute:chronic\tConcept\tcertain\tDocumentAnnotation\n" +
                "PE\tMerged_Uncertain_PE\tNegation:negated,Certainty,Temporality\tConcept\tuncertain\tDocumentAnnotation\n";

        String docRuleStr3 = "@splitter:\n" +
                "&DEFAULT_DOC_TYPE\tPE_Doc\tNeg_Pulmonary_Embolism_Doc\n" +
                "&CONCEPT_FEATURES\tNeg_Pulmonary_Embolism_Doc\tDoc_Base\n" +
                "&CONCEPT_FEATURES\tPulmonary_Embolism_Doc\tDoc_Base\n" +
                "\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\thypothetical\tcurrent\n" +
                "@FEATURE_VALUES\tExperiencer\tpatient\tnonpatient\n" +
                "PE_Doc\tPulmonary_Embolism_Doc\tCertainty:certain,ImpressionCertainty:imuncertain,Temporality:$Merged_PE,Acute:$Merged_PE,Chronic:$Merged_PE,ArteryMain:$Merged_PE,Left:$Merged_PE,Right:$Merged_PE,ArteryInterlobar:$Merged_PE,ArteryLobar:$Merged_PE,ArterySegmental:$Merged_PE,ArterySubSegmental:$Merged_PE\tMerged_PE,Merged_Uncertain_PE\t\n" +
                "\n";
        init(featureRuleStr, docRuleStr3, "uima.tcas.Annotation");
//        docInferencer = createEngine(DocInferenceAnnotator.class,
//                DocInferenceAnnotator.PARAM_RULE_STR, docRuleStr3,
//                DocInferenceAnnotator.PARAM_ANNO_POSITION, DocInferenceAnnotator.FIRSTEVIDENCE);

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "uncertain", "current");
        addConcept(inputText, "enceph", "affirmed", "certain", "current");
        featureInferencer.process(jCas);
        Collection<Concept> annos = JCasUtil.select(jCas, Concept.class);
        for (Concept c : annos) {
            System.out.println(c.toString().replaceAll("\n", " "));
        }
        docInferencer.process(jCas);
        printer.process(jCas);
        Collection<? extends Annotation> docAnnos = JCasUtil.select(jCas, AnnotationOper.getTypeClass("Pulmonary_Embolism_Doc"));
        assert(docAnnos.size()==1);
        Annotation docAnno = docAnnos.iterator().next();
        String features= (String) AnnotationOper.getFeatureValue("Features", docAnno);
        assert(features.contains("chronic"));
        assert(features.contains("current"));
        assert(features.contains("imuncertain"));
    }


    @Test
    public void testFeatureOptions2() throws ResourceInitializationException, AnalysisEngineProcessException {
        String featureRuleStr = "@processer:\tfeatureinferencer\n" +
                "@splitter:\n" +
                "@version:2\n" +
                "@casesensitive:\ttrue\n" +
                "@CONCEPT_FEATURES\tMerged_PE\tConcept\tSection:DocumentAnnotation\tCertainty:certain\tExperiencer:patient\tAcute:acute\n" +
                "@CONCEPT_FEATURES\tMerged_Uncertain_PE\tConcept\tSection:DocumentAnnotation\tCertainty:uncertain\tExperiencer:patient\tAcute:acute\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\tpatos\tplan\thypothetical\tin30d\tdayone\tafter30d\tcurrent\n" +
                "@FEATURE_VALUES\tExperiencer\tpatient\tnonpatient\n" +
                "@FEATURE_VALUES\tAcute\tacute\tchronic\n" +
                "PE\tMerged_PE\tNegation:affirmed,Certainty,Temporality,Acute:chronic\tConcept\tcertain\tDocumentAnnotation\n" +
                "PE\tMerged_Uncertain_PE\tNegation:negated,Certainty,Temporality\tConcept\tuncertain\tDocumentAnnotation\n";

        String docRuleStr3 = "@splitter:\n" +
                "@aggregate:false\n"+
                "&DEFAULT_DOC_TYPE\tPE_Doc\tNeg_Pulmonary_Embolism_Doc\n" +
                "&CONCEPT_FEATURES\tNeg_Pulmonary_Embolism_Doc\tDoc_Base\n" +
                "&CONCEPT_FEATURES\tPulmonary_Embolism_Doc\tDoc_Base\tCertainty:certain\tTemporality:current\tExperiencer:patient\tAcute:acute\tImpressionCertainty:imcertain\n" +
                "\n" +
                "@FEATURE_VALUES\tNegation\taffirm\tnegated\n" +
                "@FEATURE_VALUES\tCertainty\tcertain\tuncertain\n" +
                "@FEATURE_VALUES\tTemporality\tpresent\thistorical\thypothetical\tcurrent\n" +
                "@FEATURE_VALUES\tExperiencer\tpatient\tnonpatient\n" +
                "PE_Doc\tPulmonary_Embolism_Doc\tCertainty:certain,ImpressionCertainty:imuncertain,Temporality:$Merged_PE,Acute:$Merged_PE\tMerged_PE,Merged_Uncertain_PE\t\n" +
                "\n";
        init(featureRuleStr, docRuleStr3, "uima.tcas.Annotation");

        String inputText = "He had encephalopathy one month ago. He does have enceph now.";
        jCas.setDocumentText(inputText);
        SourceDocumentInformation sourceDocumentInformation = new SourceDocumentInformation(jCas, 0, inputText.length());
        sourceDocumentInformation.addToIndexes();
        addConcept(inputText, "encephalopathy", "affirmed", "uncertain", "current");
        addConcept(inputText, "enceph", "affirmed", "certain", "current");
        featureInferencer.process(jCas);
        Collection<Concept> annos = JCasUtil.select(jCas, Concept.class);
        for (Concept c : annos) {
            System.out.println(c.toString().replaceAll("\n", " "));
        }
        docInferencer.process(jCas);
        Collection<? extends Annotation> docAnnos = JCasUtil.select(jCas, AnnotationOper.getTypeClass("Pulmonary_Embolism_Doc"));
        assert(docAnnos.size()==1);
        Annotation docAnno = docAnnos.iterator().next();
        System.out.println(docAnno);
        assert (FSUtil.getFeature(docAnno, "Acute", String.class).equals("chronic"));
        assert (FSUtil.getFeature(docAnno, "ImpressionCertainty", String.class).equals("imuncertain"));
        assert (FSUtil.getFeature(docAnno, "Experiencer", String.class).equals("patient"));
    }
}