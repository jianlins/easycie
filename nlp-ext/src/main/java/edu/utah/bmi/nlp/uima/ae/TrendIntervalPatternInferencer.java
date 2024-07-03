package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.AnnotationDefinition;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extended the functions of  TrendInferencer to allow specify the
 */
public class TrendIntervalPatternInferencer extends TrendPatternInferencer {
    public static Logger logger = IOUtil.getLogger(TrendIntervalPatternInferencer.class);
    public static final String ANY_PRESENCE = "ANY_PRESENCE", FIRST_PRESENCE = "FIRST_PRESENCE", LAST_PRESENCE = "LAST_PRESENCE";
    public static final String END = "<END>";
    public static LinkedBlockingQueue<RecordRow> bunchRecordRows = new LinkedBlockingQueue<>();

    public static LinkedBlockingQueue<RecordRow> srcBunchRecordRows = new LinkedBlockingQueue<>();

    public void initialize(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
        parseRuleStr(inferenceStr);
    }

    protected void parseRuleStr(String ruleStr) {
        patternMatcher.clear();
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
            int ruleId = Integer.parseInt(row.get(0));
            String topic = row.get(1).trim();
            if (!patternMatcher.containsKey(topic))
                patternMatcher.put(topic, new ArrayList<>());
            ruleStore.put(ruleId, row);
//			add doc conclusion type
            String docTypeName = row.get(2).trim();
//			add evidences
            String[] evidenceTypeNames = row.get(4).split(",");
            String compare=row.get(5);
            int hours= NumberUtils.createInteger(row.get(6));
            ArrayList<ArrayList<Object>> topicRules = patternMatcher.get(topic);
            ArrayList<Object> tmp = new ArrayList<>();
            for (String evidenceTypeName : evidenceTypeNames) {
                allEvidenceTypes.add(evidenceTypeName);
                Class evidenceType;
                if (!conceptClassMap.containsKey(evidenceTypeName)) {
                    evidenceType = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(evidenceTypeName));
                    if (evidenceType == null) {
                        logger.warning("Type: " + evidenceTypeName + " has not been defined before using as an evidence.");
                        logger.warning("Rule: " + row + " skipped.");
                        continue;
                    }
                    conceptClassMap.put(evidenceTypeName, evidenceType);

                }
                tmp.add(evidenceTypeName);
            }
//            tmp.add(END);
            tmp.add(compare);
            tmp.add(hours);
            tmp.add(ruleId);
            topicRules.add(tmp);
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
        if(logger.isLoggable(Level.FINEST)){
            logger.finest("\nProcess document: "+recordRow.toString().replaceAll("\n","\t"));
        }

        Object value = recordRow.getValueByColumnName(bunchColumnName);
        int currentBunchId = value == null ? 0 : Integer.parseInt(value.toString());

        value = recordRow.getStrByColumnName(recordDateColumnName);
        if (value == null || value.equals("NULL")) {
            logger.warning("Not document date from column: '" + recordDateColumnName + "' is found in metadata");
            return;
        }
        DateTime recordDate = new DateTime(new org.pojava.datetime.DateTime(value.toString()).toTimestamp());
        DateTime referenceDate = getReferenceDate(recordRow, referenceDateColumnName);
        if (previousBunchId == -1) {
            previousBunchId = currentBunchId;
            previousRecordRow = recordRow;
            clearCounter();
            previousBunchId = currentBunchId;
            previousReferenceDate = referenceDate;
        } else if (previousBunchId != currentBunchId) {
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
            clearCounter();
            previousBunchId = currentBunchId;
            previousReferenceDate = referenceDate;
        }

//        if (recordDate.isBefore(referenceDate.minusDays(1))) {
////            System.out.println(recordRow);
//            previousRecordRow = recordRow;
//            return;
//        }


