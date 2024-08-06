package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.IntervalST;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pojava.datetime.DateTimeConfig;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is designed to find the note taken datetime in a clinical note, and update the corresponding note datetime.
 * Because the notes used in the EDW pulled from HL7 messages when a note is assigned, the recorded note datetime is not accurate.
 *
 * @author Jianlin Shi on 06/18/2019.
 */
public class TemporalMetaAnnotator_AE extends TemporalAnnotator_AE {
    public static Logger logger = IOUtil.getLogger(TemporalMetaAnnotator_AE.class);
    //  If positive integer, the found datetime mention must locate left to the offset to the document begin, if multiple, choose the left most;
//  If negative, the datetime mention must locate right to the offset to the document end, if multiple, choose the right most
//  If  0, then no constrain, if multiple, choose the left most.
    public static final String PARAM_POS_CONSTRAINS = "PositionConstrain";
    public static final String PARAM_DOC_COLUMN_NAME = "NotSerializeColumnNames";
    public static final String PARAM_AUTO_EXPAND_SCOPE = "AutoExpandScope";
    public static final String PARAM_DIFF_SOLVE_METHOD = "DiffSolveMethod";

    /**
     * Value set of PARAM_DIFF_SOLVE_METHOD
     */
    public static final String STR_PRIORITY = "STRUCTURAL", TEXT_PRIORITY = "TEXTUAL", EARLY_PRIORITY = "EARLY", LATE_PRIORITY = "LATE";


    public static final Pattern p = Pattern.compile("\\d\\d[/-]\\d\\d[/-]\\d{2,4} +(\\d\\d\\d\\d).*");

    public static final DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    protected int positionConstrain = 0;

    protected boolean autoExpandScope = false;

    protected String notSerializeColumns = "TEXT";

    protected String diffSolveMethod = EARLY_PRIORITY;

    protected HashMap<String, Class> dateClasses = new HashMap<>();

    public void initialize(UimaContext cont) {
        super.initialize(cont);
        Object obj = cont.getConfigParameterValue(PARAM_POS_CONSTRAINS);
        if (obj instanceof Integer)
            positionConstrain = (Integer) obj;

        obj = cont.getConfigParameterValue(PARAM_DOC_COLUMN_NAME);
        if (obj != null && obj instanceof String)
            notSerializeColumns = (String) obj;

        HashSet<String> valueSet = new HashSet<>();
        valueSet.addAll(Arrays.asList(new String[]{STR_PRIORITY, TEXT_PRIORITY, EARLY_PRIORITY, LATE_PRIORITY}));

        obj = cont.getConfigParameterValue(PARAM_DIFF_SOLVE_METHOD);
        if (obj != null && obj instanceof String)
            if (valueSet.contains(obj))
                diffSolveMethod = (String) obj;
            else
                logger.info("Value: '" + obj + "' is not a legal value for parameter: '" + PARAM_DIFF_SOLVE_METHOD + "'. Use default value: '" + EARLY_PRIORITY + "' instead.");

        obj = cont.getConfigParameterValue(PARAM_AUTO_EXPAND_SCOPE);
        if (obj != null && obj instanceof Boolean)
            autoExpandScope = (Boolean) obj;
        saveInferredRecordDate = true;
    }

    public void process(JCas jcas) throws AnalysisEngineProcessException {
        dateAnnos.clear();
        RecordRow baseRecordRow = new RecordRow();
        FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
        SourceDocumentInformation e = null;
        if (it.hasNext()) {
            e = (SourceDocumentInformation) it.next();
            String serializedString = new File(e.getUri()).getName();
            baseRecordRow.deserialize(serializedString);
        }
        DateTime tableRecordDate = null;
        String dateString = (String) baseRecordRow.getValueByColumnName(recordDateColumnName);
        if (dateString != null)
            tableRecordDate = new DateTime(new org.pojava.datetime.DateTime(dateString).toDate());
        if (dateString == null || dateString.equals("NULL")) {
            logger.finest("Seems an null note record, skip");
            return;
        }
        dateString = (String) baseRecordRow.getValueByColumnName(referenceDateColumnName);
        if (dateString != null && !dateString.equals("NULL"))
            referenceDate = new DateTime(new org.pojava.datetime.DateTime(dateString).toDate());


        readReferenceDate(jcas, recordDateColumnName);
        String docText = jcas.getDocumentText();
        String shrinkedText = narrowScope(docText, 0);
        HashMap<String, ArrayList<Span>> dates = ((FastCNER) fastNER).processString(shrinkedText);
        if (autoExpandScope && dates.size() == 0) {
            shrinkedText = narrowScope(docText, 1);
            dates = ((FastCNER) fastNER).processString(shrinkedText);
        }
        Comparator<Span> compareByBegin = Comparator.comparingInt(Span::getBegin);
        for (String type : dates.keySet()) {
            ArrayList<Span> spans = dates.get(type);
            Collections.sort(spans, compareByBegin);
            Span span = spans.get(0);
            spans.clear();
            spans.add(span);
            dates.put(type, spans);
        }
        ArrayList<Annotation> allDateMentions = parseDateMentions(jcas, dates, tableRecordDate);
        coordinateDateMentions(allDateMentions, jcas.getDocumentText().length());

        Annotation recordDateAnno = null;
        if (allDateMentions.size() > 0)
            recordDateAnno = allDateMentions.get(0);

        if (recordDateAnno != null && e != null) {
            logger.finest("Find date time mention: " + recordDateAnno.getCoveredText());
            DateTime recordDate = parseDateString(recordDateAnno.getCoveredText(), tableRecordDate != null ? tableRecordDate : referenceDate);
            if (logger.isLoggable(Level.FINER)) {
                if (recordDate == null) {
                    logger.warning("NoteID: " + baseRecordRow.getStrByColumnName("DOC_ID") + " date parsing error: " + recordDateAnno.getCoveredText());
                } else if (tableRecordDate == null) {
                    logger.warning("NoteID: " + baseRecordRow.getStrByColumnName("DOC_ID") + " note date in table is null");
                } else if (recordDate.getDayOfMonth() != tableRecordDate.getDayOfMonth())
                    logger.finer("NoteID: " + baseRecordRow.getStrByColumnName("DOC_ID") + "\tDateInTable: " + tableRecordDate.toString(dateformat)
                            + "\tDateInNote: " + recordDate.toString(dateformat) + "\t" + recordDateAnno.getCoveredText());
            }
            String formatedDateString = recordDate.toString(dateformat);

            switch (diffSolveMethod) {
                case TEXT_PRIORITY:
                    baseRecordRow.addCell(recordDateColumnName, formatedDateString);
                    break;
                case EARLY_PRIORITY:
                    if (tableRecordDate == null || tableRecordDate.isAfter(recordDate)) {
                        baseRecordRow.addCell(recordDateColumnName, formatedDateString);
                    }
                    break;
                case LATE_PRIORITY:
                    if (tableRecordDate == null || tableRecordDate.isBefore(recordDate)) {
                        baseRecordRow.addCell(recordDateColumnName, formatedDateString);
                    }
                    break;

            }

            String metaInfor = baseRecordRow.serialize(notSerializeColumns.split("\\s*[,;:]\\s*"));
            e.setUri(metaInfor);
        } else {
            logger.finest("Not find date time in document: " + baseRecordRow.getStrByColumnName("DOC_ID"));
        }

    }

