package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.AnnotationDefinition;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.joda.time.DateTime;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Based on the documents conclusions changing patterns, inference visit/patient level conclusions, add conclusion to
 * the last document of the corresponding bunch/visit (does not write results to db).
 * Split based on first occurrence of a document type (e.g. SSI_Doc)--- so, all docs split into two periods of time.
 */
public class TrendInferencer extends BunchMixInferencer {
    public static Logger logger = IOUtil.getLogger(TrendInferencer.class);


    public static final String PARAM_REFERENCE_DATE_COLUMN_NAME = "ReferenceDateColumnName";
    @ConfigurationParameter(name = PARAM_REFERENCE_DATE_COLUMN_NAME, mandatory = false, defaultValue = "REF_DTM",
            description = "The name of the column to hold the reference date.")
    protected String referenceDateColumnName;


    public static final String PARAM_RECORD_DATE_COLUMN_NAME = "RecordDateColumnName";
    @ConfigurationParameter(name = PARAM_RECORD_DATE_COLUMN_NAME, mandatory = false, defaultValue = "DATE",
            description = "The name of the column to hold the record date.")
    protected String recordDateColumnName;

    public static final String PARAM_DOCID_COLUMN_NAME = "DocIdColumnName";
    @ConfigurationParameter(name = PARAM_DOCID_COLUMN_NAME, mandatory = false, defaultValue = "DOC_ID",
            description = "The name of the column to hold the bunch ids.")
    protected String docIdColumnName;

    protected HashMap<String, Class<? extends Annotation>> conceptClassMap = new HashMap<>();

    protected LinkedHashSet<String> allEvidenceTypes = new LinkedHashSet<>();

    protected LinkedHashMap<DateTime, ArrayList<String>> stacker = new LinkedHashMap<>();

    //  pos of the split --- type name --- counts
    protected ArrayList<LinkedHashMap<String, Integer>> trajectoryTypeCounter = new ArrayList<>();

    //    topic--> split doc type
    protected LinkedHashMap<String, String> splitDocTypes = new LinkedHashMap<>();

    //     type name to calculate frequency ---types to count as the denominator (in string) --- types to count as the denominator (in array)
    protected LinkedHashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<String, Integer>>> counterDenominators = new LinkedHashMap<>();

    public static LinkedBlockingQueue<RecordRow> bunchRecordRows = new LinkedBlockingQueue<>();
    protected int previousBunchId = -1;
    protected RecordRow previousRecordRow = null;


    public void initialize(UimaContext cont) throws ResourceInitializationException {
//      define paragraph splitter
        init(cont);
        parseRuleStr(inferenceStr);
    }

