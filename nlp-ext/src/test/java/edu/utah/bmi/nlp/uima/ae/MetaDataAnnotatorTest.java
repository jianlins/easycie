package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

public class MetaDataAnnotatorTest {

    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine metaAnnotator, printer;
    private String typeDescriptor;

    @BeforeEach
    public void init() {
        typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        try {
            printer = createEngine(AnnotationPrinter.class,
                    AnnotationPrinter.PARAM_INDICATION, "",
                    AnnotationPrinter.PARAM_TYPE_NAME,
                    "uima.tcas.Annotation");

        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }

        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");

    }

    private void process(String metaRuleStr, String docText, String... keyValuePairs) {
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new MetaDataAnnotator().getTypeDefs(metaRuleStr);
        runner.addConceptTypes(typeDefinitions.values());
        runner.reInitTypeSystem("target/generated-test-sources/customized.xml");
        jCas = runner.initJCas();
        jCas.setDocumentText(docText);
        try {
            metaAnnotator = createEngine(MetaDataAnnotator.class, MetaDataAnnotator.PARAM_RULE_STR, metaRuleStr);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
        try {
            new Token(jCas, 0, 3).addToIndexes();
            RecordRow recordRow = new RecordRow().addCells(keyValuePairs);
            SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas, 0, docText.length());
            srcDocInfo.setUri(recordRow.serialize());
            srcDocInfo.setOffsetInSource(0);
            srcDocInfo.setDocumentSize(docText.length());
            srcDocInfo.addToIndexes();
            metaAnnotator.process(jCas);
            printer.process(jCas);
        } catch (AnalysisEngineProcessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void parseRuleStr() {
        ArrayList<ArrayList<Object>> res = new MetaDataAnnotator().parseRuleStr("&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDIFF_DTM\t<25 >30\n" +
                "WITHIN24H\tDIFF_DTM\t=23");
        System.out.println(res);
    }

    @Test
    public void testTypeDefinition() {
        LinkedHashMap<String, TypeDefinition> typeDefs = new MetaDataAnnotator().getTypeDefs("&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDIFF_DTM\t<25\n" +
                "AFTER24H\tDIFF_DTM\t>24");

        for (TypeDefinition typeDef : typeDefs.values()) {
            System.out.println(typeDef + "\t" + typeDef.getFullSuperTypeName());
        }
    }

    @Test
    public void testAE_numeric() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDIFF_DTM\t<25\n" +
                "AFTER24H\tDIFF_DTM\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"DIFF_DTM", "15"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("WITHIN24H")), 0) != null);
    }

    @Test
    public void testAE_numeric2() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDIFF_DTM\t<25\n" +
                "AFTER24H\tDIFF_DTM\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"DIFF_DTM", "25"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("AFTER24H")), 0) != null);
    }

    @Test
    public void testAE_Cat1() {
        String metaRuleStr = "&CONCEPT_FEATURES\tMD_NOTE\tConcept\n" +
                "&CONCEPT_FEATURES\tNS_NOTE\tConcept\n" +
                "MD_NOTE\tTYPE\tH&P;DISC_SUMM\n" +
                "NS_NOTE\tTYPE\tNURSE;EVAL";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"TYPE", "EVAL"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("NS_NOTE")), 0) != null);
    }

    @Test
    public void testAE_Cat2() {
        String metaRuleStr = "&CONCEPT_FEATURES\tMD_NOTE\tConcept\n" +
                "&CONCEPT_FEATURES\tNS_NOTE\tConcept\n" +
                "MD_NOTE\tTYPE\tH&P;DISC_SUMM\n" +
                "NS_NOTE\tTYPE\tNURSE;EVAL";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"TYPE", "H&P"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("MD_NOTE")), 0) != null);
    }


    @Test
    public void testAE_DateDiff1() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDATE\t-h\tREF_DATE\t<25\n" +
                "AFTER24H\tDATE\t-h\tREF_DATE\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"DATE", "2015-09-02 11:30", "REF_DATE", "2015-09-01 10:30"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("AFTER24H")), 0) != null);
    }

    @Test
    public void testAE_DateDiff2() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDATE\t-h\tREF_DATE\t<25\n" +
                "AFTER24H\tDATE\t-h\tREF_DATE\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"DATE", "2015-09-02 09:30", "REF_DATE", "2015-09-01 9:30"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("WITHIN24H")), 0) != null);
    }


    @Test
    public void testAE_DateDiff3() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tDATE\t-h\tREF_DATE\t<25\n" +
                "AFTER24H\tDATE\t-h\tREF_DATE\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"DATE", "2015-09-04 09:30", "REF_DATE", "2015-09-02 9:30"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("AFTER24H")), 0) != null);
    }

    @Test
    public void testAE_Calculation1() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tVALUE1\t-\tVALUE2\t<25\n" +
                "AFTER24H\tVALUE1\t-\tVALUE2\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"VALUE1", "30", "VALUE2", "2"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("AFTER24H")), 0) != null);
    }

    @Test
    public void testAE_Calculation2() {
        String metaRuleStr = "&CONCEPT_FEATURES\tWITHIN24H\tConcept\n" +
                "&CONCEPT_FEATURES\tAFTER24H\tConcept\n" +
                "WITHIN24H\tVALUE1\t+\tVALUE2\t<25\n" +
                "AFTER24H\tVALUE1\t+\tVALUE2\t>24";
        String docText = "This is a test documents for meta annotator.";
        String[] metaKeyValuePairs = new String[]{"VALUE1", "15", "VALUE2", "15"};
        process(metaRuleStr, docText, metaKeyValuePairs);
        assert (JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace("AFTER24H")), 0) != null);
    }

}