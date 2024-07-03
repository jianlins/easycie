package edu.utah.bmi.nlp.uima.ae;

/*******************************************************************************
 * Copyright  Apr 11, 2015  Department of Biomedical Informatics, University of Utah
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/


import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastcner.uima.FastCNER_AE_General;
import edu.utah.bmi.nlp.fastner.FastNER;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.ConceptBASE;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.joda.time.DateTime;
import org.pojava.datetime.DateTimeConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

//import org.pojava.datetime.DateTime;


/**
 * This is an AE to use FastCNER.java to identify the datetime mentions and compare with the reference date, to infer whether the context is historical or current.
 *
 * @author Jianlin Shi
 */
public class TemporalContext_AE_General extends FastCNER_AE_General {
    public static Logger logger = IOUtil.getLogger(TemporalContext_AE_General.class);

    public static final String PARAM_INFER_ALL = "InferTemporalStatusForAllTargetConcept";
    //    @ConfigurationParameter(name = CONCEPT_TYPE_NAME)

    protected LinkedHashMap<String, Class<? extends Annotation>> targetConceptClasses = new LinkedHashMap<>();
    protected LinkedHashMap<String, Method> setDiffMethods = new LinkedHashMap<>();
    //  read from record table, specify which column is the reference datetime, usually is set to admission datetime.
    public static final String PARAM_REFERENCE_DATE_COLUMN_NAME = "ReferenceDateColumnName";
    public static final String PARAM_RECORD_DATE_COLUMN_NAME = "RecordDateColumnName";
    public static final String PARAM_SAVE_DATE_ANNO = "SaveDateAnnotations";
    public static final String PARAM_SAVE_DATE_DIFF = "SaveDateDifference";
    public static final String PARAM_DATE_PRIORITY = "DatePriority";
    public static final String EARLYFIRST = "EARLYFIRST", LATEFIRST = "LATEFIRST", CLOSEFIRST = "CLOSEFIRST",
            CATEGORY_VALUES = "CATEGORY_VALUES";

    public static final String DIFFMETHOD = "AfterRefInHours";

    private String referenceDateColumnName, recordDateColumnName;


    // number of days before admission that still will be considered as current

    protected boolean inferAll = false, saveDateAnnotations = false, saveDateDifference = false;
    protected String datePriority = CLOSEFIRST;

    protected LinkedHashMap<Double, String> categories = new LinkedHashMap<>();


    private HashMap<String, IntervalST<Span>> dateAnnos = new HashMap();

    protected HashMap<String, Integer> numberMap = new HashMap<>();

    private Pattern[] patterns = new Pattern[5];

    private DateTime referenceDate;

    private HashMap<Integer, Long> sentenceStatusCache = new HashMap<>();

    public void initialize(UimaContext cont) {
        super.initialize(cont);
        initPatterns();
        initNumerMap();
        Object obj;
        String ruleStr = (String) (cont
                .getConfigParameterValue(PARAM_RULE_STR));
        LinkedHashMap<String, TypeDefinition> typeDefs = getTypeDefs(ruleStr);
        for (String typeName : typeDefs.keySet()) {
            Class<? extends Annotation> conceptClass = AnnotationOper.getTypeClass(typeDefs.get(typeName).getFullTypeName());
            targetConceptClasses.put(typeName, conceptClass);
            Method setDiffMethod = AnnotationOper.getDefaultSetMethod(conceptClass, DIFFMETHOD);
            if (setDiffMethod != null)
                setDiffMethods.put(typeName, setDiffMethod);
        }
        if (targetConceptClasses.size() == 0) {
            targetConceptClasses.put("Concept", Concept.class);
        }

        for (ArrayList<String> row : new IOUtil(ruleStr).getInitiations()) {
            if (row.get(1).endsWith(CATEGORY_VALUES)) {
                String value = row.get(2);
                double upperBound = Double.parseDouble(row.get(3));
                categories.put(upperBound, value);
            }
        }

        obj = cont.getConfigParameterValue(PARAM_REFERENCE_DATE_COLUMN_NAME);
        if (obj == null)
            referenceDateColumnName = "REF_DTM";
        else
            referenceDateColumnName = (String) obj;

        obj = cont.getConfigParameterValue(PARAM_RECORD_DATE_COLUMN_NAME);
        if (obj == null)
            recordDateColumnName = "DATE";
        else
            recordDateColumnName = (String) obj;


        obj = cont.getConfigParameterValue(PARAM_INFER_ALL);
        if (obj != null && obj instanceof Boolean && (Boolean) obj == true)
            inferAll = true;
        obj = cont.getConfigParameterValue(PARAM_SAVE_DATE_ANNO);
        if (obj != null && obj instanceof Boolean && (Boolean) obj == true)
            saveDateAnnotations = true;

        obj = cont.getConfigParameterValue(PARAM_SAVE_DATE_DIFF);
        if (obj != null && obj instanceof Boolean && (Boolean) obj == true)
            saveDateDifference = true;

        obj = cont.getConfigParameterValue(PARAM_DATE_PRIORITY);
        if (obj == null && obj instanceof String && obj.equals(LATEFIRST))
            datePriority = LATEFIRST;

    }