        for (String evidenceTypeName : allEvidenceTypes) {
            Iterator<? extends Annotation> iter = JCasUtil.iterator(jCas, conceptClassMap.get(evidenceTypeName));
            if (iter.hasNext()) {
                Annotation anno = iter.next();
                if (!stacker.containsKey(recordDate))
                    stacker.put(recordDate, new LinkedHashSet<>());
                stacker.get(recordDate).add(anno.getType().getShortName());
                logger.finest(recordDate + "\t->\t" + anno.getType().getShortName());
            }
        }
        previousRecordRow = recordRow;

    }


    public void collectionProcessComplete() {
        if (previousBunchId != -1 && previousRecordRow != null) {
            evaluateVisitCounts(previousRecordRow, typeCounter, trendMatchEvidences);
            clearCounter();
            DateTime referenceDate = getReferenceDate(previousRecordRow, referenceDateColumnName);
            stacker.put(referenceDate, new LinkedHashSet<>());
        }else if (previousBunchId==-1){

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
        stacker.clear();
    }


    protected void evaluateVisitCounts(RecordRow previousRecordRow, HashMap<String, Integer> typeCounter, HashMap<Integer, LinkedHashMap<String, String>> trendMatchEvidences) {
        List<Map.Entry<DateTime, LinkedHashSet<String>>> sortedStack = sortStacker(matchPresence, stacker);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("After sorting document conclusions:");
            for (Map.Entry<DateTime, LinkedHashSet<String>> entry : sortedStack) {
                logger.finest(entry.getKey().toString() + "\t" + entry.getValue().toString());
            }
        }
        LinkedHashMap<String, Integer> conclusions = identifyPatterns(sortedStack);
        if (conclusions.size() > 0)
            for (int ruleId : conclusions.values()) {
                ArrayList<String> matchedRule = ruleStore.get(ruleId);
                previousRecordRow.addCell("TYPE", matchedRule.get(2));
                previousRecordRow.addCell("FEATURES", "Note: " + matchedRule.get(4).replaceAll(",", "->")+"("+matchedRule.get(0)+")");
                bunchRecordRows.add(previousRecordRow.clone());
            }
        else {
            previousRecordRow.addCell("TYPE", defaultBunchTypes.values().iterator().next());
            bunchRecordRows.add(previousRecordRow.clone());
        }
        if (logger.isLoggable(Level.FINE)) {
            ArrayList<RecordRow> buff = new ArrayList<>();
            buff.addAll(bunchRecordRows);
            for (RecordRow rec : buff) {
                logger.fine(rec.getStrByColumnName("TYPE"));
                logger.fine(rec.getStrByColumnName("FEATURES"));
            }
        }
    }

    //    TODO unit test
    protected ArrayList<Map.Entry<DateTime, LinkedHashSet<String>>> sortStacker(String matchPresence, LinkedHashMap<DateTime, LinkedHashSet<String>> stacker) {
        List<Map.Entry<DateTime, LinkedHashSet<String>>> sorted = stacker.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

        LinkedHashMap<String, Integer> locationMap = new LinkedHashMap<>();
        ArrayList<Map.Entry<DateTime, LinkedHashSet<String>>> condensed = new ArrayList<>();
        switch (matchPresence) {
            case LAST_PRESENCE:
                for (int i = 0; i < sorted.size(); i++) {
                    Map.Entry<DateTime, LinkedHashSet<String>> entry = sorted.get(i);
                    condensed.add(new HashMap.SimpleEntry<>(entry.getKey(), new LinkedHashSet<>()));
                    for (String typeName : entry.getValue()) {
                        if (locationMap.containsKey(typeName)) {
                            condensed.get(locationMap.get(typeName)).getValue().remove(typeName);
                        }
                        condensed.get(condensed.size() - 1).getValue().add(typeName);
                        locationMap.put(typeName, i);
                    }
                }
                condensed.removeIf(b -> b.getValue().size() == 0);
                break;

            case FIRST_PRESENCE:
                for (int i = 0; i < sorted.size(); i++) {
                    Map.Entry<DateTime, LinkedHashSet<String>> entry = sorted.get(i);
                    condensed.add(new HashMap.SimpleEntry<>(entry.getKey(), new LinkedHashSet<>()));
                    for (String typeName : entry.getValue()) {
                        if (!locationMap.containsKey(typeName)) {
                            condensed.get(condensed.size() - 1).getValue().add(typeName);
                            locationMap.put(typeName, i);
                        }
                    }
                }
                condensed.removeIf(b -> b.getValue().size() == 0);
                break;
            case ANY_PRESENCE:
                condensed.addAll(sorted);
                break;
        }
        return condensed;
    }

