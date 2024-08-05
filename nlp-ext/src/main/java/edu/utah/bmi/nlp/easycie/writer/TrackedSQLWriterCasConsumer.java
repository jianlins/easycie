package edu.utah.bmi.nlp.easycie.writer;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.Interval1D;
import edu.utah.bmi.nlp.core.IntervalST;
import edu.utah.bmi.nlp.sql.EDAO;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 /**
 * The TrackedSQLWriterCasConsumer class is a component of a UIMA (Unstructured Information Management Applications)
 * pipeline. Its primary function is to handle, track, and store processed annotations from the analysis
 * phase to a SQL database. Essentially, it serves as a bridge, ensuring data communication between the
 * text analyzing system and the storing process, thereby making the analysis results persistently available.
 *
 * Compared with regular DBCasConsumers, it can access the tracking static fields in the Analysis Engines included
 * in this nlp-ext module. These fields were used to generate cross-document reasoning results.
 *
 * @author Jianlin Shi
 * Created on 5/22/16.
 */
public class TrackedSQLWriterCasConsumer extends JCasAnnotator_ImplBase {
    public static Logger classLogger = IOUtil.getLogger(TrackedSQLWriterCasConsumer.class);
    //    allow to save sentence boundaries

    public static final String PARAM_DB_CONFIG_FILE = DeterminantValueSet.PARAM_DB_CONFIG_FILE;

    public static final String PARAM_SNIPPET_TABLENAME = "SnippetTableName";
    public static final String PARAM_DOC_TABLENAME = "DocTableName";
    public static final String PARAM_BUNCH_TABLENAME = "BunchTableName";

    public static final String PARAM_BUNCH_COLUMN_NAME = "BunchColumnName";

    public static final String PARAM_DOCID_COLUMN_NAME = "DocIdColumnName";

    public static final String PARAM_OVERWRITETABLE = "OverWriteTable";
    public static final String PARAM_WRITE_CONCEPT = "WriteConcepts";
    public static final String PARAM_BATCHSIZE = "BatchSize";
    public static final String PARAM_ANNOTATOR = DeterminantValueSet.PARAM_ANNOTATOR;
    public static final String PARAM_VERSION = DeterminantValueSet.PARAM_VERSION;
    public static final String PARAM_MIN_LENGTH = "MinTextLength";

    public static final String PARAM_SUPPORT_BUNCH_ANNO = "SupportBunchAnno";

    public static final String PARAM_CLS_HOLD_BUNCH_QUEUE = "ClassHoldBunchQueue";

    public static final String PARAM_MAX_SNIPPET_LENGTH = "MaxSnippetLength";

    public static final String PARAM_LOG_TABLENAME = "LogTableName";

    public static final String PARAM_LOG_PROCESS_COLUMN_NAME = "LogProcessColumnName";

    public static final String PARAM_USE_ANNOTATIONS_ANNOTATOR = "UserAnnotationsAnnotator";
    protected File sqlFile;
    protected String snippetTableName, docTableName, logTableName, bunchTableName, annotator = "", version, bunchColumnName, logProcessColumn, docIdColumnName;
    protected int mDocNum, batchSize = 15, minTextLength, maxSnippetLength = -1;
    public static EDAO dao = null;
    protected boolean debug = false, overwriteTable = false, useAnnotationsAnnotator = false;

    /**
     * Whether support Bunch_Base annotations, if so, the bunch annotation will be written into db when this writer switch to
     */
    protected boolean supportBunchAnno = false;
    private ConcurrentLinkedQueue<String> typeToSave = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, HashMap<String, Method>> annotationGetFeatures = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Class> annotationClassMap = new ConcurrentHashMap<>();

    protected int previousBunchId = -1;
    private LinkedBlockingQueue<RecordRow> bunchRecordRows = new LinkedBlockingQueue<>();


    public TrackedSQLWriterCasConsumer() {
    }