    public void initPatterns() {
        patterns[0] = Pattern.compile("^\\d{1,2}/\\d{1,2}$");
        patterns[1] = Pattern.compile("^\\d{4}$");
        patterns[2] = Pattern.compile("^\\d{1,2}/\\d{4}$");
        patterns[3] = Pattern.compile("^[JFMASODN][a-z]+");
    }

    public void initNumerMap() {
        String wordstr = "one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty";
        int i = 1;
        for (String word : wordstr.split("\\|")) {
            numberMap.put(word, i);
            i++;
        }
    }


    public void process(JCas jcas) throws AnalysisEngineProcessException {
        sentenceStatusCache.clear();
        ArrayList<Annotation> sentences = new ArrayList<>();
        ArrayList<ConceptBASE> targetConcepts = new ArrayList<>();
        RecordRow metaRecordRow = AnnotationOper.deserializeDocSrcInfor(jcas);
        referenceDate = readReferenceDate(metaRecordRow, referenceDateColumnName);
        DateTime recordDate = readReferenceDate(metaRecordRow, recordDateColumnName);
        dateAnnos.clear();
        if (recordDate == null)
            recordDate = referenceDate;
        if (referenceDate == null) {
            logger.fine("No value in Reference date column: '" + referenceDateColumnName + "'. Skip the TemporalConTextDetector.");
            return;
        }
        for (Class<? extends Annotation> cls : targetConceptClasses.values()) {
            for (Annotation anno : JCasUtil.select(jcas, cls)) {
                if (anno instanceof ConceptBASE) {
//                    TODO check if this is generalizable
                    ConceptBASE concept = (ConceptBASE) anno;
                    if (concept.getTemporality() == null || concept.getTemporality().equals("present"))
                        targetConcepts.add(concept);
                } else {
                    logger.warning(cls.getCanonicalName() + " is not an instance of ConceptBASE. So it cannot be processed through TemporalContext_AE_General");
                }
            }
        }
        FSIndex annoIndex;
        Iterator annoIter;
        if (targetConcepts.size() > 0) {
            annoIndex = jcas.getAnnotationIndex(SentenceType);
            annoIter = annoIndex.iterator();
            IntervalST sentenceTree = new IntervalST();
            while (annoIter.hasNext()) {
                Annotation sentence = (Annotation) annoIter.next();
                sentenceTree.put(new Interval1D(sentence.getBegin(), sentence.getEnd()), sentence);
            }
            for (ConceptBASE concept : targetConcepts) {
                processCase(jcas, concept, (Annotation) sentenceTree.get(new Interval1D(concept.getBegin(),
                        concept.getEnd())), recordDate, referenceDate);
            }
        }
    }

    private DateTime readReferenceDate(RecordRow recordRow, String referenceDateColumnName) {
        String dateString = (String) recordRow.getValueByColumnName(referenceDateColumnName);
        if (dateString == null)
            return null;
        return parseDateString(dateString, referenceDate, referenceDate);
    }


