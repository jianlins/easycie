package edu.utah.bmi.nlp.uima;

import edu.utah.bmi.nlp.sql.RecordRow;
import org.apache.uima.jcas.JCas;

public interface Processable {
    JCas process(RecordRow recordRow, String textColumnName, String... excludeColumns);

    JCas process(String inputStr, String... metaStr);

    void showResults();
}
