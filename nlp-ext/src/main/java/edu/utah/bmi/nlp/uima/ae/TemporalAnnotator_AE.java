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
import edu.utah.bmi.nlp.core.SpanComparator;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.Paragraph;
import edu.utah.bmi.nlp.uima.common.AnnotationComparator;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.joda.time.DateTime;
import org.pojava.datetime.DateTimeConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static edu.utah.bmi.nlp.core.NERSpan.byRuleLength;
import static edu.utah.bmi.nlp.core.NERSpan.scorewidth;


/**
 * This is an AE to use FastCNER.java to identify the datetime mentions within specified sections (inclusionSections)
 * and/or the sentences that contain specified concepts (targetConceptTypes).
 * Save the date mention annotation, normalize the date to a standard format (in NormDate attribute).
 * Save the elapsed hours with respect to reference date (in Elapse attribute)
 *
 * @author Jianlin Shi
 */
public class TemporalAnnotator_AE extends FastCNER_AE_General {
    public static Logger logger = IOUtil.getLogger(TemporalAnnotator_AE.class);

    //  specify which type of annotations as the target concept
    public static final String PARAM_SAVE_INFERRED_RECORD_DATE = "SaveInferredRecordDate";

    //  read from record table, specify which column is the reference datetime, usually is set to admission datetime.
    public static final String PARAM_REFERENCE_DATE_COLUMN_NAME = "ReferenceDateColumnName";
    public static final String PARAM_RECORD_DATE_COLUMN_NAME = "RecordDateColumnName";

    //  Also try to identify the temporal mentions around the target concepts (within sentence boundary), if
    //  not covered by inclusionSections
    public static final String PARAM_AROUND_CONCEPTS = "AroundConcepts";

    //  Decide the bounary of "around", can be a sentence or a paragraph
    public static final String PARAM_AROUND_BOUNDARY = "AroundBoundary";

    protected String referenceDateColumnName, recordDateColumnName;


    protected HashMap<String, IntervalST<Span>> dateAnnos = new HashMap<>();

    protected HashMap<String, Integer> numberMap = new HashMap<>();

    protected LinkedHashSet<Class<? extends Annotation>> targetConceptTypes = new LinkedHashSet<>();

    protected Pattern[] patterns = new Pattern[5];

    protected DateTime referenceDate;
    protected boolean globalCertainty = false;
    protected boolean saveInferredRecordDate = false;
    protected boolean cner = true;
    protected String aroundBoundary = "Paragraph";

    protected LinkedHashMap<Double, String> categories = new LinkedHashMap<>();
    private RecordRow baseRecordRow;

    public void initialize(UimaContext cont) {
        super.initialize(cont);
        initPatterns();
        initNumerMap();
        Object obj;
        obj = cont.getConfigParameterValue(PARAM_SAVE_INFERRED_RECORD_DATE);
        if (obj != null && obj instanceof Boolean)
            saveInferredRecordDate = (Boolean) obj;

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

        obj = cont.getConfigParameterValue(PARAM_INCLUDE_SECTIONS);
        if (obj != null && ((String) obj).trim().length() > 0) {
            for (String sectionName : ((String) obj).split("[\\|,;]")) {
                sectionName = sectionName.trim();
                includeSectionClasses.add(AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(sectionName)));
            }
        }

        obj = cont.getConfigParameterValue(PARAM_AROUND_CONCEPTS);
        if (obj != null && ((String) obj).trim().length() > 0) {
            for (String conceptName : ((String) obj).trim().split("[;, ]+")) {
                Class typeCls = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(conceptName));
                if (typeCls != null && Concept.class.isAssignableFrom(typeCls))
                    targetConceptTypes.add((Class<? extends Concept>) typeCls);
                else
                    logger.warning("Try to search temporal mentions around '" + conceptName + "', but this type is not defined and loaded" +
                            " (or not a subclass of Concept), check the spelling and the NER rules.");
            }
        }

        aroundBoundary = "Paragraph";
        obj = cont.getConfigParameterValue(PARAM_AROUND_BOUNDARY);
        if (obj != null && ((String) obj).trim().length() > 0) {
            if (((String) obj).equalsIgnoreCase("Sentence")) {
                aroundBoundary = "Sentence";
            }
        }