//    private void addConclusion(JCas jCas, AnnotationDefinition conclusion) {
//        Span span = getAnnotationPosition(jCas);
//        Annotation anno = AnnotationOper.createAnnotation(jCas, conclusion, this.docTypeConstructorMap.get(conclusion.getShortTypeName()),
//                span.getBegin(), span.getEnd(),
//                this.conclusionConceptSetFeatures.get(this.conceptClassMap.get(conclusion.getShortTypeName())));
//        anno.addToIndexes();
//    }

    protected LinkedHashMap<String, Integer> identifyPatterns(List<Map.Entry<DateTime, LinkedHashSet<String>>> sortStacker) {
        LinkedHashMap<String, Integer> conclusions = new LinkedHashMap<>();
        for (String topic : patternMatcher.keySet()) {
            for (ArrayList<Object> topicRule : patternMatcher.get(topic)) {
                findMatch(0, sortStacker, 0, topicRule, conclusions, new ArrayList<>());
            }
        }
        return conclusions;
    }

    protected void findMatch(int step, List<Map.Entry<DateTime, LinkedHashSet<String>>> sortStacker, int ruleEleId,
                             ArrayList<Object> topicRule,
                             LinkedHashMap<String, Integer> conclusion,  ArrayList<Object[]>matchedDocs) {
        if (ruleEleId == topicRule.size() - 3) {
            int ruleId = (int) topicRule.get(topicRule.size() -1);
            String topic = ruleStore.get(ruleId).get(1);
            if (matchedDocs.size()>1 && matchedDocs.get(0)[1] instanceof DateTime &&  matchedDocs.get(1)[1] instanceof DateTime) {
                Duration p = new Duration((DateTime) matchedDocs.get(0)[1], (DateTime) matchedDocs.get(1)[1]);
                int hours = ((Long) p.getStandardHours()).intValue();
                int threshold=(int)topicRule.get(topicRule.size()-2);
                switch ((String)topicRule.get(ruleEleId)){
                    case "<":
                        if (hours<threshold)
                            break;
                        else{
                            matchedDocs.clear();
                            logger.finest("no match found for "+topicRule);
                            return;
                        }
                    case ">":
                        if (hours>threshold)
                            break;
                        else{
                            matchedDocs.clear();
                            logger.finest("no match found for "+topicRule);
                            return;
                        }
                    default:
                        matchedDocs.clear();
                        logger.finest("no match found for "+topicRule);
                        return;
                }
                if (!conclusion.containsKey(topic)) {
                    conclusion.put(topic, ruleId);
                } else if (conclusion.get(topic) > ruleId) {
                    conclusion.put(topic, ruleId);
                }
                matchedDocs.clear();
            }
        } else {
            String ruleEleType = (String) topicRule.get(ruleEleId);
            for (int i = step; i < sortStacker.size(); i++) {
                DateTime dateTime = sortStacker.get(i).getKey();
                LinkedHashSet<String> docConclusionTypes = sortStacker.get(i).getValue();
                if (docConclusionTypes.contains(ruleEleType)) {
                    matchedDocs.add(new Object[]{ruleEleType, dateTime});
                    findMatch(i + 1, sortStacker, ruleEleId + 1, topicRule, conclusion, matchedDocs);
                }
            }
        }

    }

    protected void identifyPatterns(List<Map.Entry<DateTime, LinkedHashSet<String>>> sortStacker, int step,
                                    HashMap pm,
                                    LinkedHashMap<String, Integer> conclusion) {
        if (pm.containsKey(END)) {
            LinkedHashSet<Integer> ruleIds = (LinkedHashSet<Integer>) pm.get(END);
            for (int ruleId : ruleIds) {
                String topic = ruleStore.get(ruleId).get(1);
                if (!conclusion.containsKey(topic)) {
                    conclusion.put(topic, ruleId);
                } else if (conclusion.get(topic) > ruleId) {
                    conclusion.put(topic, ruleId);
                }
            }
        }
        for (int i = step; i < sortStacker.size(); i++) {
            Map.Entry<DateTime, LinkedHashSet<String>> entry = sortStacker.get(i);
            for (String docConclusionType : entry.getValue()) {
                if (pm.containsKey(docConclusionType)) {
                    identifyPatterns(sortStacker, i + 1, (HashMap) pm.get(docConclusionType), conclusion);
                } else if (i < sortStacker.size() - 1) {
                    identifyPatterns(sortStacker, i + 1, pm, conclusion);
                }
            }
        }
    }

}
