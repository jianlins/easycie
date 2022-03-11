package edu.utah.bmi.nlp.sectiondectector;

import edu.utah.bmi.nlp.rush.uima.RuSH_AE;
import edu.utah.bmi.nlp.type.system.SectionBody;
import edu.utah.bmi.nlp.type.system.SectionHeader;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * Using existing tuned rules based on known dataset, build vocabulary based stats model for
 * stats-based section detector AE
 *
 * @author Jianlin Shi on 11/30/19.
 */
public class GenVocabStatsModel {

    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private Object[] configurationData;
    private AnalysisEngine sectionDetectorR_AE, ruSH_AE;

    public void setUp(String sectionRuleStr, String rushRuleStr) {
        String typeDescriptor = "desc/type/All_Types";
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        runner.addConceptTypes(new SectionDetectorR_AE().getTypeDefs(sectionRuleStr).values());
        runner.reInitTypeSystem("target/generated-test-sources/customized");
        jCas = runner.initJCas();
//      Set up the parameters
        configurationData = new Object[]{SectionDetectorR_AE.PARAM_RULE_STR, sectionRuleStr
                , SectionDetectorR_AE.PARAM_ENABLE_DEBUG, true};
        try {
            sectionDetectorR_AE = createEngine(SectionDetectorR_AE.class,
                    configurationData);
            ruSH_AE = createEngine(RuSH_AE.class, new Object[]{RuSH_AE.PARAM_RULE_STR, rushRuleStr, RuSH_AE.PARAM_TOKEN_TYPE_NAME,"Token"});
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() throws ResourceInitializationException,
            AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {
        setUp("src/test/resources/edu.utah.bmi.nlp/00_Section_Detector.tsv", "src/test/resources/edu.utah.bmi.nlp/10_RuSH_AE.tsv");

        String text = "THis is a blank section that has not been categorized. HISTORY:\n" +
                " The patient has history of right greater saphenous vein EVLT.  Physician requests right leg venous reflux mapping.\n" +
                " \n" +
                " FINDINGS:\n" +
                " The right lower extremity venous system is studied with gray scale and color duplex scanning.\n" +
                " \n" +
                " The right common femoral, profunda femoral, femoral, popliteal, posterior tibial, and peroneal veins are patent with spontaneous flow and normal responses to respiration and compression.  \n" +
                " IMPRESSION:\n" +
                " \n" +
                "     1. No deep venous thrombus of the right lower extremity.\n" +
                "     2. Chronic appearing superficial venous thrombus of the right lower extremity within greater saphenous vein that had previously undergone EVLT.";

        jCas.setDocumentText(text);

        configurationData = new Object[]{SectionDetectorR_AE.PARAM_RULE_STR, "conf/section_crules.tsv"
                , SectionDetectorR_AE.PARAM_ENABLE_DEBUG, true, SectionDetectorR_AE.PARAM_DEFAULT_DECTION, "Procedure"};
        try {
            sectionDetectorR_AE = createEngine(SectionDetectorR_AE.class,
                    configurationData);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
        sectionDetectorR_AE.process(jCas);
        for (Annotation annotation : JCasUtil.select(jCas, SectionHeader.class)) {
            System.out.println(annotation.getType().getShortName() + ":\t>" + annotation.getCoveredText() + "<");
        }

        for (Annotation annotation : JCasUtil.select(jCas, SectionBody.class)) {
            System.out.println("Section '" + annotation.getType().getShortName() + "': ");
            System.out.println(">" + annotation.getCoveredText() + "<");
        }
    }


}