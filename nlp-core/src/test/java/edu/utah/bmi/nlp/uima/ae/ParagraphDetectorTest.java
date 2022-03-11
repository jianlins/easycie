package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.type.system.Paragraph;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

public class ParagraphDetectorTest {
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine paragraphDetector, printer, counter;
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


    @Test
    public void test() {
        String docText = "Problem: Daily Care\n" +
                "\n" +
                "\n" +
                "\n" +
                "Goal: Daily care needs are met\n" +
                "\n" +
                "\n" +
                "\n" +
                "Outcome: Progressing\n" +
                "\n" +
                "\n" +
                "\n" +
                "Problem: Drain Management/ Wound";
        createCounter(4);
        process(docText);
        assert (AnnotationCountEvaluator.pass);
    }

    @Test
    public void test1() {
        String docText = "I put these and other questions to Smith in a recent interview. \nA lightly edited transcript of our conversation follows.\n" +
                "Sean Illing\n" +
                "\n" +
                "It’s hard to sum up the thesis of a book like this. How would you characterize it?";
        createCounter(2);
        process(docText);
        assert (AnnotationCountEvaluator.pass);
    }

    @Test
    public void test2() {
        String docText = "Patient will not fall during hospitalization. \n" +
                "\n" +
                "\n" +
                "\n" +
                "Outcome: Progressing\n" +
                "\n" +
                "\n" +
                "\n" +
                "Problem: Bladder/Voiding\n" +
                "\n" +
                "\n" +
                "\n" +
                "Goal: Patient verbalizes understanding of catheter care indwelling/intermittent\n" +
                "\n";
        createCounter(3);
        process(docText);
        System.out.println(JCasUtil.select(jCas, Paragraph.class));
    }

    private void process(String docText) {
        runner.reInitTypeSystem("target/generated-test-sources/customized.xml");
        jCas = runner.initJCas();
        jCas.setDocumentText(docText);
        try {
            paragraphDetector = createEngine(ParagraphDetector.class);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
        try {
            paragraphDetector.process(jCas);
            printer.process(jCas);
            counter.process(jCas);
        } catch (AnalysisEngineProcessException e) {
            e.printStackTrace();
        }
    }

    private void createCounter(int num) {
        try {
            counter = createEngine(AnnotationCountEvaluator.class, AnnotationCountEvaluator.PARAM_TYPE_NAME, "Paragraph", AnnotationCountEvaluator.PARAM_TYPE_COUNT, num);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
    }
}