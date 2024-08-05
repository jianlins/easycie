package edu.utah.bmi.nlp.easycie.reader;

import edu.utah.bmi.nlp.easycie.MetaDataCommonFunctions;
import edu.utah.bmi.nlp.sql.EDAO;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.sql.RecordRowIterator;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * In order to integrate metadata into document level reasoning, use  SQLMetaTextReader instead
 *
 * @see edu.utah.bmi.nlp.easycie.reader.SQLMetaTextReader
 *
 */
public class SQLTextReader extends CollectionReader_ImplBase {
    public static Logger logger = Logger.getLogger(SQLTextReader.class.getCanonicalName());
    public static final String PARAM_DB_CONFIG_FILE = "DBConfigFile";
    public static final String PARAM_DOC_TABLE_NAME = "DocTableName";
    public static final String PARAM_QUERY_SQL_NAME = "InputQueryName";
    public static final String PARAM_COUNT_SQL_NAME = "CountQueryName";
    public static final String PARAM_DOC_COLUMN_NAME = "DocColumnName";
    public static final String PARMA_TRIM_TEXT = "TrimText";
    public static final String PARAM_DATASET_ID = "DatasetId";
    protected File dbConfigFile;
    protected String querySqlName, countSqlName, docColumnName, docTableName;
    public static EDAO dao = null;
    protected int mCurrentIndex, totalDocs;
    protected RecordRowIterator recordIterator;
    @Deprecated
    public static boolean debug = false;
    public boolean trimText = false;
    protected String datasetId;


    public void initialize() {
        readConfigurations();
        this.mCurrentIndex = 0;
        addDocs();
    }

    protected void readConfigurations() {
        if (System.getProperty("java.util.logging.config.file") == null &&
                new File("logging.properties").exists()) {
            System.setProperty("java.util.logging.config.file", "logging.properties");
        }
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dbConfigFile = new File(readConfigureString(PARAM_DB_CONFIG_FILE, null));
        logger.info("read db config: "+dbConfigFile);
        dao = EDAO.getInstance(this.dbConfigFile);
        querySqlName = readConfigureString(PARAM_QUERY_SQL_NAME, "masterInputQuery");
        countSqlName = readConfigureString(PARAM_COUNT_SQL_NAME, "masterCountQuery");
        docColumnName = readConfigureString(PARAM_DOC_COLUMN_NAME, "TEXT");
        docTableName = readConfigureString(PARAM_DOC_TABLE_NAME, "DOCUMENTS");
        datasetId = readConfigureString(PARAM_DATASET_ID, "0");
        Object value = this.getConfigParameterValue(PARMA_TRIM_TEXT);
        if (value instanceof Boolean)
            trimText = (Boolean) value;
    }

    protected String readConfigureString(String parameterName, String defaultValue) {
        Object tmpObj = this.getConfigParameterValue(parameterName);
        if (tmpObj == null) {
            if (defaultValue == null) {
                throw new UIMA_IllegalArgumentException("parameter not set", new Object[]{parameterName, this.getMetaData().getName()});
            } else {
                tmpObj = defaultValue;
            }
        }
        return (tmpObj + "").trim();
    }

    protected void addDocs() {
        totalDocs = dao.countRecords(countSqlName, docTableName, datasetId);
        logger.info("Total documents need to be processed: " + totalDocs);
        recordIterator = dao.queryRecordsFromPstmt(querySqlName, docTableName, datasetId);
    }

    public boolean hasNext() {

        return recordIterator != null && recordIterator.hasNext();
    }

    public void getNext(CAS aCAS) throws CollectionException {
        RecordRow currentRecord = recordIterator.next();
        String text = (String) currentRecord.getValueByColumnName(docColumnName);
        if (trimText) {
            text = text.replaceAll("(\\n[^\\w\\p{Punct}]+\\n)", "\n\n")
                    .replaceAll("(\\n\\s*)+(?:\\n)", "\n\n")
                    .replaceAll("^(\\n\\s*)+(?:\\n)", "")
                    .replaceAll("[^\\w\\p{Punct}\\s]", " ");
        }
        logger.finest("Read document: " + currentRecord.getStrByColumnName("DOC_NAME"));
        if (text == null)
            text = "";
        JCas jcas;
        try {
            jcas = aCAS.getJCas();
        } catch (CASException var6) {
            throw new CollectionException(var6);
        }
        jcas.setDocumentText(text);
        SourceDocumentInformation srcDocInfo = MetaDataCommonFunctions.genSourceDocumentInformationAnno(jcas, currentRecord, docColumnName, text.length());

        srcDocInfo.setLastSegment(this.mCurrentIndex == this.totalDocs);
        srcDocInfo.addToIndexes();
        mCurrentIndex++;
    }


    public void close() {
        dao.close();
    }

    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(this.mCurrentIndex, totalDocs, Progress.ENTITIES)};
    }

    public int getNumberOfDocuments() {
        return this.totalDocs;
    }
}
