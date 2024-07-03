
package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.Bunch_Base;
import edu.utah.bmi.nlp.type.system.Sentence;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.DEFAULT_BUNCH_TYPE1;
import static edu.utah.bmi.nlp.core.DeterminantValueSet.checkNameSpace;
import static edu.utah.bmi.nlp.uima.ae.DocInferenceAnnotator.*;

/**
 * Allows the inference based on multiple document types.
 * Rule format:
 * Question name    Bunch Conclusion Type   Evidence Type 1, Evidence Type 2...
 */
public class BunchMixInferencer extends JCasAnnotator_ImplBase implements RuleBasedAEInf, BunchInferInterface {
    public static Logger logger = IOUtil.getLogger(BunchMixInferencer.class);
    public static final String PARAM_BUNCH_COLUMN_NAME = "BunchColumnName";
    @ConfigurationParameter(name = PARAM_BUNCH_COLUMN_NAME, mandatory = false, defaultValue = "BUNCH_ID",
            description = "The name of the column to hold the bunch ids.")
    protected String bunchColumnName;

    public static final String PARAM_DOCID_COLUMN_NAME = "DocIdColumnName";
    @ConfigurationParameter(name = PARAM_DOCID_COLUMN_NAME, mandatory = false, defaultValue = "DOC_ID",
            description = "The name of the column to hold the bunch ids.")
    protected String docIdColumnName;


    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    @ConfigurationParameter(name = DeterminantValueSet.PARAM_RULE_STR, mandatory = true,
            description = "The inferencer's rule definition (either a rule string or a path to a rule file).")
    protected String inferenceStr;

    public static final String PARAM_ANNO_POSITION = "AnnotatePosition";
    @ConfigurationParameter(name = PARAM_ANNO_POSITION, mandatory = false, defaultValue = FIRSTWORD,
            description = "The position to hold the inferencer's conclusion")
    protected String annotatePosition;

    public static final String PARAM_SAVE_EVIDENCES = "SaveEvidences";
    @ConfigurationParameter(name = PARAM_SAVE_EVIDENCES, mandatory = false, defaultValue = "false",
            description = "Whether save the evidences that lead to the conclusion.")
    protected boolean saveEvidences;


    public static final String PARAM_CLS_HOLD_BUNCH_QUEUE = "ClassHoldBunchQueue";
    @ConfigurationParameter(name = PARAM_CLS_HOLD_BUNCH_QUEUE, mandatory = false, defaultValue = "",
            description = "AE class name to hold the bunch of documents conclusions in queue")
    protected String bunchQueueClassName;

    public static final String PARAM_COPY_TREND_FEATURE = "CopyTrendFeature";
    @ConfigurationParameter(name = PARAM_COPY_TREND_FEATURE, mandatory = false, defaultValue = "",
            description = "Whether copy the trend pattern ae's feature (how the trend conclusion is made).")
    protected Boolean copyTrendFeature;

    //                          topic   rules
    protected LinkedHashMap<String, ArrayList<ArrayList<Object>>> inferenceMap = new LinkedHashMap<>();
    protected HashMap<String, String> defaultBunchTypes = new HashMap<>();
    //                   type name, Type
    protected HashMap<String, Type> typeMap = new HashMap<>();
    //     type counter
    protected HashMap<String, Integer> typeCounter = new HashMap<>();

    protected HashMap<String, String> trendMatchEvidences = new HashMap<>();

    //  a bunch can be used to represent an encounter or a patient that have a bunch of documents bundled with.
    protected int previousBunchId = -1;

    protected RecordRow previousRecordRow = null;

    protected ArrayList<ArrayList<String>> ruleCells = new ArrayList<>();
    protected HashMap<Integer, ArrayList<String>> ruleStore = new HashMap<>();

    protected LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();
    protected Pattern pattern = Pattern.compile("^\\s*(\\w+)");

    public static LinkedBlockingQueue<RecordRow> bunchRecordRows = new LinkedBlockingQueue<>();

    public static LinkedBlockingQueue<RecordRow> srcBunchRecordRows = new LinkedBlockingQueue<>();


