package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.uima.reader.StringMetaReader;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;

public class GenAEDescriptorFiles {

    public static String descDir = "target/generated-sources/desc/";

    public static void main(String[] args) throws ResourceInitializationException, IOException, SAXException {
        if (!descDir.endsWith("/")) {
            descDir = descDir + "/";
        }
        if (!new File(descDir).exists()) {
            new File(descDir).mkdirs();
        }
        LinkedHashMap<Class, String> clss = new LinkedHashMap<>();
        clss.put(StringMetaReader.class, "StringMetaReader");
        clss.put(AnnotationCountEvaluator.class, null);
        clss.put(AnnotationCountEvaluator.class, null);
        clss.put(AnnotationFeatureMergerAnnotator.class, "70_FeatureMerger_aeDescriptor");
        clss.put(FeatureInferenceAnnotator.class, "60_FeatureInference_aeDescriptor");
        System.out.println("Generated ae descriptor files will be saved to:\n\t" + descDir);
        for (Class cls : clss.keySet()) {
            gen(cls, clss.get(cls));
        }
        System.out.println("Done! Please double check before move them to desc/ae_ordered.");
    }


    public static void gen(Class cls, String descriptorName) throws ResourceInitializationException, IOException, SAXException {
        if (JCasAnnotator_ImplBase.class.isAssignableFrom(cls)) {
            AnalysisEngineDescription analysisEngine = null;
            analysisEngine = AnalysisEngineFactory.createEngineDescription(
                    cls);
            if (descriptorName == null) {
                descriptorName = cls.getSimpleName() + "_aeDescriptor.xml";
            }
            if (!descriptorName.toLowerCase().endsWith(".xml")) {
                descriptorName = descriptorName + ".xml";
            }
            analysisEngine.toXML(
                    new FileOutputStream(descDir + descriptorName));
        } else if (CollectionReader_ImplBase.class.isAssignableFrom(cls)) {
            CollectionReaderDescription rd = CollectionReaderFactory.createReaderDescription(cls);
            if (descriptorName == null) {
                descriptorName = cls.getSimpleName() + ".xml";
            }
            if (!descriptorName.toLowerCase().endsWith(".xml")) {
                descriptorName = descriptorName + ".xml";
            }
            rd.toXML(new FileOutputStream(descDir + descriptorName));
        }
    }
}