    protected void parseRuleStr(String ruleStr) {
        inferenceMap.clear();
//        conceptClassMap.clear();
//        docTypeConstructorMap.clear();
//        evidenceConceptGetFeatures.clear();
//        conclusionConceptSetFeatures.clear();
        counterDenominators.clear();
        trajectoryTypeCounter.clear();
        typeDefinitions = getTypeDefs(ruleStr);
        IOUtil ioUtil = new IOUtil(ruleStr);
        for (ArrayList<String> initRow : ioUtil.getInitiations()) {
            String topic = initRow.get(1).substring(1).trim();
            String defaultDocTypeName = initRow.get(2).trim();
//            buildConstructor(defaultDocTypeName);
            if (topic.equals(DeterminantValueSet.CONCEPT_FEATURES1.substring(1)) ||
                    topic.equals(DeterminantValueSet.FEATURE_VALUES1.substring(1)))
                continue;
            if (topic.equals(DeterminantValueSet.DEFAULT_BUNCH_TYPE1.substring(1))) {
                defaultBunchTypes.put(defaultDocTypeName, initRow.get(3).trim());
            } else
                defaultBunchTypes.put(topic, defaultDocTypeName);
        }
        for (ArrayList<String> row : ioUtil.getRuleCells()) {

            String topic = row.get(1).trim();
            if (!inferenceMap.containsKey(topic))
                inferenceMap.put(topic, new ArrayList<>());
            ArrayList<Object> inference = new ArrayList<>();
//			add doc conclusion type
            String docTypeName = row.get(2).trim();
            inference.add(docTypeName);
//            buildConstructor(docTypeName);
//			add evidences
            ArrayList<Class> evidences = new ArrayList<>();
            String[] evidenceTypeNames = row.get(4).split(",");

//          init counter map
            LinkedHashMap<String, Integer> counter = new LinkedHashMap<>();
            int length = evidenceTypeNames.length;
            if (!counterDenominators.containsKey(length)) {
                counterDenominators.put(length, new LinkedHashMap<>());
            }
            for (int i = 0; i < length; i++) {
                if (!counterDenominators.get(length).containsKey(i))
                    counterDenominators.get(length).put(i, new LinkedHashMap<>());
                counterDenominators.get(length).get(i).put(row.get(5), 0);
            }
            for (String typeName : row.get(5).split(",")) {
                counter.put(typeName, 0);
                allEvidenceTypes.add(typeName);
            }
            if (trajectoryTypeCounter.size() == 0)
                for (int i = 0; i < evidenceTypeNames.length; i++) {
//                  each split segment will need a separate counter
                    trajectoryTypeCounter.add((LinkedHashMap<String, Integer>) counter.clone());
                }
            for (String evidenceTypeName : evidenceTypeNames) {
                Class evidenceType;
                if (!conceptClassMap.containsKey(evidenceTypeName)) {
                    evidenceType = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(evidenceTypeName));
                    if (evidenceType == null) {
                        logger.warning("Type: " + evidenceTypeName + " has not been defined before using as an evidence.");
                        logger.warning("Rule: " + row + " skipped.");
                        continue;
                    }
                    conceptClassMap.put(evidenceTypeName, evidenceType);

                } else {
                    evidenceType = conceptClassMap.get(evidenceTypeName);
                }
                evidences.add(evidenceType);
            }
//			add feature reader

            String featureSetting = row.get(3).trim();

            inference.add(getFeatureSettings(featureSetting, typeDefinitions.get(row.get(2))));


            inference.add(evidenceTypeNames);

//            add denominator
            inference.add(row.get(5));
            inferenceMap.get(topic).add(inference);
        }

    }

    protected AnnotationDefinition getFeatureSettings(String featureSetting, TypeDefinition typeDefinition) {
        AnnotationDefinition annotationDefinition = new AnnotationDefinition(typeDefinition);
        for (String featureValueStr : featureSetting.split(",")) {

            featureValueStr = featureValueStr.trim();
            String[] featureValuePair = featureValueStr.split(":");
            annotationDefinition.setFeatureValue(featureValuePair[0], featureValuePair[1]);
        }
        return annotationDefinition;
    }


    public void process(JCas jCas) throws AnalysisEngineProcessException {
        String serializedString;
        RecordRow recordRow = AnnotationOper.deserializeDocSrcInfor(jCas);


        Object value = recordRow.getValueByColumnName(bunchColumnName);
        int currentBunchId = value == null ? 0 : Integer.parseInt(value.toString());
        addSnippetInfos(jCas, recordRow);
        String docId=recordRow.getStrByColumnName(docIdColumnName);

        value = recordRow.getStrByColumnName(recordDateColumnName);
        if (value == null) {
            logger.warning("Not document date from column: '" + recordDateColumnName + "' is found in metadata");
            return;
        }
        DateTime recordDate = new DateTime(new org.pojava.datetime.DateTime(value.toString()).toTimestamp());

        if (previousBunchId == -1) {
            previousBunchId = currentBunchId;
            previousRecordRow = recordRow;
            clearCounter();
            previousBunchId = currentBunchId;
            DateTime referenceDate = getReferenceDate(recordRow, referenceDateColumnName);
            stacker.put(referenceDate, new ArrayList<>());
        } else if (previousBunchId != currentBunchId) {
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
            clearCounter();
            previousBunchId = currentBunchId;
            DateTime referenceDate = getReferenceDate(recordRow, referenceDateColumnName);
            stacker.put(referenceDate, new ArrayList<>());
        }
        if (!stacker.containsKey(recordDate))
            stacker.put(recordDate, new ArrayList<>());

        for (String evidenceTypeName : allEvidenceTypes) {
            Iterator<? extends Annotation> iter = JCasUtil.iterator(jCas, conceptClassMap.get(evidenceTypeName));
            if (iter.hasNext()) {
                Annotation anno = iter.next();
                stacker.get(recordDate).add(anno.getType().getShortName());
                logger.finest(recordDate + "\t"+currentBunchId+"-"+docId+" ->\t" + anno.getType().getShortName());
            }
        }
        previousRecordRow = recordRow;

    }


    public void collectionProcessComplete() {
        if (previousBunchId != -1 && previousRecordRow != null) {
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
            clearCounter();
            DateTime referenceDate = getReferenceDate(previousRecordRow, referenceDateColumnName);
            stacker.put(referenceDate, new ArrayList<>());
        }
    }


    private DateTime getReferenceDate(RecordRow recordRow, String columnName) {
        Object value = recordRow.getStrByColumnName(columnName);
        if (value == null) {
            logger.warning("Not date column: '" + columnName + "' is found in metadata");
            return null;
        }
        DateTime referenceDate = new DateTime(new org.pojava.datetime.DateTime(value.toString()).toTimestamp());
        return referenceDate;
    }

    protected void clearCounter() {
        for (LinkedHashMap<String, Integer> count : trajectoryTypeCounter) {
            for (String typeName : count.keySet()) {
                count.put(typeName, 0);
            }
        }
        stacker.clear();
    }


    protected void evaluateVisitCounts(RecordRow previousRecordRow, HashMap<String, Integer> typeCounter, HashMap<String, String> trendMatchEvidences) {
        logger.finest("Analyze the trend of BUNCH_ID:" + previousRecordRow.getStrByColumnName(bunchColumnName));
        LinkedHashMap<Integer, ArrayList<HashSet<String>>> trajectories = groupCounts(trajectoryTypeCounter, stacker);
        ArrayList<AnnotationDefinition> conclusions = identifyTrajectory(trajectories);
        if (conclusions.size() > 0)
            for (AnnotationDefinition conclusion : conclusions) {
//            addConclusion(jCas, conclusion);
                previousRecordRow.addCell("TYPE", conclusion.getShortTypeName());
                previousRecordRow.addCell("FEATURES", "Note: " + conclusion.getFeatureValue("Note"));
                bunchRecordRows.add(previousRecordRow.clone());
            }
        else {
            previousRecordRow.addCell("TYPE", defaultBunchTypes.values().iterator().next());
            bunchRecordRows.add(previousRecordRow.clone());
        }
    }