//        if no section and concept classs is specified, process the whole document.
        if (includeSectionClasses.size() == 0 && ConceptTypeClasses.size() == 0) {
            includeSectionClasses.add(SourceDocumentInformation.class);
        }

        String ruleStr = (String) cont.getConfigParameterValue(DeterminantValueSet.PARAM_RULE_STR);
        for (ArrayList<String> row : new IOUtil(ruleStr).getInitiations()) {
            if (row.get(1).endsWith(DeterminantValueSet.TEMPORAL_CATEGORIES1.substring(1))) {
                String value = row.get(2);
                double upperBound = Double.parseDouble(row.get(3));
                categories.put(upperBound, value);
            }
        }
//      default attribute is a string, parse it to an integer
        for (Integer ruleId : this.fastNER.fastRule.ruleStore.keySet()) {
            NERRule rule = this.fastNER.fastRule.ruleStore.get(ruleId);
            if (rule.attributes.size() > 0) {
                rule.attributes.set(0, Integer.parseInt("" + rule.attributes.get(0)));
                this.fastNER.fastRule.ruleStore.put(ruleId, rule);
            }
        }
    }

    protected LinkedHashMap<String, TypeDefinition> initFastNER(UimaContext cont, String ruleStr) {
        IOUtil ioUtil = new IOUtil(ruleStr);
        if (ioUtil.getSettings().containsKey("fastcner")) {
            super.initFastNER(cont, ruleStr);
        } else {
            fastNER = new FastNER(ruleStr, caseSenstive);
            if (markPseudo)
                fastNER.setRemovePseudo(false);
            fastNER.setCompareMethod(scorewidth);
            fastNER.setWidthCompareMethod(byRuleLength);
            cner = false;
        }
        LinkedHashMap<String, TypeDefinition> typeDefs = fastNER.getTypeDefinitions();
        typeDefs.remove(DeterminantValueSet.TEMPORAL_CATEGORIES1.substring(1));
        return typeDefs;
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


    public void process(JCas jCas) throws AnalysisEngineProcessException {
        baseRecordRow = AnnotationOper.deserializeDocSrcInfor(jCas);

        referenceDate = readReferenceDate(jCas, referenceDateColumnName);
        DateTime recordDate = readReferenceDate(jCas, recordDateColumnName);
        if (referenceDate == null)
            referenceDate = new DateTime(System.currentTimeMillis());

        globalCertainty = recordDate != null;
        dateAnnos.clear();
        String docText = jCas.getDocumentText();
        IntervalST<Annotation> sectionTree = indexAnnotations(jCas, includeSectionClasses);
        IntervalST<Annotation> conceptTree = indexAnnotations(jCas, targetConceptTypes);
        IntervalST<Annotation> paragraphTree = new IntervalST<>();
        if (aroundBoundary.equals("Paragraph")) {
            paragraphTree = indexParagraphs(jCas, conceptTree, sectionTree);
        }
        ArrayList<Annotation> sentences = indexSentences(jCas, conceptTree, paragraphTree, sectionTree);
        sentences.sort(new AnnotationComparator());
        ArrayList<Annotation> tokens = new ArrayList<>();
        TreeMap<Integer, TreeSet<Integer>> sentence2TokenMap = new TreeMap<Integer, TreeSet<Integer>>();
        if (!cner) {
            Iterator<? extends Annotation> annoIter = JCasUtil.iterator(jCas, TokenType);
            while (annoIter.hasNext()) {
                tokens.add(annoIter.next());
            }
            tokens.sort(new AnnotationComparator());
            AnnotationOper.buildAnnoMap(sentences, tokens, sentence2TokenMap);
        }
        ArrayList<Annotation> allDateMentions = processSentences(jCas, sentences, recordDate, sentence2TokenMap, tokens);
        coordinateDateMentions(allDateMentions, jCas.getDocumentText().length());
    }

    /**
     * Index paragraphs that contains a concept or within selected sections
     *
     * @param jCas
     * @param conceptTree
     * @param sectionTree
     * @return an interval tree of paragraphs
     */
    private IntervalST<Annotation> indexParagraphs(JCas jCas, IntervalST<Annotation> conceptTree, IntervalST<Annotation> sectionTree) {
        IntervalST<Annotation> paragraphTree = new IntervalST<>();
        Iterator<? extends Annotation> annoIter = JCasUtil.iterator(jCas, Paragraph.class);
        while (annoIter.hasNext()) {
            Annotation paragraph = annoIter.next();
            Interval1D interval = new Interval1D(paragraph.getBegin(), paragraph.getEnd());
            if ((this.aroundBoundary.equals("Paragraph") && conceptTree.contains(interval)) || sectionTree.contains(interval)) {
                if (!paragraphTree.contains(interval))
                    paragraphTree.put(interval, paragraph);
            }
        }
        return paragraphTree;
    }

    /**
     * Index all target concepts
     *
     * @param jCas
     * @param annotationClasses
     * @return
     */
    protected IntervalST<Annotation> indexAnnotations(JCas jCas, HashSet<Class<? extends Annotation>> annotationClasses) {
        IntervalST<Annotation> annotationTree = new IntervalST<>();
        for (Class<? extends Annotation> annoCls : annotationClasses) {
            Iterator<? extends Annotation> annoIter = JCasUtil.iterator(jCas, annoCls);
            while (annoIter.hasNext()) {
                Annotation concept = annoIter.next();
                Interval1D interval = new Interval1D(concept.getBegin(), concept.getEnd());
                if (!annotationTree.contains(interval)) {
                    annotationTree.put(interval, concept);
                }
            }
        }
        return annotationTree;
    }

    protected ArrayList<Annotation> processSentences(JCas jCas, ArrayList<Annotation> sentences, DateTime recordDate,
                                                     TreeMap<Integer, TreeSet<Integer>> sentence2TokenMap,
                                                     ArrayList<Annotation> tokens) {
        ArrayList<Annotation> allDateMentions = new ArrayList<>();
        try {
            for (int i = 0; i < sentences.size(); i++) {
                Annotation sentence = sentences.get(i);
                HashMap<String, ArrayList<Span>> dates;
                if (cner)
                    dates = ((FastCNER) fastNER).processString(sentence.getCoveredText());
                else {
                    ArrayList<Annotation> tokensInSeg = new ArrayList<Annotation>();
                    for (int tokenId : sentence2TokenMap.get(i)) {
                        tokensInSeg.add(tokens.get(tokenId));
                    }
                    dates = fastNER.processAnnotationList(tokensInSeg);
                }
                if (sentence!=null)
                    allDateMentions.addAll(parseDateMentions(jCas, sentence, dates, recordDate));
            }
        } catch (Exception e) {
            System.err.println("DOC_ID=" + baseRecordRow.getStrByColumnName("DOC_ID"));
            e.printStackTrace();
        }
        return allDateMentions;
    }


    /**
     * Index senteces: if boundary is paragraphy, then sentences within the paragraphs that contains a target concept or within selected sections will be index.
     * If boundary is sentence, then the sentences that contains a target concept or within selected sections will be index.
     * @param jCas JCas object
     * @param conceptTree interval tree of concepts
     * @param paragraphTree interval tree of paragraphs
     * @param sectionTree interval tree of sections
     * @return a list of sentence annotations.
     */
    protected ArrayList<Annotation> indexSentences(JCas jCas, IntervalST<Annotation> conceptTree,
                                                   IntervalST<Annotation> paragraphTree, IntervalST<Annotation> sectionTree) {

        Iterator<? extends Annotation> annoIter = JCasUtil.iterator(jCas, SentenceType);
        ArrayList<Annotation> sentences = new ArrayList<>();
        while (annoIter.hasNext()) {
            Annotation sentence = annoIter.next();
            Interval1D interval = new Interval1D(sentence.getBegin(), sentence.getEnd());
            if (this.aroundBoundary.equals("Paragraph")) {
                if (paragraphTree.contains(interval))
                    sentences.add(sentence);
            } else {
                if (conceptTree.contains(interval) || sectionTree.contains(interval)) {
                    sentences.add(sentence);
                }
            }
        }
        return sentences;
    }

    protected DateTime readReferenceDate(JCas jcas, String referenceDateColumnName) {
        if (referenceDateColumnName == null || referenceDateColumnName.trim().length() == 0)
            return null;
        FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
        RecordRow recordRow = new RecordRow();
        if (it.hasNext()) {
            SourceDocumentInformation e = (SourceDocumentInformation) it.next();
            String serializedString = e.getUri();
            recordRow.deserialize(serializedString);
        }
        String dateString = (String) recordRow.getValueByColumnName(referenceDateColumnName);
        if (dateString == null)
            return null;
        return parseDateString(dateString, referenceDate);
    }


    protected DateTime parseDateString(String dateString, DateTime recordDate) {
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


        DateTime date = new DateTime(utilDate);
        return date;
    }


    /**
     * For parse date mentions and save as annotations.
     *
     * @param jcas       JCas object
     * @param dates      List of date spans grouped by types
     * @param recordDate document record date
     * @return a list of date mention annotations
     */
    protected ArrayList<Annotation> parseDateMentions(JCas jcas, Annotation sentence, HashMap<String, ArrayList<Span>> dates,
                                                      DateTime recordDate) {
        String text;
        int offset;
        if (cner) {
            offset = sentence.getBegin();
            text = sentence.getCoveredText();
        } else {
            text = jcas.getDocumentText();
            offset = 0;
        }
        String latestDateMention = "";
        ArrayList<Annotation> allDateMentions = new ArrayList<>();
        if (recordDate == null) {
            if (dates.containsKey("CERTAIN_DATE"))
                for (Span span : dates.get("CERTAIN_DATE")) {
                    DateTime dt = null;
                    String dateMention = text.substring(span.begin, span.end).trim();
                    try {
                        dt = parseDateString(dateMention, recordDate);
                        if (recordDate == null || dt.isAfter(recordDate)) {
                            recordDate = dt;
                            latestDateMention = dateMention;
                        }
                    } catch (Exception e) {
//                    e.printStackTrace();
                    }
                }
        }
        if (recordDate == null) {
            if (dates.containsKey("CERTAIN_YEAR"))
                for (Span span : dates.get("CERTAIN_YEAR")) {
                    DateTime dt = null;
                    String dateMention = text.substring(span.begin, span.end).trim();
                    try {
                        dt = parseDateString(dateMention, recordDate);
                        if (recordDate == null || dt.isAfter(recordDate)) {
                            recordDate = dt;
                            latestDateMention = dateMention;
                        }
                    } catch (Exception e) {
//                    e.printStackTrace();
                    }
                }
        }
        logger.finest(latestDateMention.length() > 0 ? "Record date is not set, inferred from the mention: \"" + latestDateMention + "\" as " + recordDate : "");
        if (saveInferredRecordDate && recordDate != null) {
            SourceDocumentInformation meta = JCasUtil.select(jcas, SourceDocumentInformation.class).iterator().next();
            RecordRow metaRecord = new RecordRow();
            metaRecord.deserialize(meta.getUri());
            metaRecord.addCell(recordDateColumnName, recordDate);
            meta.setUri(metaRecord.serialize());
        }
//      process absolute date mentions first, from which relative date mentions can be referenced to
        ArrayList<Annotation> absDates = new ArrayList<>();
        ArrayList<DateTime> absDateTimes = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Span>> entry : dates.entrySet()) {
            String typeOfDate = entry.getKey();
            switch (typeOfDate) {
                case "Date":
                case "CERTAIN_YEAR":
                case "CERTAIN_DATE":
                case "ABS_DATE":
                    ArrayList<Span> dateMentions = entry.getValue();
                    dateMentions.sort(new SpanComparator());
                    for (Span span : dateMentions) {
                        String certainty = globalCertainty || typeOfDate.startsWith("CERTAIN") ? "certain" : "uncertain";
                        if (fastNER.getMatchedNEType(span) == DeterminantValueSet.Determinants.PSEUDO)
                            continue;
                        String dateMention = text.substring(span.begin, span.end).trim();
                        DateTime dt = null;
                        try {
                            dt = parseDateString(dateMention, recordDate);
                        } catch (Exception e) {
//                    		e.printStackTrace();
                        }
                        if (dt == null) {
                            certainty = "uncertain";
                            dt = handleAmbiguousCase(dateMention, recordDate);
                        }
                        logger.finest("Parse '" + dateMention + "' as: '" + dt.toString() + "'");
                        absDates.add(addDateMentions(jcas, ConceptTypeConstructors, allDateMentions,
                                typeOfDate, certainty, span, offset, dt, getRuleInfo(span)));
                        absDateTimes.add(dt);
                    }
                    break;
            }
        }
//      split intervals around date mentions and build an interval tree.
        IntervalST<DateTime> absDateTree = new IntervalST<>();
        if (absDates.size() > 1) {
            absDateTree.put(new Interval1D(sentence.getBegin(), (absDates.get(1).getBegin() + absDates.get(0).getEnd()) / 2), absDateTimes.get(0));
            for (int i = 1; i < absDates.size() - 1; i++) {
                absDateTree.put(new Interval1D((int) (absDates.get(i - 1).getEnd() + absDates.get(i).getBegin()) / 2, (int) (absDates.get(i).getEnd() + absDates.get(i + 1).getBegin()) / 2),
                        absDateTimes.get(i));
            }
            absDateTree.put(new Interval1D((absDates.get(absDates.size() - 1).getBegin() + absDates.get(absDates.size() - 2).getEnd()) / 2, sentence.getEnd()), absDateTimes.get(absDateTimes.size() - 1));
        } else if (absDates.size() == 1) {
            absDateTree.put(new Interval1D(sentence.getBegin(), sentence.getEnd()), absDateTimes.get(0));
        }
//      process relative date mentions next
        for (Map.Entry<String, ArrayList<Span>> entry : dates.entrySet()) {
            String typeOfDate = entry.getKey();
            switch (typeOfDate) {
                case "Yeard":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 365, offset, absDateTree);
                    break;
                case "Monthd":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 30, offset, absDateTree);
                    break;
                case "Weekd":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 7, offset, absDateTree);
                    break;
                case "Dayd":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 1, offset, absDateTree);
                    break;
                case "Yearw":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 365, offset, absDateTree);
                    break;
                case "Monthw":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 30, offset, absDateTree);
                    break;
                case "Weekw":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 7, offset, absDateTree);
                    break;
                case "Dayw":
                    for (Span span : entry.getValue())
                        inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 1, offset, absDateTree);
                    break;
                case "REL_DATE":
                    for (Span span : entry.getValue()) {
                        NERRule rule = fastNER.getMatchedRuleString(span);
                        int unit = 0;
                        if (rule.attributes.size() > 0) {
                            unit = (int) rule.attributes.get(0);
                        }
                        String numericType = "d";
                        if (rule.attributes.size() > 1) {
                            numericType = ((String) rule.attributes.get(1)).substring(0, 1).toLowerCase();
                        }
                        if (numericType.equals("w")) {
                            inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, unit, offset, absDateTree);
                        } else {
                            inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, unit, offset, absDateTree);
                        }

                    }
                    break;
            }
        }
        return allDateMentions;
    }

    protected long getDiffHours(DateTime dt, DateTime referenceDate) {
        long diff = dt.getMillis() - referenceDate.getMillis();
        diff = diff / 3600000;
        return diff;
    }

    protected Concept addDateMentions(JCas jcas, HashMap<String, Constructor<? extends Concept>> ConceptTypeConstructors,
                                      ArrayList<Annotation> allDateMentions, String typeName,
                                      String certainty, Span span, int offset, DateTime date, String... ruleInfo) {
        if (getSpanType(span) != DeterminantValueSet.Determinants.ACTUAL) {
            return null;
        }
        Concept anno = null;
        if (!dateAnnos.containsKey(typeName)) {
            dateAnnos.put(typeName, new IntervalST<>());
        }
        IntervalST<Span> intervalST = dateAnnos.get(typeName);
        Interval1D interval1D = new Interval1D(span.begin, span.end);
        if (intervalST.contains(interval1D)) {
            return anno;
        } else {
            intervalST.put(interval1D, span);
        }
        try {
            anno = ConceptTypeConstructors.get(typeName).newInstance(jcas, span.begin + offset, span.end + offset);
            anno.setCertainty(certainty);

            if (ruleInfo.length > 0) {
                anno.setNote(String.join("\n", ruleInfo));
            }
            if (anno instanceof edu.utah.bmi.nlp.type.system.Date) {
                if (date != null) {
                    ((edu.utah.bmi.nlp.type.system.Date) anno).setNormDate(date.toString());
                    long diff = getDiffHours(date, referenceDate);
                    ((edu.utah.bmi.nlp.type.system.Date) anno).setElapse(diff);
                    if (categories.size() > 0) {
                        for (double upperBound : categories.keySet()) {
                            if (diff < upperBound) {
                                String status=categories.get(upperBound);
                                anno.setTemporality(status);
                                break;
                            }
                        }
                    }
                }
            }
            allDateMentions.add(anno);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return anno;
    }


    protected DateTime inferDateFromRelativeNumericTime(JCas jcas, ArrayList<Annotation> allDateMentions, String typeName, String text, Span span,
                                                        DateTime anchorDate, int unit, int offset, IntervalST<DateTime> absDateTree) {
        DateTime dt = null;
        //      adjust the anchor date to local (within sentence) date mentions, if available.
        Interval1D thisSpan = new Interval1D(span.getBegin() + offset, span.getEnd() + offset);
        if (absDateTree.contains(thisSpan)) {
            anchorDate = absDateTree.get(thisSpan);
        }
        try {
            String certainty = globalCertainty ? "certain" : "uncertain";
            float interval = Float.parseFloat(text.substring(span.begin, span.end).trim());
            if (anchorDate == null)
                anchorDate = referenceDate;
            dt = anchorDate.minusDays((int) interval * unit);
            if (unit > 7) {
                certainty = "uncertain";
            }
            addDateMentions(jcas, ConceptTypeConstructors, allDateMentions, typeName, certainty, span, offset, dt, getRuleInfo(span));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dt;
    }

    protected DateTime inferDateFromRelativeLiteralTime(JCas jcas, ArrayList<Annotation> allDateMentions, String typeName, String text, Span span,
                                                        DateTime anchorDate, int unit, int offset, IntervalST<DateTime> absDateTree) {
        DateTime dt = null;
//      adjust the anchor date to local (within sentence) date mentions, if available.
        Interval1D thisSpan = new Interval1D(span.getBegin() + offset, span.getEnd() + offset);
        if (absDateTree.contains(thisSpan)) {
            anchorDate = absDateTree.get(thisSpan);
        }
        if (anchorDate == null)
            anchorDate = referenceDate;
        try {
            String numWord = text.substring(span.begin, span.end).trim().toLowerCase();
            String certainty = globalCertainty ? "certain" : "uncertain";
            if (numberMap.containsKey(numWord)) {
                int interval = numberMap.get(numWord);
                dt = anchorDate.minusDays(interval * unit);
                if (interval > 7) {
                    certainty = "uncertain";
                }
                addDateMentions(jcas, ConceptTypeConstructors, allDateMentions, typeName, certainty, span, offset, dt, getRuleInfo(span));
            } else {
//              deal with yesterday, the day before yesterday etc.
                dt = anchorDate.minusDays(unit);
                addDateMentions(jcas, ConceptTypeConstructors, allDateMentions, typeName, certainty, span, offset, dt, getRuleInfo(span));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dt;
    }


    protected DateTime handleAmbiguousCase(String dateMentions, DateTime recordDate) {
        DateTime dt = null;
        if (recordDate == null)
            recordDate = referenceDate;
        if (patterns[0].matcher(dateMentions).find()) {
            dt = parseDateString(dateMentions + " " + referenceDate.getYear(), recordDate);
            if (dt == null)
                return referenceDate;
            if (dt.getMonthOfYear() > referenceDate.getMonthOfYear())
                dt = dt.minusYears(1);
        } else if (patterns[1].matcher(dateMentions).find()) {
            dateMentions = "01/01/" + dateMentions;
            dt = parseDateString(dateMentions, recordDate);
            if (dt == null)
                dt = referenceDate;
        } else if (patterns[2].matcher(dateMentions).find()) {
            dateMentions = "01/" + dateMentions;
            dt = parseDateString(dateMentions, recordDate);
            if (dt == null)
                dt = referenceDate;
        } else if (patterns[3].matcher(dateMentions).find()) {
            dt = parseDateString(dateMentions + " " + referenceDate.getYear(), recordDate);
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

    protected void coordinateDateMentions(ArrayList<Annotation> allDateMentions, int length) {
        IntervalST<Integer> checker = new IntervalST<>();
        HashSet<Integer> scheduleToRemoveIds = new HashSet<>();
        for (int i = 0; i < allDateMentions.size(); i++) {
            Annotation anno = allDateMentions.get(i);
            Interval1D span = new Interval1D(anno.getBegin(), anno.getEnd());
            if (checker.contains(span)) {
                for (Integer existingId : checker.getAll(span)) {
                    Annotation existingAnno = allDateMentions.get(existingId);
                    if ((existingAnno.getEnd() - existingAnno.getBegin()) <= (anno.getEnd() - anno.getBegin())) {
                        logger.finest("DateMention: \"" + anno.getCoveredText() + "\"(as " + anno.getClass().getSimpleName()
                                + ") replaces the existing DateMention: \"" + existingAnno.getCoveredText() + "\"(as "
                                + existingAnno.getClass().getSimpleName() + ")");
                        checker.remove(span);
                        scheduleToRemoveIds.add(existingId);
                        checker.put(span, i);
                    } else {
                        logger.finest("DateMention: \"" + anno.getCoveredText() + "\"(as " + anno.getClass().getSimpleName()
                                + ") is covered by the existing DateMention: \"" + existingAnno.getCoveredText() + "\"(as "
                                + existingAnno.getClass().getSimpleName() + ")");
                        scheduleToRemoveIds.add(i);
                    }
                }
            } else {
                logger.finest("DateMention: \"" + anno.getCoveredText() + "\"(as " + anno.getClass().getSimpleName() + ") doesn't have any overlap.");
                checker.put(span, i);
            }
        }
        for (int i = 0; i < allDateMentions.size(); i++) {
            if (!scheduleToRemoveIds.contains(i)) {
                Annotation anno = allDateMentions.get(i);
                anno.addToIndexes();
                logger.finest("DateMention: \"" + anno.getCoveredText() + "\"(as " + anno.getClass().getSimpleName() + ") added to index");
            }
        }

    }

    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        LinkedHashMap<String, TypeDefinition> typeDefs = super.getTypeDefs(ruleStr);
        typeDefs.remove(DeterminantValueSet.TEMPORAL_CATEGORIES1.substring(1));
        for (String typeName : typeDefs.keySet()) {
            TypeDefinition typeDef = typeDefs.get(typeName);
            String superTypeName = typeDef.getFullSuperTypeName();
//          force to use Date as super type to inherit its attributes.
            if (superTypeName.equals(Concept.class.getCanonicalName())) {
                typeDef.fullSuperTypeName = "edu.utah.bmi.nlp.type.system.Date";
                typeDef.shortSuperTypeName = "Date";
                typeDefs.put(typeName, typeDef);
            }
        }
        return typeDefs;
    }


}