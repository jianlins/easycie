package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastner.uima.FastNER_AE_General;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class TrendPatternInferencerTest {
    private String ruleStr;
    private  AdaptableUIMACPERunner runner;

    @BeforeAll
    public static void clearTargetDir() {
        try {
            FileUtils.deleteDirectory(new File("target/target/generated-test-classes"));
            FileUtils.deleteDirectory(new File("target/generated-test-sources"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @BeforeEach
    public void setUpClass() throws Exception {
        String ruleFilePath = "edu/utah/bmi/nlp/uima/ae/80_TrendPatternInference_AE.tsv";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(ruleFilePath)) {
            if (inputStream == null) {
                fail("Rule file not found: " + ruleFilePath);
            }
            ruleStr = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
        String typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-classes");
        Collection<TypeDefinition> types = new TrendPatternInferencer().getTypeDefs(ruleStr).values();
        runner.addConceptTypes(types);
        runner.reInitTypeSystem("target/generated-test-sources/customized", "target/generated-test-sources/");
    }

    private void simulateStackerDataMatching(TrendPatternInferencer trendPatternInferencer) {
        DateTime date1 = new DateTime("2021-01-01");
        DateTime date2 = new DateTime("2021-01-02");

        trendPatternInferencer.stacker.put(date1, new LinkedHashSet<>());
        trendPatternInferencer.stacker.put(date2, new LinkedHashSet<>());

        trendPatternInferencer.stacker.get(date1).add("SSI_Doc");
        trendPatternInferencer.stacker.get(date2).add("Healed_Doc");
    }

    @Test
    public void testTrendPatternInferencer() throws Exception {

        // Create a JCas instance
        JCas jCas = runner.initJCas();

        // Set up the Analysis Engine
        AnalysisEngine trendPatternInferencer = AnalysisEngineFactory.createEngine(TrendPatternInferencer.class,
                TrendPatternInferencer.PARAM_RULE_STR, ruleStr,
                TrendPatternInferencer.PARAM_BUNCH_COLUMN_NAME, "BUNCH_ID",
                TrendPatternInferencer.PARAM_DOCID_COLUMN_NAME, "DOC_ID",
                TrendPatternInferencer.PARAM_REFERENCE_DATE_COLUMN_NAME, "REF_DTM",
                TrendPatternInferencer.PARAM_RECORD_DATE_COLUMN_NAME, "DATE",
                TrendPatternInferencer.PARAM_OVERWRITE_NOTE, true,
                TrendPatternInferencer.PARAM_MATCH_PRESENCE, TrendPatternInferencer.LAST_PRESENCE);

        // Create a dummy document with annotations
        String documentText = "Sample document text with various annotations.";
        jCas.setDocumentText(documentText);

        // Add dummy RecordRow metadata
        RecordRow recordRow = new RecordRow();
        recordRow.addCell("BUNCH_ID", "1");
        recordRow.addCell("DOC_ID", "doc1");
        recordRow.addCell("REF_DTM", "2021-01-01");
        recordRow.addCell("DATE", "2021-01-02");
        String metaInfor = recordRow.serialize();
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas, 0, documentText.length());
        srcDocInfo.setUri(metaInfor);
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize(documentText.length());
        srcDocInfo.setLastSegment(true);
        srcDocInfo.addToIndexes();

        // Process the JCas with the Analysis Engine

        trendPatternInferencer.process(jCas);

        simulateStackerDataMatching((TrendPatternInferencer) trendPatternInferencer);

        trendPatternInferencer.collectionProcessComplete();
        // Verify the results
        Collection<RecordRow> results = TrendPatternInferencer.bunchRecordRows;
        assertFalse(results.isEmpty(), "No results were generated.");


        // Cleanup

    }

}