    protected String narrowScope(String docText, int shift) {
        if (positionConstrain > 0 && positionConstrain * (shift + 1) < docText.length()) {
            docText = StringUtils.repeat(" ", positionConstrain * shift) + docText.substring(positionConstrain * shift, positionConstrain * (shift + 1));
        } else if (positionConstrain < 0 && -positionConstrain * (shift + 1) < docText.length()) {
            docText = StringUtils.repeat(" ", docText.length() + positionConstrain * (shift + 1)) + docText.substring(docText.length() + positionConstrain * (shift + 1));
        }
        return docText;
    }

    protected DateTime parseDateString(String dateString, DateTime recordDate) {
        dateString = addColon(dateString);
        Date utilDate = null;
        try {
            if (recordDate != null) {
                utilDate = new org.pojava.datetime.DateTime(dateString, DateTimeConfig.getDateTimeConfig(recordDate.toDate())).toDate();
            } else {
                utilDate = new org.pojava.datetime.DateTime(dateString, DateTimeConfig.getDateTimeConfig(referenceDate.toDate())).toDate();
            }

        } catch (Exception e) {
            logger.finest("Illegal date string: " + dateString);
            logger.finest(e.toString());
        }


        DateTime date = new DateTime(utilDate);
        return date;
    }

    protected String addColon(String dateString) {
        Matcher m = p.matcher(dateString);
        if (m.find()) {
            String time = m.group(1);
            time = time.substring(0, 2) + ":" + time.substring(2);
            int start = m.start(1);
            int end = m.end(1);
            dateString = dateString.substring(0, start) + time + dateString.substring(end);

        }
        return dateString;
    }


    /**
     * For parse date mentions and save as annotations.
     *
     * @param jcas       JCas object
     * @param dates      List of date spans grouped by types
     * @param recordDate document record date
     * @return a list of date mention annotations
     */
    protected ArrayList<Annotation> parseDateMentions(JCas jcas, HashMap<String, ArrayList<Span>> dates,
                                                      DateTime recordDate) {
        String text = jcas.getDocumentText();
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
        for (Map.Entry<String, ArrayList<Span>> entry : dates.entrySet()) {
            String typeOfDate = entry.getKey();
            Class typeOfDateCls = getClassFromName(typeOfDate);
            if (edu.utah.bmi.nlp.type.system.Date.class.isAssignableFrom(typeOfDateCls)) {
                ArrayList<Span> dateMentions = entry.getValue();
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
                    addDateMentions(jcas, allDateMentions,
                            typeOfDate, certainty, span, 0, dt, getRuleInfo(span));
                }
            } else {
                switch (typeOfDate) {
                    case "Yeard":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 365, 0, new IntervalST<>());
                        break;
                    case "Monthd":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 30, 0, new IntervalST<>());
                        break;
                    case "Weekd":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 7, 0, new IntervalST<>());
                        break;
                    case "Dayd":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeNumericTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 1, 0, new IntervalST<>());
                        break;
                    case "Yearw":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 365, 0, new IntervalST<>());
                        break;
                    case "Monthw":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 30, 0, new IntervalST<>());
                        break;
                    case "Weekw":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 7, 0, new IntervalST<>());
                        break;
                    case "Dayw":
                        for (Span span : entry.getValue())
                            inferDateFromRelativeLiteralTime(jcas, allDateMentions, typeOfDate, text, span, recordDate, 1, 0, new IntervalST<>());
                        break;
                }
            }
        }
        return allDateMentions;
    }

    protected Class getClassFromName(String typeOfDate) {
        if (dateClasses.containsKey(typeOfDate)) {
            return dateClasses.get(typeOfDate);
        }
        Class typeOfDateCls = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(typeOfDate));
        dateClasses.put(typeOfDate, typeOfDateCls);
        return typeOfDateCls;
    }
}
