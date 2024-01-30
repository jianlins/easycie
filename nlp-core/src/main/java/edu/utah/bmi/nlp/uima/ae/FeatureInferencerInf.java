package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import org.apache.uima.jcas.JCas;

import java.util.LinkedHashMap;

public interface FeatureInferencerInf {

    void init();

    void processInferences(JCas jcas);

    LinkedHashMap<String, TypeDefinition> getTypeDefinitions(IOUtil ioUtil);

    LinkedHashMap<String, TypeDefinition> getTypeDefinitions();
}