    public void initialize(UimaContext cont) throws ResourceInitializationException {
        init(cont);
        if (bunchQueueClassName.length() > 0) {
            if (!bunchQueueClassName.contains(".")) {
                bunchQueueClassName = "edu.utah.bmi.nlp.uima.ae." + bunchQueueClassName;
            }
            try {
                Class bunchQueueCls = AnnotationOper.getTypeClass(bunchQueueClassName);
                Field bunchRecordRowsField = bunchQueueCls.getDeclaredField("bunchRecordRows");
                Object value = bunchRecordRowsField.get(null);
                srcBunchRecordRows = (LinkedBlockingQueue<RecordRow>) value;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        parseRuleStr(inferenceStr, defaultBunchTypes, inferenceMap, typeCounter, ruleStore);
//        this.typeDefinitions = getTypeDefs(inferenceStr);
    }

    protected void init(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
    }


    public static void parseRuleStr(String ruleStr, HashMap<String, String> defaultBunchType,
                                    LinkedHashMap<String, ArrayList<ArrayList<Object>>> inferenceMap,
                                    HashMap<String, Integer> typeCounter,
                                    HashMap<Integer, ArrayList<String>> ruleStore) {
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        for (ArrayList<String> initRow : ioUtil.getInitiations()) {
            if (initRow.get(1).endsWith("DefaultBunchConclusion") || initRow.get(1).endsWith(DEFAULT_BUNCH_TYPE1.substring(1))) {
                String topic = initRow.get(2).trim();
                String defaultDocTypeName = initRow.get(3).trim();
                defaultBunchType.put(topic, defaultDocTypeName);
            }
        }
        for (ArrayList<String> row : ioUtil.getRuleCells()) {
            if (row.size() < 4) {
                System.err.println("Format error in the visit inference rule " + row.get(0) + "." +
                        "\n\t" + row);
            }
            int ruleId = Integer.parseInt(row.get(0));
            ruleStore.put(ruleId, row);
            String topic = row.get(1).trim();
            if (!inferenceMap.containsKey(topic))
                inferenceMap.put(topic, new ArrayList<>());
            ArrayList<Object> inference = new ArrayList<>();
//            0. ruleId; 1. bunch question name (topic); 2. bunch conclusion type; 2. evidence types;
//			add visit conclusion type
            inference.add(ruleId);
            String visitTypeName = row.get(2).trim();
            inference.add(visitTypeName);
            String evidenceDocTypes = row.get(3).trim();
//            save the value space for future counting support
            HashMap<String, Integer> evidencesMap = new HashMap<>();
            for (String evidenceDocType : evidenceDocTypes.split("[,;\\|]")) {
                evidenceDocType = evidenceDocType.trim();
                evidencesMap.put(evidenceDocType, 1);
                typeCounter.put(evidenceDocType, 0);
            }
            inference.add(evidencesMap);
            inferenceMap.get(topic).add(inference);
        }

    }

    public void process(JCas jCas) throws AnalysisEngineProcessException {
        if (logger.isLoggable(Level.FINEST)) {
            if (bunchRecordRows.size() > 0) {
                RecordRow recordRow = bunchRecordRows.peek();
                logger.finest("Previous: " + recordRow.getStrByColumnName(bunchColumnName) + "\tDocId: " + recordRow.getValueByColumnName(docIdColumnName));
                recordRow = AnnotationOper.deserializeDocSrcInfor(jCas);
                logger.finest("\tCurrent: " + recordRow.getStrByColumnName(bunchColumnName) + "\tDocId: " + recordRow.getValueByColumnName(docIdColumnName));
            } else {
                RecordRow recordRow = AnnotationOper.deserializeDocSrcInfor(jCas);
                logger.finest("\tCurrent: " + recordRow.getStrByColumnName(bunchColumnName) + "\tDocId: " + recordRow.getValueByColumnName(docIdColumnName));
            }
        }
        CAS cas = jCas.getCas();
        if (typeMap.size() == 0) {
            initMaps(cas);
        }
        String serializedString;
        RecordRow recordRow = AnnotationOper.deserializeDocSrcInfor(jCas);
        Object value = recordRow.getValueByColumnName(bunchColumnName);
        int currentBunchId = value == null ? 0 : Integer.parseInt(value.toString());
        if (recordRow.getValueByColumnName(docIdColumnName).equals("0")) {
            recordRow.addCell("COMMENTS", "NO NOTE");
        }
        addSnippetInfos(jCas, recordRow);

        logger.finest(this.getClass().getSimpleName()+": "+previousBunchId + "****" + currentBunchId);
        if (previousBunchId == -1) {
            previousBunchId = currentBunchId;
            clearCounter();
        } else if (previousBunchId != currentBunchId) {
            if(logger.isLoggable(Level.FINEST) && ( currentBunchId==2580 || currentBunchId==2581 || currentBunchId==2582)){
                logger.finest("srcBunchRecordRows:"+srcBunchRecordRows.size());
                logger.finest("trendMatchEvidences:"+trendMatchEvidences);
            }
            if (srcBunchRecordRows.size() > 0) {
                updateTypeCounterThroughUpstreamBunchInferencer(srcBunchRecordRows, bunchColumnName, previousBunchId, typeMap, typeCounter, trendMatchEvidences);
            }
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
            previousBunchId = currentBunchId;
            clearCounter();
        }


        if (typeMap.size() == 0) {
            initMaps(cas);
        }

        for (String typeName : typeCounter.keySet()) {
            Iterator<AnnotationFS> iter = CasUtil.iterator(cas, typeMap.get(typeName));
            while (iter.hasNext()) {
                iter.next();
                if (typeCounter.containsKey(typeName))
                    typeCounter.put(typeName, typeCounter.get(typeName) + 1);
                else
                    typeCounter.put(typeName, 1);

            }

        }
        previousRecordRow = recordRow;
    }

    protected void addSnippetInfos(JCas jCas, RecordRow recordRow) {
        Span position = getAnnotationPosition(jCas);
        recordRow.addCell("BEGIN", position.getBegin());
        recordRow.addCell("END", position.getEnd());
        recordRow.addCell("SNIPPET_BEGIN", position.getBegin());
        String text = jCas.getDocumentText();
        text = text.substring(position.getBegin(), position.getEnd());
        recordRow.addCell("SNIPPET", text);
//      TODO need to check if it works when text column name is customized
        recordRow.addCell("TEXT", text);

    }

    protected void evaluateVisitCounts(RecordRow previousRecordRow, HashMap<String, Integer> typeCounter,
                                       HashMap<String, String> trendMatchEvidences) {

        for (String topic : inferenceMap.keySet()) {
            ArrayList<ArrayList<Object>> rules = inferenceMap.get(topic);
            boolean matched = true;
            for (ArrayList<Object> rule : rules) {
                HashMap<String, Integer> evidencesMap = (HashMap<String, Integer>) rule.get(2);
                matched = true;
                StringBuilder sb = new StringBuilder();
                for (String typeName : evidencesMap.keySet()) {
                    if (!this.typeCounter.containsKey(typeName) || this.typeCounter.get(typeName) < evidencesMap.get(typeName))
                        matched = false;
                    if (trendMatchEvidences.containsKey(typeName)) {
                        sb.append(trendMatchEvidences.get(typeName) + "\n");
                    }
                }
                if (matched) {
                    addBunchConclusion(previousRecordRow, (String) rule.get(1), ruleStore.get((int) rule.get(0)).get(3), sb.toString());
                    break;
                }
            }
            if (!matched) {
                addBunchConclusion(previousRecordRow, defaultBunchTypes.get(topic), "", "");
            }
        }

    }

    protected void addBunchConclusion(RecordRow previousRecordRow, String typeName, String evidence, String trendFeatures) {

        previousRecordRow.addCell("TYPE", typeName);
        if (saveEvidences) {
            String evidences = evidence.replaceAll(",", "+").trim();
            if (evidences.length() > 0)
                previousRecordRow.addCell("FEATURES", "Note: " + evidences);
            if (this.copyTrendFeature && trendFeatures.length() > 0) {
                previousRecordRow.addCell("FEATURES", "Note: " + evidences + "\nTrend" + trendFeatures.toString());
            }
            logger.finest("Add bunch conclusion: "
                    + previousRecordRow.getValueByColumnName(bunchColumnName)
                    + "\t" + previousRecordRow.getValueByColumnName("TYPE")
                    + "\t" + previousRecordRow.getValueByColumnName("FEATURES"));
        } else {
            logger.finest("Add bunch conclusion: " + previousRecordRow.getValueByColumnName(bunchColumnName)
                    + "\t" + previousRecordRow.getValueByColumnName("TYPE"));
        }
        bunchRecordRows.add(previousRecordRow.clone());
    }

    protected Span getAnnotationPosition(JCas jCas) {
        Annotation anno = null;
        Span span = null;
        switch (annotatePosition) {
            case FIRSTWORD:
                anno = JCasUtil.selectByIndex(jCas, Token.class, 0);
                break;
            case FIRSTSENTENCE:
                anno = JCasUtil.selectByIndex(jCas, Sentence.class, 0);
                break;
            case LASTWORD:
                anno = JCasUtil.selectByIndex(jCas, Token.class, -1);
                break;
            case LASTSENTENCE:
                anno = JCasUtil.selectByIndex(jCas, Sentence.class, -1);
                break;
            default:
//                check if the configured value is a type name or a snippet of text
                if (annotatePosition.startsWith("edu.") || annotatePosition.startsWith("org.")
                        || annotatePosition.startsWith("com.") || annotatePosition.startsWith("net.")) {
                    anno = JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(annotatePosition), 0);
                } else {
                    String docText = jCas.getDocumentText();
                    int begin = docText.indexOf(annotatePosition);
                    int end = begin + annotatePosition.length();
                    span = new Span(begin, end);
                }
                break;
        }
        if (anno != null)
            return new Span(anno.getBegin(), anno.getEnd());
        else {
            String txt = jCas.getDocumentText();
            Matcher matched = pattern.matcher(txt);
            if (matched.find()) {
                span = new Span(matched.start(1), matched.end(1));
            }
            return span;
        }
    }

    /**
     * Initiate typeMap and featureMap, so that Type and Feature can be easier and faster called
     *
     * @param cas UIMA CAS object
     */
    protected void initMaps(CAS cas) {
        for (String typeName : typeCounter.keySet()) {
            Type type = CasUtil.getAnnotationType(cas, DeterminantValueSet.checkNameSpace(typeName));
            if (!typeMap.containsKey(typeName))
                typeMap.put(typeName, type);
        }
    }

    protected void clearCounter() {
        for (String typeName : typeCounter.keySet()) {
            typeCounter.put(typeName, 0);
        }
        this.trendMatchEvidences.clear();
    }

    protected void updateTypeCounterThroughUpstreamBunchInferencer(LinkedBlockingQueue<RecordRow> srcBunchRecordRows,
                                                                   String bunchColumnName, int previousBunchId,
                                                                   HashMap<String, Type> typeMap,
                                                                   HashMap<String, Integer> typeCounter, HashMap<String, String> trendMatchEvidences) {
        RecordRow srcBunchRecordRow = srcBunchRecordRows.peek();
        if (srcBunchRecordRow.getStrByColumnName(bunchColumnName) == null) {
            logger.warning("Null  " + bunchColumnName + " " + srcBunchRecordRow);
        }
        if (Integer.parseInt(srcBunchRecordRow.getStrByColumnName(bunchColumnName)) != previousBunchId){
            logger.warning("The bunch_id in the stack: "+Integer.parseInt(srcBunchRecordRow.getStrByColumnName(bunchColumnName)) +
                    "does not match with previous processed record: "+ previousBunchId);
//          TODO this might be caused by skipped clean steps while no note processed.
            srcBunchRecordRows.clear();
            return;
        }
        try {
            while (srcBunchRecordRow != null && Integer.parseInt(srcBunchRecordRow.getStrByColumnName(bunchColumnName)) == previousBunchId) {
                srcBunchRecordRow = srcBunchRecordRows.poll();
                String typeName = srcBunchRecordRow.getStrByColumnName("TYPE");
                if (typeMap.containsKey(typeName)) {
                    if (typeCounter.containsKey(typeName)) {
                        typeCounter.put(typeName, typeCounter.get(typeName) + 1);
                    } else
                        typeCounter.put(typeName, 1);
                    if (this.copyTrendFeature && srcBunchRecordRow.getValueByColumnName("FEATURES") != null)
                        trendMatchEvidences.put(typeName, srcBunchRecordRow.getStrByColumnName("FEATURES"));
                }
                srcBunchRecordRow = srcBunchRecordRows.peek();
            }
        } catch (Exception e) {
            System.err.println(srcBunchRecordRow);
            System.err.println(previousBunchId);
            e.printStackTrace();
        }


    }

    public void collectionProcessComplete() {
        if (previousBunchId != -1 && previousRecordRow != null) {
            if (srcBunchRecordRows.size() > 0) {
                updateTypeCounterThroughUpstreamBunchInferencer(srcBunchRecordRows, bunchColumnName, previousBunchId, typeMap, typeCounter, trendMatchEvidences);

            }
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
        }
    }

    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        return getTypeDefs(ioUtil);
    }

    protected LinkedHashMap<String, TypeDefinition> getTypeDefs(IOUtil ioUtil) {
        if (ioUtil.getInitiations().size() > 0) {
            UIMATypeFunctions.getTypeDefinitions(ioUtil, ruleCells,
                    new HashMap<>(), new HashMap<>(), defaultBunchTypes, typeDefinitions);
        }
        for (ArrayList<String> row : ioUtil.getRuleCells()) {
            logger.finest("Parse rule:" + row);
            String bunchTypeName = checkNameSpace(row.get(2).trim());
            String shortName = DeterminantValueSet.getShortName(bunchTypeName);
            if (!typeDefinitions.containsKey(shortName))
                typeDefinitions.put(shortName, new TypeDefinition(bunchTypeName, Bunch_Base.class.getCanonicalName()));
        }
        for (String defaultType : defaultBunchTypes.values()) {
            if (!typeDefinitions.containsKey(defaultType))
                typeDefinitions.put(defaultType, new TypeDefinition(defaultType, Bunch_Base.class.getCanonicalName()));
        }

        return typeDefinitions;

    }
}
