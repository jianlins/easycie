package edu.utah.bmi.nlp.uima.loggers;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;

/**
 * @author Jianlin Shi
 * Created on 1/13/17.
 */
public interface UIMALogger {

    void reset();

    void setItem(String key, Object value);

    void logStartTime();

    void logCompleteTime();

    long getStarttime();

    long getCompletetime();

    String logItems();

    Object getRunid();

    String getItem(String key);

    void logString(String msg);

    String getUnit();

    void setUnit(String unit);

    void initializationComplete(int totalDocs);

    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus);

    void batchProcessComplete();

    void collectionProcessComplete(String reportContent);

    void paused();

    void resumed();

    void aborted();
}