    public void initialize(UimaContext cont) {
        this.mDocNum = 0;
        this.sqlFile = new File(readConfigureString(cont, PARAM_DB_CONFIG_FILE, null));
        this.snippetTableName = readConfigureString(cont, PARAM_SNIPPET_TABLENAME, "RESULT_SNIPPET");
        this.docTableName = readConfigureString(cont, PARAM_DOC_TABLENAME, "RESULT_DOC");
        this.logTableName = readConfigureString(cont, PARAM_LOG_TABLENAME, "NLP_INPUT");
        this.bunchTableName = readConfigureString(cont, PARAM_BUNCH_TABLENAME, "RESULT_BUNCH");
        this.bunchColumnName = readConfigureString(cont, PARAM_BUNCH_COLUMN_NAME, "BUNCH_ID");
        this.docIdColumnName = readConfigureString(cont, PARAM_DOCID_COLUMN_NAME, "NOTE_ID");
        this.logProcessColumn=readConfigureString(cont, PARAM_LOG_PROCESS_COLUMN_NAME, "LAST_PROCESSED");
        String bunchQueueClassName = readConfigureString(cont, PARAM_CLS_HOLD_BUNCH_QUEUE, "BunchMixInferencer");

        if (!bunchQueueClassName.contains(".")) {
            bunchQueueClassName = "edu.utah.bmi.nlp.uima.ae." + bunchQueueClassName;
        }
        try {
            Class bunchQueueCls = Class.forName(bunchQueueClassName);
            Field bunchRecordRowsField = bunchQueueCls.getDeclaredField("bunchRecordRows");
            Object value = bunchRecordRowsField.get(null);
            bunchRecordRows = (LinkedBlockingQueue<RecordRow>) value;
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        previousBunchId = -1;

        Object value = readConfigureObject(cont, PARAM_OVERWRITETABLE, false);
        if (value instanceof Boolean)
            overwriteTable = ((Boolean) value);
        else
            overwriteTable = value.toString().toLowerCase().startsWith("t");
        useAnnotationsAnnotator = (Boolean) readConfigureObject(cont, PARAM_USE_ANNOTATIONS_ANNOTATOR, false);

        batchSize = (Integer) readConfigureObject(cont, PARAM_BATCHSIZE, 15);
        minTextLength = (Integer) readConfigureObject(cont, PARAM_MIN_LENGTH, 0);
        maxSnippetLength = (Integer) readConfigureObject(cont, PARAM_MAX_SNIPPET_LENGTH, -1);
        annotator = readConfigureString(cont, PARAM_ANNOTATOR, "uima");
        version = readConfigureString(cont, PARAM_VERSION, null);

        supportBunchAnno = (Boolean) readConfigureObject(cont, PARAM_SUPPORT_BUNCH_ANNO, false);

        dao = EDAO.getInstance(this.sqlFile);
        dao.batchsize = batchSize;
//        dao.initiateTableFromTemplate("ANNOTATION_TABLE", snippetTableName, overwriteTable);
//        dao.initiateTableFromTemplate("DOC_ANNO_TABLE", docTableName, overwriteTable);
//        if (supportBunchAnno)
//            dao.initiateTableFromTemplate("BUNCH_ANNO_TABLE", bunchTableName, overwriteTable);
//        if (!this.sqlFile.exists()) {
//            this.sqlFile.mkdirs();
//        }
        Object writeConceptObj = cont.getConfigParameterValue(PARAM_WRITE_CONCEPT);
        typeToSave.clear();
        if (writeConceptObj != null && writeConceptObj.toString().trim().length() > 0) {
            for (String type : ((String) writeConceptObj).split("[,;\\|]"))
                typeToSave.add(DeterminantValueSet.checkNameSpace(type.trim()));
        } else {
            typeToSave.add(Concept.class.getCanonicalName());
            typeToSave.add(Doc_Base.class.getCanonicalName());
        }
    }

    public void process(JCas jcas) {
        if (jcas.getDocumentText().length() < minTextLength)
            return;


        IntervalST sentenceTree = new IntervalST();
        ArrayList<Sentence> sentenceList = new ArrayList<>();


        RecordRow baseRecordRow = AnnotationOper.deserializeDocSrcInfor(jcas);
        baseRecordRow.addCell("RUN_ID", version);
        baseRecordRow.addCell("ANNOTATOR", this.annotator);

        int currentBunchId = Integer.parseInt(baseRecordRow.getStrByColumnName(bunchColumnName));
        if (supportBunchAnno) {
            if (previousBunchId == -1) {
                previousBunchId = currentBunchId;
            } else if (previousBunchId != currentBunchId) {
                saveBunchAnnotations();
                previousBunchId = currentBunchId;
            }
        }

        classLogger.finest("Write annotations for doc: " + baseRecordRow.getStrByColumnName(docIdColumnName));
        if (baseRecordRow.getValueByColumnName(docIdColumnName).equals("0")) {
//          if there is no note associated with this patient, skip rest logic.
            return;
        }

        FSIterator<Annotation> it = jcas.getAnnotationIndex(Sentence.type).iterator();
        while (it.hasNext()) {
            Sentence thisSentence = (Sentence) it.next();
            sentenceList.add(thisSentence);
            sentenceTree.put(new Interval1D(thisSentence.getBegin(), thisSentence.getEnd()), sentenceList.size() - 1);
        }

        int total = saveAnnotations(jcas, baseRecordRow, sentenceList, sentenceTree);
//        classLogger.finest("Total annotations: " + total);


//        ldao.insertRecords(snippetResultTable, annotations);
    }

    protected void saveBunchAnnotations() {
        RecordRow previousBunchRecordRow = bunchRecordRows.poll();
        if(previousBunchRecordRow==null)
            classLogger.warning("The previousBunchRecordRow is null");
        if (classLogger.isLoggable(Level.FINEST)) {
            if (previousBunchRecordRow != null)
                classLogger.finest("Save: " + previousBunchRecordRow.toString("\t"));
            else
                classLogger.finest("Nothing to Save: bunchRecordRows is empty.");
        }
        if (previousBunchRecordRow != null) {
            previousBunchRecordRow.addCell("ANNOTATOR", this.annotator);
            previousBunchRecordRow.addCell("RUN_ID", version);
            dao.insertRecord(bunchTableName, previousBunchRecordRow);
            RecordRow processLogRecord=new RecordRow();
            processLogRecord.addCell(bunchColumnName, previousBunchRecordRow.getValueByColumnName(bunchColumnName));
            dao.updateRecord(logTableName, processLogRecord);
        }
    }

    protected int saveAnnotations(JCas jcas, RecordRow baseRecordRow, ArrayList<Sentence> sentenceList, IntervalST sentenceTree) {
        String docText = jcas.getDocumentText();
        int total = 0;
        for (String typeName : typeToSave) {
            Class<? extends Annotation> typeCls = getAnnotationClass(typeName);
            if (typeCls == null || typeCls.getSimpleName().endsWith("SourceDocumentInformation")) {
                continue;
            }
            String tableName = snippetTableName;
            if (Doc_Base.class.isAssignableFrom(typeCls)) {
//                System.out.println(typeCls.getSuperclass().getCanonicalName());
                tableName = docTableName;
            }

            total += saveOneTypeAnnotation(jcas, docText, typeCls, baseRecordRow, tableName, sentenceList, sentenceTree);
        }
        return total;
    }

    protected Class<? extends Annotation> getAnnotationClass(String typeName) {
        if (annotationClassMap.containsKey(typeName)) {
            return annotationClassMap.get(typeName);
        } else {
            Class<? extends Annotation> annoCls = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(typeName));
            annotationClassMap.put(typeName, annoCls);
            return annoCls;
        }
    }