//    private void addConclusion(JCas jCas, AnnotationDefinition conclusion) {
//        Span span = getAnnotationPosition(jCas);
//        Annotation anno = AnnotationOper.createAnnotation(jCas, conclusion, this.docTypeConstructorMap.get(conclusion.getShortTypeName()),
//                span.getBegin(), span.getEnd(),
//                this.conclusionConceptSetFeatures.get(this.conceptClassMap.get(conclusion.getShortTypeName())));
//        anno.addToIndexes();
//    }

    private ArrayList<AnnotationDefinition> identifyTrajectory(LinkedHashMap<Integer, ArrayList<HashSet<String>>> trajectories) {
        ArrayList<AnnotationDefinition> conclusions = new ArrayList<>();
        for (String topic : inferenceMap.keySet()) {
            ArrayList<ArrayList<Object>> rules = inferenceMap.get(topic);
            for (ArrayList<Object> rule : rules) {
                boolean matched = true;
                String[] typeNames = (String[]) rule.get(2);
                int length = typeNames.length;
                for (int i = 0; i < length; i++) {
                    String docTypeName = typeNames[i];
                    if (!trajectories.containsKey(length)) {
                        matched = false;
                        break;
                    }
//                    System.out.println(length+"\t<-->\t"+trajectories.get(length));
                    if (trajectories.get(length).size() <= i) {
                        logger.warning(bunchColumnName+":" + previousRecordRow.getValueByColumnName(bunchColumnName) + "\t"+docIdColumnName+":" + previousRecordRow.getValueByColumnName(docIdColumnName));
                        logger.warning("Try to access " + i + " in " + trajectories.get(length));
                        matched = false;
                        break;
                    }
                    if (!trajectories.get(length).get(i).contains(docTypeName)) {
                        matched = false;
                        break;
                    }

                }
                if (matched) {
                    logger.finest("Add trajectory conclusion\t->\t" + rule);
                    conclusions.add((AnnotationDefinition) rule.get(1));
                    break;
                }

            }
        }
        return conclusions;
    }


    protected LinkedHashMap<Integer, ArrayList<HashSet<String>>> groupCounts(ArrayList<LinkedHashMap<String, Integer>> trajectoryTypeCounter,
                                                                             LinkedHashMap<DateTime, ArrayList<String>> stacker) {
//      Sort docs on note datetime.
        List<Map.Entry<DateTime, ArrayList<String>>> sortedStack = stacker.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

        LinkedHashMap<Integer, ArrayList<HashSet<String>>> trajectories = new LinkedHashMap<>();
        for (String splitDocType : inferenceMap.keySet()) {
            int splitSectionId = 0;
            for (Map.Entry<DateTime, ArrayList<String>> entry : sortedStack) {
                ArrayList<String> typeNames = entry.getValue();
                if (typeNames.contains(splitDocType)) {
                    logger.finest("Split on " + entry.getKey());
                    splitSectionId = 1;
                }
                if (typeNames.size() > 0) {
                    DateTime docDateTime = entry.getKey();
                    for (String typeName : typeNames) {
                        trajectoryTypeCounter.get(splitSectionId).put(typeName, trajectoryTypeCounter.get(splitSectionId).get(typeName) + 1);
                    }
                }
            }
        }

        trajectories.put(0, new ArrayList<>());
        trajectories.put(1, new ArrayList<>());
        for (int i = 0; i < 2; i++) {
            HashMap<Integer, HashSet<String>> reverseMap = new HashMap<>();
            int max = 0;
            for (Map.Entry<String, Integer> entry : trajectoryTypeCounter.get(i).entrySet()) {
                int count = entry.getValue();
                if (count > max) {
                    max = entry.getValue();
                    if (!reverseMap.containsKey(count)) {
                        reverseMap.put(count, new HashSet<>());
                    }
                    reverseMap.get(count).add(entry.getKey());
                }
            }
            if (reverseMap.containsKey(max))
                trajectories.get(i).add(reverseMap.get(max));
            else
                logger.finest(reverseMap.keySet().toString());
        }
        if (logger.isLoggable(Level.FINEST)) {
            for (int splitId : trajectories.keySet()) {
                logger.finest("Trend " + splitId + ":\t" + trajectories.get(splitId));
            }
        }

        return trajectories;
    }

}