    private DateTime parseDateString(String dateString, DateTime recordDate, DateTime referenceDate) {
        Date utilDate = null;
        try {
            if (recordDate != null) {
                utilDate = new org.pojava.datetime.DateTime(dateString, DateTimeConfig.getDateTimeConfig(recordDate.toDate())).toDate();
            } else {
                utilDate = new org.pojava.datetime.DateTime(dateString, DateTimeConfig.getDateTimeConfig(this.referenceDate == null ? null : this.referenceDate.toDate())).toDate();
            }

        } catch (Exception e) {
            logger.fine("Illegal date string: " + dateString);
            logger.fine(e.getMessage());
        }
//        try {
//            if (utilDate == null) {
//                utilDate = new org.pojava.datetime.DateTime(dateString).toDate();
//                System.out.println(referenceDate.getYear());
//                utilDate.setYear(referenceDate.getYear());
//            }
//        } catch (Exception e) {
//            logger.fine("Illegal date string: " + dateString);
//        }


        DateTime date = new DateTime(utilDate);
        return date;
    }


    private void processCase(JCas jcas, ConceptBASE concept, Annotation sentence,
                             DateTime recordDate, DateTime referenceDate) {
        long temporalStatus;
        if (sentenceStatusCache.containsKey(sentence.getBegin())) {
            temporalStatus = sentenceStatusCache.get(sentence.getBegin());
        } else {
            HashMap<String, ArrayList<Span>> dates = ((FastCNER) fastNER).processAnnotation(sentence);
            sentence.getBegin();
//          presented on 1/04/89 with one week of right upper thigh and groin pain, as well as swelling.
            temporalStatus = inferTemporalStatus(jcas, concept, dates, recordDate, referenceDate);
        }
        if (temporalStatus != 1000000 && temporalStatus != -1000000) {
            String typeName = concept.getClass().getSimpleName();
            if (setDiffMethods.containsKey(typeName) && saveDateDifference)
                AnnotationOper.setFeatureValue(setDiffMethods.get(typeName), concept, temporalStatus + "");
            for (double upperBound : categories.keySet()) {
                if (temporalStatus < upperBound) {
                    concept.setTemporality(categories.get(upperBound));
                    break;
                }
            }
        }

    }

