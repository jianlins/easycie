package edu.utah.bmi.nlp.uima.loggers;


import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.easycie.entry.EasyCIETask;
import edu.utah.bmi.nlp.sql.EDAO;
import edu.utah.bmi.nlp.sql.RecordRow;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Jianlin Shi
 * Created on 1/13/17.
 */
public class NLPDBEDWLogger extends NLPDBConsoleLogger {
    public static Logger classLogger = IOUtil.getLogger(NLPDBEDWLogger.class);

    protected NLPDBEDWLogger() {
        ldao = null;
        tableName = "";
        keyColumnName = "";

    }
    public NLPDBEDWLogger(String dbConfigureFile, String tableName, String keyColumnName, String annotator, int maxCommentLength) {
        this.dbConfigureFile = dbConfigureFile;
        this.ldao = EDAO.getInstance(new File(dbConfigureFile), true, false);
        this.tableName = tableName;
        this.initRecordRow = new RecordRow().addCell("PIPELINE_ID", Integer.parseInt(annotator))
                .addCell("ANNOTATOR", annotator);
        this.keyColumnName = keyColumnName;
        recordRow = new RecordRow();
        this.maxCommentLength = maxCommentLength;
    }


    public NLPDBEDWLogger(String dbConfigureFile, String tableName, String keyColumnName, int maxCommentLength, Object... initials) {
        super(dbConfigureFile, tableName, keyColumnName, maxCommentLength, initials);
    }


    @Override
    public void collectionProcessComplete(String reportContent) {
        logCompleteTime();
        classLogger.fine(this.df.format(completeTime) + "\tProcessing Complete");
        logString("Processing Complete.");
        long initTime = this.initCompleteTime - startTime;
        long processingTime = completeTime - initCompleteTime;
        long elapsedTime = initTime + processingTime;
        StringBuilder report = new StringBuilder();
        report.append(this.entityCount + " notes\n");
        if (this.size > 0L) {
            report.append(this.size + " characters\n");
        }

        report.append("Total:\t" + elapsedTime + " ms\n");
        report.append("Initialization:\t" + initTime + " ms\n");
        report.append("Processing:\t" + processingTime + " ms\n");
        report.append(reportContent);
        setItem("NUM_NOTES", this.entityCount);
        String comments;
        if (this.maxCommentLength > 0 && report.length() > this.maxCommentLength) {
            comments = report.substring(0, this.maxCommentLength);
        } else {
            comments = report.toString();
        }
        if (maxCommentLength > 0 && comments.length() > maxCommentLength)
            comments = comments.substring(0, maxCommentLength);
        setItem("COMMENTS", comments);

        ldao.updateRecord(tableName, recordRow);
        ldao.close();

        reset();
    }

}
