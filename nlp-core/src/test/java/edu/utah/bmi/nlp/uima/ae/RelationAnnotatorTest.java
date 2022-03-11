package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.type.system.Relation;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static edu.utah.bmi.nlp.core.StdOut.println;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

public class RelationAnnotatorTest {
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private AnalysisEngine relationAnnotator, printer, tokenizer;
    private String typeDescriptor;

    @BeforeAll
    public static void setLoader(){
        MemoryClassLoader.CURRENT_LOADER_NAME ="testRelation";
    }
    @AfterAll
    public static void resetLoader(){
        MemoryClassLoader.resetLoaderName();
    }

    private void init(String printTypeName, String text) {
        typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        String ruleStr = "src/test/resources/source/Test_Relation_AE.tsv";
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new RelationAnnotator().getTypeDefs(ruleStr);
        runner.addConceptType(new TypeDefinition("Observation", "Concept", Arrays.asList(new String[]{"FMObservation"})));
        runner.addConceptType(new TypeDefinition("FamilyMember", "Concept", Arrays.asList(new String[]{"FMRelation"})));
        runner.addConceptType(new TypeDefinition("LivingStatus", "Concept", Arrays.asList(new String[]{"FMLivingStatus"})));
        runner.addConceptTypes(typeDefinitions.values());
        runner.reInitTypeSystem("target/generated-test-sources/customized.xml");
        jCas = runner.initJCas();
        try {
            relationAnnotator = createEngine(RelationAnnotator.class,
                    RelationAnnotator.PARAM_RULE_STR, ruleStr);
            tokenizer = createEngine(SimpleParser_AE.class, SimpleParser_AE.PARAM_INCLUDE_PUNCTUATION, true);
            printer = createEngine(AnnotationPrinter.class,
                    AnnotationPrinter.PARAM_INDICATION, "",
                    AnnotationPrinter.PARAM_TYPE_NAME,
                    DeterminantValueSet.checkNameSpace("Relation"));
            jCas.setDocumentText(text);
            tokenizer.process(jCas);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        } catch (AnalysisEngineProcessException e) {
            e.printStackTrace();
        }

    }


    private Annotation addConcept(JCas jCas, String type, int begin, int end) {
        Class<? extends Annotation> annoCls = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(type));
        Annotation annotation = null;
        try {
            Constructor<? extends Annotation> constructor = annoCls.getConstructor(JCas.class, int.class, int.class);
            annotation = constructor.newInstance(jCas, begin, end);
            annotation.addToIndexes();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return annotation;
    }

    private void print(JCas jCas, String type) {
        print(jCas, type, true);
    }

    private void print(JCas jCas, String type, boolean brief) {
        for (Annotation anno : JCasUtil.select(jCas, AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(type)))) {
            if (brief)
                System.out.println(String.format("%s (%d-%d):\t%s", anno.getType().getShortName(), anno.getBegin(), anno.getEnd(), anno.getCoveredText()));
            else
                System.out.println(anno);
        }
    }


    @Test
    public void getTypeDefs() {
        LinkedHashMap<String, TypeDefinition> types = new RelationAnnotator().getTypeDefs("src/test/resources/source/Test_Relation_AE.tsv");
        for (TypeDefinition td : types.values()) {
            System.out.println(td);
        }
    }

    private Annotation addConcept(String phrase, String text, String type) {
        return addConcept(phrase, text, type, 0);
    }

    private Annotation addConcept(String phrase, String text, String type, int numOfInstance) {
        Annotation anno = null;
        int offset = 0;
        int begin = -1, end = -1;
        for (int i = 0; i <= numOfInstance; i++) {
            begin = text.indexOf(phrase, offset);
            end = begin + phrase.length();
            offset = end;
        }
        if (end > 0) {
            anno = addConcept(jCas, type, begin, end);
            println(phrase + "(" + begin + "-" + end + ")");
        }
        return anno;
    }

    @Test
    public void test1() throws AnalysisEngineProcessException {
        String text = "Another sister, age 39, is in overall good general health but does" +
                " have a history of conduction disorders on her face at age 49.";
        init("Concept", text);
        addConcept("sister", text, "FamilyMember");
        addConcept("conduction disorders", text, "Observation");
        relationAnnotator.process(jCas);
//        printer.process(jCas);
        assert (JCasUtil.select(jCas, Relation.class).size() == 1);
    }

    @Test
    public void test2() throws AnalysisEngineProcessException {
        String text = "Another sister, age 39, is in overall good general health but does" +
                " have a history of conduction disorders on her face at age 49.";
        init("Concept", text);

        addConcept("sister", text, "FamilyMember");
        addConcept("conduction disorders", text, "Observation");
        addConcept("health", text, "LivingStatus");
        println(RelationAnnotator.logger.getLevel());
        relationAnnotator.process(jCas);
        assert (JCasUtil.select(jCas, Relation.class).size() == 2);
    }

    @Test
    public void test3() throws AnalysisEngineProcessException {
        String text = "A daughter, age 25, and a son, age 34, are both in overall good general health. Two brothers, ages 26 and 37, are healthy.";
        init("Concept", text);
        addConcept("daughter", text, "FamilyMember");
        addConcept("son", text, "FamilyMember");
        addConcept("Two brothers", text, "FamilyMember");
        addConcept("health", text, "LivingStatus");
        addConcept("healthy", text, "LivingStatus");
        relationAnnotator.process(jCas);
        print(jCas, "Relation");
        assert (JCasUtil.select(jCas, Relation.class).size() == 4);
    }

    @Test
    public void test4() throws AnalysisEngineProcessException {

        String text = "Yow has a healthy 30-year-old brother who has two healthy children and a healthy 34-year-old sister who has five healthy children and who has one hypermnesia.  ";
        init("Concept", text);
        addConcept("brother", text, "FamilyMember");
        addConcept("children", text, "FamilyMember");
        addConcept("sister", text, "FamilyMember");
        addConcept("healthy", text, "LivingStatus");
        addConcept("healthy", text, "LivingStatus", 1);
        addConcept("healthy", text, "LivingStatus", 2);
        addConcept("healthy", text, "LivingStatus", 3);
        relationAnnotator.process(jCas);
        print(jCas, "Relation");
    }

    @Test
    public void test5() throws AnalysisEngineProcessException {
        String text = "grant aunt and aunt with cervical cancer.";
        init("Concept", text);
        addConcept("grant aunt", text, "FamilyMember");
        addConcept("aunt", text, "FamilyMember");
        addConcept("cervical cancer", text, "Observation");
        relationAnnotator.process(jCas);
        print(jCas, "Relation");
    }

//    @Disabled
    @Test
    public void test6() throws AnalysisEngineProcessException {
        String text = "half brother/throat cancer.";
        init("Concept", text);
        Annotation anno = addConcept("half brother", text, "FamilyMember");
        AnnotationOper.setFeatureValue(AnnotationOper.getDefaultSetMethod(anno.getClass(), "FMRelation"), anno, "BROTHER");
        addConcept("throat cancer", text, "Observation");
        relationAnnotator.process(jCas);
        print(jCas, "Relation",false);
    }

}