    /**
     * For now only infer historical, current or uncertain.
     * Hypothetical will be handled later if necessary.
     *
     * @param jcas
     * @param concept
     * @param dates
     * @param recordDate
     * @param referenceDate @return
     */
    private long inferTemporalStatus(JCas jcas, ConceptBASE concept, HashMap<String, ArrayList<Span>> dates,
                                     DateTime recordDate, DateTime referenceDate) {
        long temporalStatus = datePriority.equals(EARLYFIRST) ? 1000000 : -1000000;
        int[] distance = new int[]{1000000};
        String text = jcas.getDocumentText();
        if (inferAll && (dates.size() == 0) || !hasDateMentions(dates)) {
            return (long) (recordDate.getMillis() - referenceDate.getMillis()) / 3600000;
        }
        for (Map.Entry<String, ArrayList<Span>> entry : dates.entrySet()) {
            String typeOfDate = entry.getKey();
            switch (typeOfDate) {
                case "Date":
                    ArrayList<Span> dateMentions = entry.getValue();
                    for (Span span : dateMentions) {
                        if (fastNER.getMatchedNEType(span) == DeterminantValueSet.Determinants.PSEUDO)
                            continue;
                        String dateMention = text.substring(span.begin, span.end).trim();
                        DateTime dt = null;
                        try {
                            dt = parseDateString(dateMention, recordDate, referenceDate);
                        } catch (Exception e) {
//                    e.printStackTrace();
                        }
                        if (dt == null) {
                            dt = handleAmbiguousCase(dateMention, recordDate, referenceDate);
                        }
                        logger.finest("Parse '" + dateMention + "' as: '" + dt.toString() + "'");
                        int currentDistance = getDistance(span, concept);
                        temporalStatus = updateTemporalStatus(dt, referenceDate, temporalStatus, distance, currentDistance);
                        saveDateConcept(jcas, ConceptTypeConstructors, typeOfDate, span, temporalStatus, "ParsedDate:\t" + dt.toString(), getRuleInfo(span));
                    }
                    break;
                case "Yeard":
                    temporalStatus = inferTemporalStatusFromRelativeNumericTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 365, concept,distance);
                    break;
                case "Monthd":
                    temporalStatus = inferTemporalStatusFromRelativeNumericTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 30, concept,distance);
                    break;
                case "Weekd":
                    temporalStatus = inferTemporalStatusFromRelativeNumericTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 7, concept, distance);
                    break;
                case "Dayd":
                    temporalStatus = inferTemporalStatusFromRelativeNumericTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 1, concept, distance);
                    break;
                case "Yearw":
                    temporalStatus = inferTemporalStatusFromRelativeLiteralTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 365, concept, distance);
                    break;
                case "Monthw":
                    temporalStatus = inferTemporalStatusFromRelativeLiteralTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 30, concept, distance);
                    break;
                case "Weekw":
                    temporalStatus = inferTemporalStatusFromRelativeLiteralTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 7, concept, distance);
                    break;
                case "Dayw":
                    temporalStatus = inferTemporalStatusFromRelativeLiteralTime(jcas, typeOfDate, text, entry.getValue(), referenceDate, recordDate, temporalStatus, 1, concept, distance);
                    break;
            }
        }
        return temporalStatus;
    }

    private int getDistance(Span span, ConceptBASE concept) {
        if (span.getBegin() > concept.getEnd()) {
            return span.getBegin() - concept.getEnd();
        } else if (span.getEnd() < concept.getBegin()) {
            return concept.getBegin() - span.getEnd();
        }
        return 0;
    }

    public boolean hasDateMentions(HashMap<String, ArrayList<Span>> dates) {
        for (ArrayList<Span> mentions : dates.values()) {
            if (mentions.size() > 0)
                return true;
        }
        return false;
    }

    private long inferTemporalStatusFromRelativeNumericTime(JCas jcas, String typeName, String text, ArrayList<Span> spans,
                                                            DateTime referenceDate, DateTime recordDate, long temporalStatus, int unit, ConceptBASE concept, int[] distance) {
        for (Span span : spans) {
            try {
                int interval = Integer.parseInt(text.substring(span.begin, span.end).trim());
                DateTime dt = recordDate.minusDays(interval * unit);
                int currentDistance=getDistance(span, concept);
                temporalStatus = updateTemporalStatus(dt, referenceDate, temporalStatus, distance, currentDistance);
                saveDateConcept(jcas, ConceptTypeConstructors, typeName, span, temporalStatus, "ParsedDate:\t" + dt.toString(), getRuleInfo(span));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return temporalStatus;
    }

    private long inferTemporalStatusFromRelativeLiteralTime(JCas jcas, String typeName, String text, ArrayList<Span> spans,
                                                            DateTime referenceDate, DateTime recordDate, long temporalStatus, int unit, ConceptBASE concept, int[] distance) {
        for (Span span : spans) {
            try {
                String numWord = text.substring(span.begin, span.end).trim().toLowerCase();
                if (numberMap.containsKey(numWord)) {
                    int interval = numberMap.get(numWord);
                    DateTime dt = recordDate.minusDays(interval * unit);
                    int currentDistance=getDistance(span, concept);
                    temporalStatus = updateTemporalStatus(dt, referenceDate, temporalStatus, distance, currentDistance);
                    saveDateConcept(jcas, ConceptTypeConstructors, typeName, span, temporalStatus, "ParsedDate:\t" + dt.toString(), getRuleInfo(span));


                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return temporalStatus;
    }

    private long updateTemporalStatus(DateTime dt, DateTime referenceDate, long previousDiff, int[] distance, int currentDistance) {
        long diff = dt.getMillis() - referenceDate.getMillis();
        diff = diff / 3600000;
        switch (datePriority) {
            case EARLYFIRST:
                if (diff > previousDiff)
                    diff = previousDiff;
                break;
            case LATEFIRST:
                if (diff < previousDiff)
                    diff = previousDiff;
                break;
            case CLOSEFIRST:
                if (currentDistance > distance[0])
                    diff = previousDiff;
                else
                    distance[0] = currentDistance;
                break;

        }
        return diff;
    }

    private DateTime handleAmbiguousCase(String dateMentions, DateTime recordDate, DateTime referenceDate) {
        DateTime dt = null;
        if (patterns[0].matcher(dateMentions).find()) {
            dt = parseDateString(dateMentions + " " + referenceDate.getYear(), recordDate, referenceDate);
            if (dt == null)
                return referenceDate;
            if (dt.getMonthOfYear() > referenceDate.getMonthOfYear())
                dt = dt.minusYears(1);
        } else if (patterns[1].matcher(dateMentions).find()) {
            dateMentions = "01/01/" + dateMentions;
            dt = parseDateString(dateMentions, recordDate, referenceDate);
            if (dt == null)
                dt = referenceDate;
        } else if (patterns[2].matcher(dateMentions).find()) {
            dateMentions = "01/" + dateMentions;
            dt = parseDateString(dateMentions, recordDate, referenceDate);
            if (dt == null)
                dt = referenceDate;
        } else if (patterns[3].matcher(dateMentions).find()) {
            dt = parseDateString(dateMentions + " " + referenceDate.getYear(), recordDate, referenceDate);
            if (dt.getMonthOfYear() > referenceDate.getMonthOfYear())
                dt = dt.minusYears(1);
        } else {
            logger.fine("Uncertain date: " + dateMentions + "\t");
            dt = referenceDate;
        }
        logger.fine("Interpret ambigous date mention: " + dateMentions + " as " + dt.toString());
        return dt;
    }


    protected String getRuleInfo(Span span) {
        return span.ruleId + ":\t" + fastNER.getMatchedRuleString(span).rule;
    }

    protected String getMatchedNEName(Span span) {
        return fastNER.getMatchedNEName(span);
    }

    protected DeterminantValueSet.Determinants getSpanType(Span span) {
        return fastNER.getMatchedNEType(span);
    }

    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleFile, boolean caseSenstive) {
        LinkedHashMap<String, TypeDefinition> typeDefs = new FastNER(ruleFile, caseSenstive, false).getTypeDefinitions();
        for (String typeName : typeDefs.keySet()) {
            TypeDefinition typeDef = typeDefs.get(typeName);
            if (!typeDef.getFeatureValuePairs().containsKey(DIFFMETHOD)) {
                typeDef.addFeatureName(DIFFMETHOD);
            }
        }
        typeDefs.remove(CATEGORY_VALUES);
        return typeDefs;
    }

    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        return TemporalContext_AE_General.getTypeDefinitions(ruleStr, true);
    }


    protected void saveDateConcept(JCas jcas, HashMap<String, Constructor<? extends Concept>> ConceptTypeConstructors,
                                   String typeName, Span span, long afterRefInhours, String... rule) {
        if (!saveDateAnnotations || getSpanType(span) != DeterminantValueSet.Determinants.ACTUAL) {
            return;
        }
        Annotation anno = null;
        if (!dateAnnos.containsKey(typeName)) {
            dateAnnos.put(typeName, new IntervalST<>());
        }
        IntervalST<Span> intervalST = dateAnnos.get(typeName);
        Interval1D interval1D = new Interval1D(span.begin, span.end);
        if (intervalST.contains(interval1D)) {
            return;
        } else {
            intervalST.put(interval1D, span);
        }
        try {
            anno = ConceptTypeConstructors.get(typeName).newInstance(jcas, span.begin, span.end);
            if (anno instanceof ConceptBASE) {
                if (afterRefInhours != 1000000 && afterRefInhours != -1000000)
                    ((ConceptBASE) anno).setCategory(afterRefInhours + "");
                if (rule.length > 0) {
                    ((ConceptBASE) anno).setNote(String.join("\n", rule));
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        anno.addToIndexes();
    }


}