    protected Method getAnnotationGetMethod(Annotation annotation, String featureName) {
        String typeName = annotation.getClass().getSimpleName();
        if (!annotationGetFeatures.containsKey(typeName))
            annotationGetFeatures.put(typeName, new LinkedHashMap<>());
        Method getMethod;
        if (!annotationGetFeatures.get(typeName).containsKey(featureName)) {
            getMethod = AnnotationOper.getDefaultGetMethod(annotation.getClass(), featureName);
            annotationGetFeatures.get(typeName).put(featureName, getMethod);
        } else {
            getMethod = annotationGetFeatures.get(typeName).get(featureName);
        }
        return getMethod;
    }

    private int saveOneTypeAnnotation(JCas jcas, String docText, Class annotationCls,
                                      RecordRow baseRecordRow, String tableName,
                                      ArrayList<Sentence> sentenceList, IntervalST sentenceTree) {
        int total = 0;
        Iterator<AnnotationFS> annoIter = JCasUtil.iterator(jcas, annotationCls);
        while (annoIter.hasNext()) {
            Annotation thisAnnotation = (Annotation) annoIter.next();
            RecordRow record = baseRecordRow.clone();
            record.addCell("TYPE", thisAnnotation.getType().getShortName());
            if (thisAnnotation instanceof Bunch_Base)
                record.addCell("DOC_NAME", record.getValueByColumnName(bunchColumnName));
            String text = "";
            try {
                text = thisAnnotation.getCoveredText();
            } catch (Exception e) {
                classLogger.warning("thisAnnotation error: " + thisAnnotation.getClass().getSimpleName() + "\t" + thisAnnotation.getBegin() + "-" + thisAnnotation.getEnd() + " (" + docText.length() + ")");
            }
            record.addCell("TEXT", text);
            if (useAnnotationsAnnotator) {
                if (thisAnnotation instanceof ConceptBASE) {
                    ConceptBASE conceptBASE = (ConceptBASE) thisAnnotation;
                    String thisAnnotator = conceptBASE.getAnnotator();
                    if (thisAnnotator == null || thisAnnotator.length() == 0)
                        record.addCell("ANNOTATOR", this.annotator);
                    else
                        record.addCell("ANNOTATOR", thisAnnotator);
                }
            } else {
                record.addCell("ANNOTATOR", this.annotator);
            }

            if (maxSnippetLength > 0) {
                StringBuilder sb = new StringBuilder();
                for (Feature feature : thisAnnotation.getType().getFeatures()) {
                    String domain = feature.getDomain().getShortName();
                    if (domain.equals("AnnotationBase") || domain.equals("Annotation"))
                        continue;
                    String featureName = feature.getShortName();
                    String value = "";
                    String typeName = thisAnnotation.getClass().getSimpleName();
                    if (!annotationGetFeatures.containsKey(typeName))
                        annotationGetFeatures.put(typeName, new LinkedHashMap<>());
                    Method getMethod = getAnnotationGetMethod(thisAnnotation, featureName);
                    if (getMethod != null) {
                        try {
                            Object obj = getMethod.invoke(thisAnnotation);
                            if (obj instanceof FSArray) {
                                value = serilizeFSArray((FSArray) obj);
                            } else {
                                value = obj + "";
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                    switch (featureName) {
                        case "Annotator":
                            record.addCell("ANNOTATOR", this.annotator);
                        default:
                            if (value != null && value.trim().length() > 0) {
                                sb.append(featureName + ": " + value);
//                sb.append(value);
                                sb.append("\n");
                            }
                    }
                }
                record.addCell("FEATURES", sb.toString());
            } else {
                record.addCell("FEATURES", "");
                record.addCell("TEXT", "");
            }

            if (thisAnnotation instanceof ConceptBASE) {

                Object sentenceIdObject = sentenceTree.get(new Interval1D(thisAnnotation.getBegin(), thisAnnotation.getEnd()));
                String sentence;

                Sentence sentenceAnno;
                int sentenceBegin, sentenceEnd;
                if (sentenceIdObject != null) {
                    int sentenceId = (Integer) sentenceIdObject;
                    sentenceAnno = sentenceList.get(sentenceId);
                    sentence = sentenceAnno.getCoveredText();

                    if (sentence.length() < 50) {
                        sentenceBegin = sentenceList.get(sentenceId == 0 ? sentenceId : sentenceId - 1).getBegin();
                        sentenceEnd = sentenceList.get(sentenceId == sentenceList.size() - 1 ? sentenceId : sentenceId + 1).getEnd();
                    } else {
                        sentenceBegin = sentenceAnno.getBegin();
                        sentenceEnd = sentenceAnno.getEnd();
                    }
                    sentence = docText.substring(sentenceBegin, sentenceEnd);

                } else {
                    int contBegin = thisAnnotation.getBegin() - 50;
                    int contEnd = thisAnnotation.getEnd() + 50;
                    if (contBegin < 0)
                        contBegin = 0;
                    if (contEnd > docText.length()) {
                        contEnd = docText.length();
                    }
                    sentence = docText.substring(contBegin, contEnd);

                    record.addCell("NOTE", record.getValueByColumnName("NOTE") + "<missed sentence>");
                    sentenceBegin = contBegin;
                }
                record.addCell("SNIPPET", sentence);
                record.addCell("SNIPPET_BEGIN", sentenceBegin);
                record.addCell("SNIPPET_END", sentenceBegin + sentence.length());
                record.addCell("BEGIN", thisAnnotation.getBegin() - sentenceBegin);
                record.addCell("END", thisAnnotation.getEnd() - sentenceBegin);
            } else if (annotationCls.getSimpleName().indexOf("Sentence") != -1) {
                Object sentenceIdObj = sentenceTree.get(new Interval1D(thisAnnotation.getBegin(), thisAnnotation.getEnd()));
                int sentenceId = -1;
                if (sentenceIdObj != null)
                    sentenceId = (int) sentenceIdObj;
                int snippetBegin;
                int snippetEnd = thisAnnotation.getEnd();
                if (sentenceId > 0)
                    snippetBegin = sentenceList.get(sentenceId - 1).getBegin();
                else
                    snippetBegin = thisAnnotation.getBegin();

                if (sentenceId < sentenceList.size() - 1)
                    snippetEnd = sentenceList.get(sentenceId + 1).getEnd();
                else
                    snippetEnd = thisAnnotation.getEnd();
                record.addCell("SNIPPET", docText.substring(snippetBegin, snippetEnd));
                record.addCell("SNIPPET_BEGIN", snippetBegin);
                record.addCell("SNIPPET_END", snippetEnd);
                record.addCell("BEGIN", thisAnnotation.getBegin() - snippetBegin);
                record.addCell("END", thisAnnotation.getEnd() - snippetBegin);
            } else {
                record.addCell("SNIPPET", thisAnnotation.getCoveredText());
                record.addCell("SNIPPET_BEGIN", thisAnnotation.getBegin());
                record.addCell("SNIPPET_END", thisAnnotation.getEnd());
                record.addCell("BEGIN", 0);
                record.addCell("END", thisAnnotation.getEnd() - thisAnnotation.getBegin());
            }
            if (maxSnippetLength == 0)
                record.addCell("SNIPPET", "");
            else if (maxSnippetLength > 0) {
                int spanLength = (int) record.getValueByColumnName("END") - (int) record.getValueByColumnName("BEGIN");
                if (spanLength > maxSnippetLength) {
                    record.addCell("BEGIN", 0);
                    record.addCell("END", maxSnippetLength);
                    record.addCell("SNIPPET", record.getStrByColumnName("SNIPPET").substring(0, maxSnippetLength));
                    record.addCell("SNIPPET_END", maxSnippetLength);
                } else if (spanLength < maxSnippetLength) {
                    int snippetLength = (int) record.getValueByColumnName("SNIPPET_END") - (int) record.getValueByColumnName("SNIPPET_BEGIN");
                    if (snippetLength > maxSnippetLength) {
                        int totalCut = snippetLength - maxSnippetLength;
                        int equalCuit = totalCut / 2;
                        int leftSide = (int) record.getValueByColumnName("BEGIN");
                        int rightSide = (int) record.getValueByColumnName("SNIPPET_END") - (int) record.getValueByColumnName("SNIPPET_BEGIN") - (int) record.getValueByColumnName("END");
                        int leftCut = equalCuit;
                        int rightCut = equalCuit;
                        if (leftSide < equalCuit) {
                            leftCut = leftSide;
                            if (rightSide > equalCuit) {
                                rightCut = totalCut - leftSide;
                            } else {
                                rightCut = rightSide;
                            }
                        } else {
                            if (rightSide > equalCuit) {
                                rightCut = totalCut - leftCut;
                            } else {
                                rightCut = rightSide;
                                leftCut = totalCut - rightCut;
                            }
                        }
                        String snippet = record.getStrByColumnName("SNIPPET");
                        if (leftCut < 0) {
                            System.out.println(record);
                        }
                        record.addCell("BEGIN", (int) record.getValueByColumnName("BEGIN") - leftCut);
                        record.addCell("END", (int) record.getValueByColumnName("END") - leftCut);
                        record.addCell("SNIPPET_BEGIN", (int) record.getValueByColumnName("SNIPPET_BEGIN") + leftCut);
                        record.addCell("SNIPPET_END", (int) record.getValueByColumnName("SNIPPET_END") - rightCut);
                        snippet = snippet.substring(leftCut, leftCut + (int) record.getValueByColumnName("SNIPPET_END") - (int) record.getValueByColumnName("SNIPPET_BEGIN"));

//                        System.out.println(snippet.length());
                        record.addCell("SNIPPET", snippet);
                    }
                }
            }
            dao.insertRecord(tableName, record);
//            if (thisAnnotation instanceof Bunch_Base) {
//                System.out.println(record.getStrByColumnName(bunchColumnName) + ":" + record.getStrByColumnName("Note"));
//            }

            total++;
        }
        return total;
    }

    private String serilizeFSArray(FSArray ary) {
        StringBuilder sb = new StringBuilder();
        int size = ary.size();
        String[] values = new String[size];
        ary.copyToArray(0, values, 0, size);
        for (FeatureStructure fs : ary) {
            List<Feature> features = fs.getType().getFeatures();
            for (Feature feature : features) {
                String domain = feature.getDomain().getShortName();
                if (domain.equals("AnnotationBase") || domain.equals("Annotation"))
                    continue;
                Type range = feature.getRange();
                if (!range.isPrimitive()) {
                    FeatureStructure child = fs.getFeatureValue(feature);
                    sb.append(child + "");
                } else {
                    sb.append("\t" + feature.getShortName() + ":" + fs.getFeatureValueAsString(feature) + "\n");
                }
            }

        }
        return sb.toString();
    }


    public void collectionProcessComplete() {
        if (supportBunchAnno) {
            while (bunchRecordRows.size() > 0)
                saveBunchAnnotations();
        }
        previousBunchId = -1;
        dao.endBatchInsert();
    }

    private String getFeatures(Annotation thisAnnotation) {
        String value;
        StringBuilder sb = new StringBuilder();
        for (Feature feature : thisAnnotation.getType().getFeatures()) {
            String domain = feature.getDomain().getShortName();
            if (domain.equals("AnnotationBase") || domain.equals("Annotation"))
                continue;
            String featureName = feature.getShortName();
            value = thisAnnotation.getFeatureValueAsString(feature);
            if (value != null && value.trim().length() > 0) {
                sb.append(featureName + ": " + value);
//                sb.append(value);
                sb.append("\n");
            }
        }
        value = sb.toString();
        return value;
    }

    private Object readConfigureObject(UimaContext cont, String parameterName, Object defaultValue) {
        Object tmpObj = cont.getConfigParameterValue(parameterName);
        if (tmpObj == null) {
            if (defaultValue == null) {
                throw new UIMA_IllegalArgumentException("parameter not set", new Object[]{parameterName});
            } else {
                tmpObj = defaultValue;
            }
        }
        return tmpObj;
    }

    private String readConfigureString(UimaContext cont, String parameterName, String defaultValue) {
        String value = readConfigureObject(cont, parameterName, defaultValue) + "";
        return value.trim();
    }

}

