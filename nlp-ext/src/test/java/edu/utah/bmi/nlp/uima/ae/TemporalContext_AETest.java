package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastner.uima.FastNER_AE_General;
import edu.utah.bmi.nlp.rush.uima.RuSH_AE;
import edu.utah.bmi.nlp.sectiondectector.SectionDetectorR_AE;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TemporalContext_AETest {
    protected AnalysisEngine sectionDetector, sentenceSegmentor;
    protected AdaptableUIMACPERunner runner;
    protected JCas jCas;
    protected CAS cas;
    protected AnalysisEngine temporalAnnotatorAE, nerAE, cnerTestAe, paragraphDetectorAE, temporalContextAE;

    @BeforeAll
    public static void clearTargetDir() {
        try {
            FileUtils.deleteDirectory(new File("target/target/generated-test-classes"));
            FileUtils.deleteDirectory(new File("target/generated-test-sources"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    protected String getRules(String ruleFileURI){
        String ruleStr="";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(ruleFileURI)) {
            if (inputStream == null) {
                fail("Rule file not found: " + ruleFileURI);
            }
            ruleStr = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ruleStr;
    }


    protected void init(String[] ruleStrs) throws ResourceInitializationException {

        String typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        String sectionRule = getRules("edu/utah/bmi/nlp/uima/ae/00_Section_Detector.tsv");
        String rushRule = getRules("edu/utah/bmi/nlp/uima/ae/10_RuSH_AE.tsv");
        String tempContextRule=getRules("edu/utah/bmi/nlp/uima/ae/50_TemporalContext_AE.tsv");
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-classes");
        Collection<TypeDefinition> types = new FastNER_AE_General().getTypeDefs(ruleStrs[0]).values();
        runner.addConceptTypes(types);
        types=new SectionDetectorR_AE().getTypeDefs(sectionRule).values();
        runner.addConceptTypes(types);
        types=new TemporalAnnotator_AE().getTypeDefs(ruleStrs[1]).values();
        runner.addConceptTypes(types);
        types=new TemporalContext_AE().getTypeDefs(tempContextRule).values();
        runner.addConceptTypes(types);

        runner.reInitTypeSystem("target/generated-test-sources/customized", "target/generated-test-sources/");
        sectionDetector = AnalysisEngineFactory.createEngine(SectionDetectorR_AE.class, SectionDetectorR_AE.PARAM_RULE_STR, sectionRule);
        sentenceSegmentor = AnalysisEngineFactory.createEngine(RuSH_AE.class, RuSH_AE.PARAM_RULE_STR, rushRule,
                RuSH_AE.PARAM_TOKEN_TYPE_NAME,"Token",RuSH_AE.PARAM_INCLUDE_PUNCTUATION,true);
        paragraphDetectorAE =AnalysisEngineFactory.createEngine(ParagraphDetector.class);
        temporalContextAE=AnalysisEngineFactory.createEngine(TemporalContext_AE.class, TemporalContext_AE.PARAM_RULE_STR, tempContextRule);
    }

    @Test
    void test1() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection 1/19/2015 \n";
        String recordDate = "01/20/2015", referenceDate = "01/02/2015";

        String nerRule = "@fastner\n" +
                "@CONCEPT_FEATURES\tINFECTION\tConcept\n" +
                "infection\tINFECTION";
        String tempRuleStr = getRules("edu/utah/bmi/nlp/uima/ae/46_TemporalAnnotator_AE.tsv");
        

        init(new String[]{nerRule, tempRuleStr});

        nerAE = AnalysisEngineFactory.createEngine(FastNER_AE_General.class, FastNER_AE_General.PARAM_RULE_STR, nerRule);

        temporalAnnotatorAE = AnalysisEngineFactory.createEngine(TemporalAnnotator_AE.class,
                TemporalAnnotator_AE.PARAM_RULE_STR, tempRuleStr,
                TemporalAnnotator_AE.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TemporalAnnotator_AE.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TemporalAnnotator_AE.PARAM_INCLUDE_SECTIONS, "PresentHistory",
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION",
                TemporalAnnotator_AE.PARAM_AROUND_BOUNDARY,"Sentence");
        jCas = addMeta(inputText, recordDate, referenceDate);
        paragraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);

        temporalAnnotatorAE.process(jCas);
        temporalContextAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        assertTrue(JCasUtil.select(jCas, Concept.class).size() == 2);
        assertTrue(JCasUtil.select(jCas, Date.class).size() ==1);
        Iterator<Concept>conceptIterator=JCasUtil.select(jCas, Concept.class).iterator();
        conceptIterator.next();
        Concept dateConcept = conceptIterator.next();
//        System.out.println(dateConcept.getTemporality());
        assertTrue(JCasUtil.select(jCas, Concept.class).iterator().next().getTemporality().equals("in30d"));
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }

    @Test
    void test2() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection 1/19/2015 \n";
        String recordDate = "01/20/2015", referenceDate = "01/18/2015";

        String nerRule = "@fastner\n" +
                "@CONCEPT_FEATURES\tINFECTION\tConcept\n" +
                "infection\tINFECTION";
        String tempRuleStr = getRules("edu/utah/bmi/nlp/uima/ae/46_TemporalAnnotator_AE.tsv");

        init(new String[]{nerRule, tempRuleStr});

        nerAE = AnalysisEngineFactory.createEngine(FastNER_AE_General.class, FastNER_AE_General.PARAM_RULE_STR, nerRule);

        temporalAnnotatorAE = AnalysisEngineFactory.createEngine(TemporalAnnotator_AE.class,
                TemporalAnnotator_AE.PARAM_RULE_STR, tempRuleStr,
                TemporalAnnotator_AE.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TemporalAnnotator_AE.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TemporalAnnotator_AE.PARAM_INCLUDE_SECTIONS, "PresentHistory",
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION",
                TemporalAnnotator_AE.PARAM_AROUND_BOUNDARY,"Sentence");
        jCas = addMeta(inputText, recordDate, referenceDate);
        paragraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);

        temporalAnnotatorAE.process(jCas);
        temporalContextAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        assertTrue(JCasUtil.select(jCas, Concept.class).size() == 2);
        assertTrue(JCasUtil.select(jCas, Date.class).size() ==1);
        Iterator<Concept>conceptIterator=JCasUtil.select(jCas, Concept.class).iterator();
        conceptIterator.next();
        Concept dateConcept = conceptIterator.next();
//        System.out.println(dateConcept.getTemporality());
        assertTrue(JCasUtil.select(jCas, Concept.class).iterator().next().getTemporality().equals("dayone"));
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
    }

    @Test
    void test3() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection 1/14/2015 \n";
        String recordDate = "01/20/2015", referenceDate = "01/18/2015";

        String nerRule = "@fastner\n" +
                "@CONCEPT_FEATURES\tINFECTION\tConcept\n" +
                "infection\tINFECTION";
        String tempRuleStr = getRules("edu/utah/bmi/nlp/uima/ae/46_TemporalAnnotator_AE.tsv");

        init(new String[]{nerRule, tempRuleStr});

        nerAE = AnalysisEngineFactory.createEngine(FastNER_AE_General.class, FastNER_AE_General.PARAM_RULE_STR, nerRule);

        temporalAnnotatorAE = AnalysisEngineFactory.createEngine(TemporalAnnotator_AE.class,
                TemporalAnnotator_AE.PARAM_RULE_STR, tempRuleStr,
                TemporalAnnotator_AE.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TemporalAnnotator_AE.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TemporalAnnotator_AE.PARAM_INCLUDE_SECTIONS, "PresentHistory",
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION",
                TemporalAnnotator_AE.PARAM_AROUND_BOUNDARY,"Sentence");
        jCas = addMeta(inputText, recordDate, referenceDate);
        paragraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);

        temporalAnnotatorAE.process(jCas);
        temporalContextAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        assertTrue(JCasUtil.select(jCas, Concept.class).size() == 2);
        assertTrue(JCasUtil.select(jCas, Date.class).size() ==1);
        Iterator<Concept>conceptIterator=JCasUtil.select(jCas, Concept.class).iterator();
        conceptIterator.next();
        Concept dateConcept = conceptIterator.next();
