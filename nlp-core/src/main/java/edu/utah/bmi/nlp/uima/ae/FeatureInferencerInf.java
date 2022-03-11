package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import org.apache.uima.jcas.JCas;

import java.util.LinkedHashMap;

public interface FeatureInferencerInf {

    public void init();

    public void processInferences(JCas jcas);

    public LinkedHashMap<String, TypeDefinition> getTypeDefinitions(IOUtil ioUtil);

    public LinkedHashMap<String, TypeDefinition> getTypeDefinitions();
}
