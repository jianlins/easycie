package edu.utah.bmi.nlp.uima;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.loggers.ConsoleLogger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.CollectionReaderDescription_impl;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.util.LinkedHashMap;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdaptableUIMACPEDescriptorRunnerTest {
    @Test
    @Order(4)
    public void testBasic() throws CpeDescriptorException {
        AdaptableCPEDescriptorRunner runner = new AdaptableCPEDescriptorRunner("desc/cpe/demo_cpe.xml", "test", null);
        CpeCasProcessor[] cpeCasProcessors = runner.currentCpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
        for (CpeCasProcessor cpeCasProcessor : cpeCasProcessors) {
            System.out.println(cpeCasProcessor.getName());
        }
    }

    @Test
    @Order(3)
    public void testMetaReader() throws CpeDescriptorException {
        LinkedHashMap<String, String> configs = new LinkedHashMap<>();
        configs.put("FastNER/RuleFileOrStr", "@fastner\n" +
                "@splitter:|\n" +
                "very concept|ConceptA\n" +
                "tee|ConceptB");
        configs.put("SQL_Meta_Text_Reader/MetaColumns", "Code,Label");

        AdaptableCPEDescriptorRunner runner =
                new AdaptableCPEDescriptorRunner("desc/cpe/demo_cpe.xml", "v1", null, AdaptableCPEDescriptorRunner.parseExternalConfigMap(configs));
        CpeCollectionReader[] readers = runner.currentCpeDesc.getAllCollectionCollectionReaders();
        for (CpeCollectionReader reader : readers) {
            System.out.println(reader instanceof CollectionReaderDescription_impl);
            System.out.println(reader.getDescriptor().getSourceUrlString());
            System.out.println(reader.getClass().getSimpleName());
            CpeComponentDescriptor desc = reader.getCollectionIterator().getDescriptor();
            System.out.println(desc.getImport().getLocation());
        }
    }

    @Test
    @Order(2)
    public void testCompile() throws ClassNotFoundException {
        AdaptableCPEDescriptorRunner runner = new AdaptableCPEDescriptorRunner("desc/cpe/demo_cpe.xml", "test",
                null, "target/generated-test-sources", "target/generated-test-sources","target/generated-test-sources");
        Class<? extends Annotation> ccls = AnnotationOper.getTypeClass("Concept", true);
        runner.addConceptType("Digit3","Concept");
        runner.addConceptType("Digit4","Concept");
        runner.reInitTypeSystem("target/generated-test-sources/tmp.xml","target/test-classes");
        Class<? extends Annotation> cls = AnnotationOper.getTypeClass("Digit3",true);
        System.out.println(cls);
        assert(Concept.class.isAssignableFrom(cls));
    }

    @Test
    @Order(1)
    public void testRunPipe() throws InterruptedException {
        LinkedHashMap<String, String> configs = new LinkedHashMap<>();
        configs.put("StringMetaReader/InputString", "The patient visited E.D. this morning with high fever. He was treated with abx.");
        configs.put("StringMetaReader/Meta", "");
        configs.put("AnnotationPrinter/TypeName","SourceDocumentInformation");
//        configs.put("SQL_Meta_Text_Reader/MetaColumns", "Code,Label");
        AdaptableCPEDescriptorRunner runner = AdaptableCPEDescriptorRunner.getInstance("src/test/resources/desc/test_reader_cpe.xml", "test", configs,"target/generated-test-sources/");
        runner.setUIMALogger(new ConsoleLogger());
        Logger logger = UIMAFramework.getLogger();
        System.out.println(logger.isLoggable(Level.FINEST));
        runner.run();
        SimpleStatusCallbackListenerImpl listener = runner.getStatCbL();
        while (listener.isProcessing) {
            Thread.sleep(5);
        }
        System.out.println("finish");
    }
}