//        System.out.println(dateConcept.getTemporality());
        assertTrue(JCasUtil.select(jCas, Concept.class).iterator().next().getTemporality().equals("historical"));
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
    }


    @Test
    void test4() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection after two months \n";
        String recordDate = "01/20/2015", referenceDate = "01/18/2015";

        String nerRule = "@fastner\n" +
                "@CONCEPT_FEATURES\tINFECTION\tConcept\n" +
                "infection\tINFECTION";
        String tempRuleStr = getRules("edu/utah/bmi/nlp/uima/ae/46_TemporalAnnotator_AE.tsv");

        init(new String[]{nerRule, tempRuleStr});

        nerAE = AnalysisEngineFactory.createEngine(FastNER_AE_General.class, FastNER_AE_General.PARAM_RULE_STR, nerRule);

        temporalAnnotatorAE = AnalysisEngineFactory.createEngine(TemporalAnnotator_AE.class,
                TemporalAnnotator_AE.PARAM_RULE_STR, tempRuleStr,
                TemporalAnnotator_AE.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TemporalAnnotator_AE.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TemporalAnnotator_AE.PARAM_INCLUDE_SECTIONS, "PresentHistory",
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION",
                TemporalAnnotator_AE.PARAM_AROUND_BOUNDARY,"Sentence");
        jCas = addMeta(inputText, recordDate, referenceDate);
        paragraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);

        temporalAnnotatorAE.process(jCas);
        temporalContextAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        assertTrue(JCasUtil.select(jCas, Concept.class).size() == 2);
        assertTrue(JCasUtil.select(jCas, Date.class).size() ==1);
        Iterator<Concept>conceptIterator=JCasUtil.select(jCas, Concept.class).iterator();
        conceptIterator.next();
        Concept dateConcept = conceptIterator.next();
//        System.out.println(dateConcept.getTemporality());
        assertTrue(JCasUtil.select(jCas, Concept.class).iterator().next().getTemporality().equals("after30d"));
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
    }

    protected JCas addMeta(String text, String recordDate, String referenceDate) {
        JCas jCas = runner.initJCas();
        jCas.setDocumentText(text);
        RecordRow recordRow = new RecordRow().addCell("DATE", recordDate).addCell("REF_DTM", referenceDate);
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas, 0, jCas.getDocumentText().length());
        srcDocInfo.setUri(recordRow.serialize());
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize(jCas.getDocumentText().length());
        srcDocInfo.addToIndexes();
        return jCas;
    }


}