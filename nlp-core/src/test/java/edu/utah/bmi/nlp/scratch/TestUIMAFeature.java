package edu.utah.bmi.nlp.scratch;

import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.EntityBASE;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

public class TestUIMAFeature {
    private JCas jCas;

    @BeforeEach
    public void init() throws UIMAException {
        jCas = JCasFactory.createJCas("desc/type/All_Types");
        jCas.setDocumentText("This is a test jcas.");
        Concept concept = new Concept(jCas, 6, 8);
        concept.addToIndexes();
        EntityBASE entityBASE = new EntityBASE(jCas, 10, 11);
        entityBASE.addToIndexes();

    }

    @Test
    public void getAnnotations() {
        Collection<AnnotationFS> annotations = CasUtil.selectAll(jCas.getCas());
        for (AnnotationFS anno : annotations) {
            System.out.println(anno);
        }

    }

    @Test
    public void getAnnotationsByType() {
        Type conceptType = CasUtil.getType(jCas.getCas(), "edu.utah.bmi.nlp.type.system.Concept");
        Collection<AnnotationFS> annotations = CasUtil.select(jCas.getCas(),conceptType);
        for (AnnotationFS anno : annotations) {
            System.out.println(anno);
        }
    }

    @Test
    public void getFeatures() {
        Type conceptType = CasUtil.getAnnotationType(jCas.getCas(),Concept.class);
        for (Feature f : conceptType.getFeatures()) {
            System.out.println(f.getDomain());
            System.out.println(f.getShortName());
            System.out.println(f.getRange());
            System.out.println(f.getName());
        }
    }

    @Test
    public void getTestClass(){
        ArrayList<AnnotationFS> annos=new ArrayList<>();
        annos.addAll(CasUtil.selectAll(jCas.getCas()));
        AnnotationFS anno = annos.get(1);
        System.out.println(anno.getClass());
        String a="dd";
        System.out.println(a.getClass().getName());
        int b=2;
        System.out.println(((Object) b).getClass().getSimpleName());
        System.out.println(isInteger(b));
        byte c = a.getBytes()[0];

        System.out.println(((Object)c).getClass().getSimpleName());

    }
    public static boolean isInteger(Object object) {
        System.out.println(object.getClass());
        if(object instanceof Integer) {
            return true;
        } else {
            String string = object.toString();
            try {
                Integer.parseInt(string);
            } catch(Exception e) {
                return false;
            }
        }

        return true;
    }
}
