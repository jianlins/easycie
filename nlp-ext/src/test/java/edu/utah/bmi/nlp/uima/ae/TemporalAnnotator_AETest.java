package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastner.uima.FastNER_AE_General;
import edu.utah.bmi.nlp.rush.uima.RuSH_AE;
import edu.utah.bmi.nlp.sectiondectector.SectionDetectorR_AE;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TemporalAnnotator_AETest {
    protected AnalysisEngine sectionDetector, sentenceSegmentor;
    protected AdaptableUIMACPERunner runner;
    protected JCas jCas;
    protected CAS cas;
    protected AnalysisEngine temporalAnnotatorAE, nerAE, cnerTestAe, ParagraphDetectorAE;

    @BeforeAll
    public static void clearTargetDir() {
        try {
            FileUtils.deleteDirectory(new File("target/target/generated-test-classes"));
            FileUtils.deleteDirectory(new File("target/generated-test-sources"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getRules(String ruleFileURI){
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
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-classes");
        Collection<TypeDefinition> types = new FastNER_AE_General().getTypeDefs(ruleStrs[0]).values();
        runner.addConceptTypes(types);
        types=new SectionDetectorR_AE().getTypeDefs(sectionRule).values();
        runner.addConceptTypes(types);
        types=new TemporalAnnotator_AE().getTypeDefs(ruleStrs[1]).values();
        runner.addConceptTypes(types);

        runner.reInitTypeSystem("target/generated-test-sources/customized", "target/generated-test-sources/");
        sectionDetector = AnalysisEngineFactory.createEngine(SectionDetectorR_AE.class, SectionDetectorR_AE.PARAM_RULE_STR, sectionRule);
        sentenceSegmentor = AnalysisEngineFactory.createEngine(RuSH_AE.class, RuSH_AE.PARAM_RULE_STR, rushRule,
                RuSH_AE.PARAM_TOKEN_TYPE_NAME,"Token",RuSH_AE.PARAM_INCLUDE_PUNCTUATION,true);
        ParagraphDetectorAE =AnalysisEngineFactory.createEngine(ParagraphDetector.class);
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
        ParagraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        System.out.println(JCasUtil.select(jCas, Sentence.class).size());
        temporalAnnotatorAE.process(jCas);
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 1);
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }

    @Test
    void test1b() throws ResourceInitializationException, AnalysisEngineProcessException {
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
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION");
        jCas = addMeta(inputText, recordDate, referenceDate);
        ParagraphDetectorAE.process(jCas);
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        System.out.println(JCasUtil.select(jCas, Sentence.class).size());
        temporalAnnotatorAE.process(jCas);
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 2);
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }

    @Test
    void test2() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection two weeks ago. \n";
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
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Date.class));
        System.out.println(JCasUtil.select(jCas, Date.class).size());
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 1);
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        assertTrue(anno.getNormDate().startsWith("2015-01-06T00:00:00"));
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }


    @Test
    void test3() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection 1/7.2 \n";
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
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Date.class));
//        assertTrue(JCasUtil.select(jCas, Date.class).size() == 1);
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }

    @Test
    void test4() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Soft tissue infection the day before yesterday \n";
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
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Date.class));
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 1);
        Date anno = JCasUtil.select(jCas, Date.class).iterator().next();
        assertTrue(anno.getNormDate().startsWith("2015-01-18T00:00:00"));
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }


    @Test
    void test5() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText=
                "HPI:\n" +
                        "Operation done 06/17/16 \n";
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
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 1);
    }

    @Test
    void test6() throws ResourceInitializationException, AnalysisEngineProcessException {
        String inputText = "Record Date 1/27/2015\n" +
                "HPI:\n" +
                "Pt 1/18/2015 admitted. Soft tissue infection today. \n";
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
        sectionDetector.process(jCas);
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Date.class));
        assertTrue(JCasUtil.select(jCas, Date.class).size() == 2);
        Date anno = (Date) JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("ABS_DATE"))).iterator().next();
        assertTrue(anno.getNormDate().startsWith("2015-01-18T00:00:00.000"));
        anno = (Date) JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("REL_DATE"))).iterator().next();
        assertTrue(anno.getNormDate().startsWith("2015-01-20T00:00:00.000"));
        System.out.println(inputText.substring(anno.getBegin(), anno.getEnd()));
    }

    @Test
    void test7() throws ResourceInitializationException, AnalysisEngineProcessException, NoSuchMethodException {
        String inputText = "End colostomy on 8/8. On POD2, he developed infection.";
        String recordDate = "08/23/2010", referenceDate = "08/08/2010";

        String nerRule = "@fastner\n" +
                "@CONCEPT_FEATURES\tINFECTION\tConcept\n" +
                "infection\tINFECTION";
        String tempRuleStr = "@fastner\n" +
                "##############\n" +
                "@CONCEPT_FEATURES\tABS_DATE\tConcept\n" +
                "@CONCEPT_FEATURES\tREL_DATE\tConcept\tElapse\tNumType\n" +
                "\n" +
                "\n" +
                "@CATEGORY_VALUES\thistorical\t-168\n" +
                "@CATEGORY_VALUES\tpatos\t23\n" +
                "# reported infection on day 32 stating infection happened 2 days ago.\n" +
                "@CATEGORY_VALUES\tpresent\t744\n" +
                "@CATEGORY_VALUES\tafter30d\t10000000\t\t\n" +
                "\\< 13 / \\< 32\t1\tABS_DATE\n" +
                "POD \\( \\< 10\t1\tREL_DATE\tACTUAL\t-1\td";

        init(new String[]{nerRule, tempRuleStr});

        nerAE = AnalysisEngineFactory.createEngine(FastNER_AE_General.class, FastNER_AE_General.PARAM_RULE_STR, nerRule);

        temporalAnnotatorAE = AnalysisEngineFactory.createEngine(TemporalAnnotator_AE.class,
                TemporalAnnotator_AE.PARAM_RULE_STR, tempRuleStr,
                TemporalAnnotator_AE.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TemporalAnnotator_AE.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TemporalAnnotator_AE.PARAM_INCLUDE_SECTIONS, "DefaultSection",
                TemporalAnnotator_AE.PARAM_AROUND_CONCEPTS, "INFECTION",
                TemporalAnnotator_AE.PARAM_AROUND_BOUNDARY,"Sentence");
        jCas = addMeta(inputText, recordDate, referenceDate);
        DefaultSection section = new DefaultSection(jCas, 0, inputText.length());
        section.addToIndexes();
        Paragraph paragraph=new Paragraph(jCas, 0, inputText.length());
        paragraph.addToIndexes();
        sentenceSegmentor.process(jCas);
        nerAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, DefaultSection.class));
        System.out.println(JCasUtil.select(jCas, Concept.class));
        temporalAnnotatorAE.process(jCas);
        System.out.println(JCasUtil.select(jCas, Date.class));
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