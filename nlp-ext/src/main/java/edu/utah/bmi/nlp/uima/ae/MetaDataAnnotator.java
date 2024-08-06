package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.AnnotationDefinition;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.pojava.datetime.DateTime;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use meta data to create annotations for down steam inferences
 */
public class MetaDataAnnotator extends JCasAnnotator_ImplBase implements RuleBasedAEInf {
    public static Logger logger = IOUtil.getLogger(MetaDataAnnotator.class);
    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    public static final String PARAM_MATCH_MULTIPLE = "MatchMultiple";
    private ArrayList<ArrayList<Object>> rules;
    private final static int NUMERIC_RULE = 1, CATEGORICAL_RULE = 0, CALCULATION_RULE = 2, DATE_DIFF_RULE = 3;
    private HashMap<String, String> defaultDocTypes = new HashMap<>();
    private HashMap<String, String> valueFeatureMap = new HashMap<>();
    private LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();
    private ArrayList<ArrayList<String>> ruleCells = new ArrayList<>();
    private boolean matchMultiple = false;

    private boolean exactMatchCategoryRules = true;

    private int version = 1;

    private HashMap<String, String> resultGrouping = new HashMap<>();

    public void initialize(UimaContext cont) {
        /***
         * Rule format
         * ConclusionType|tColumnName\tCondition
         * Condition examples:
         * 1.   >3
         * 2.   CONTAIN:Progress_Note;H&P
         *
         * categorical condition can add matching method
         * CONTAIN: will return true if a given value contains any of the condition values.
         * EXACT: will return true if a given value exactly match any of the condition values. (Default)
         *
         */

        String inferenceStr = (String) cont.getConfigParameterValue(PARAM_RULE_STR);
        rules = parseRuleStr(inferenceStr);

        Object value = cont.getConfigParameterValue(PARAM_MATCH_MULTIPLE);
        if (value instanceof Boolean && (boolean) value) {
            matchMultiple = true;
        }
    }

    protected ArrayList<ArrayList<Object>> parseRuleStr(String ruleStr) {
        ArrayList<ArrayList<Object>> rules = new ArrayList<>();
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        typeDefinitions = getTypeDefs(ruleStr, ioUtil);

        int col=0;
        if (ioUtil.getSetting("version").equals("2")) {
            this.version = 2;
            col = 1;
        }
        for (ArrayList<String> row : ioUtil.getRuleCells()) {
            String conclusion = row.get(1 + col).trim();
            if (this.version == 2) {
                String groupName = row.get(col).trim();
                this.resultGrouping.put(conclusion, groupName);
            }
            String columnName = row.get(2 + col).trim();
            String condition = row.get(3 + col).trim();
            int conditionType = isNumbericRule(condition);
            ArrayList<Object> rule = new ArrayList<>();
            rule.add(conditionType);
            rule.add(columnName);
            switch (conditionType) {
                case NUMERIC_RULE:
                    ArrayList<Object> numeric_conditions = parseNumericCondition(condition);
                    rule.add(numeric_conditions);
                    rule.add(conclusion);
                    break;
                case CATEGORICAL_RULE:
                    HashSet<String> valueSet = new HashSet<>();
                    String[] conds = condition.split(":");
                    if (conds.length > 0) {
                        exactMatchCategoryRules = conds[0].equals("EXACT");
                        condition = conds[0];
                    }
                    for (String value : condition.split("[;,\\|]")) {
                        valueSet.add(value);
                    }
                    rule.add(valueSet);
                    rule.add(conclusion);
                    break;
                case CALCULATION_RULE:
                case DATE_DIFF_RULE:
                    rule.addAll(row.subList(3+col, 5+col));
                    numeric_conditions = parseNumericCondition(row.get(5+col));
                    rule.add(numeric_conditions);
                    rule.add(conclusion);
                    break;
            }
            rules.add(rule);
        }
        return rules;
    }

    protected ArrayList<Object> parseNumericCondition(String condition) {
        ArrayList<Object> conditions = new ArrayList<>();
        int whitespace = 0, num = 1, operator = 2;
        int previousCharType = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : condition.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                if (previousCharType == num || previousCharType == operator)
                    sb.append(c);
                previousCharType = num;
            } else if (Character.isWhitespace(c)) {
                if (previousCharType == num && sb.length() > 0)
                    conditions.add(Double.parseDouble(sb.toString()));
                sb.setLength(0);
                previousCharType = whitespace;
                continue;
            } else if (c == '<' || c == '>' || c == '=') {
                if (previousCharType == num)
                    conditions.add(Double.parseDouble(sb.toString()));
                conditions.add(c);
                sb.setLength(0);
                previousCharType = operator;
            }

        }
        if (sb.length() > 0)
            conditions.add(Double.parseDouble(sb.toString()));
        return conditions;
    }

//    private void buildConstructor(TypeDefinition typeDefinition) {
//        Class docType;
//        try {
//            if (!docTypeConstructorMap.containsKey(typeDefinition.shortTypeName)) {
//                docType = AnnotationOper.getTypeClass(typeDefinition.fullTypeName);
//                Constructor cc = docType.getConstructor(JCas.class, int.class, int.class);
//                docTypeConstructorMap.put(typeDefinition.shortTypeName, cc);
//            }
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
//
//    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        RecordRow baseRecordRow = new RecordRow();
        FSIterator it = aJCas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
        if (it.hasNext()) {
            SourceDocumentInformation e = (SourceDocumentInformation) it.next();
            String serializedString = new File(e.getUri()).getName();
            baseRecordRow.deserialize(serializedString);
        }
//        Object recordDate = baseRecordRow.getValueByColumnName((String) rules.get(0).get(1));
//        if (recordDate == null || recordDate.equals("NULL")) {
//            logger.finest("This is likely an NULL record, skip this.");
//            return;
//        }
        if (logger.isLoggable(Level.FINEST)) {
            SourceDocumentInformation so = JCasUtil.selectByIndex(aJCas, SourceDocumentInformation.class, 0);
            RecordRow recordRow = new RecordRow();
            recordRow.deserialize(so.getUri());
            logger.finest("Meta inference for: " + recordRow.getStrByColumnName("DOC_ID") + "\t" + recordRow.getStrByColumnName("DATE") + "\t" + recordRow.getStrByColumnName("DIFF_DTM"));

        }
        ArrayList<String> conclusionTypes = getAnnotationTypes(baseRecordRow);
        if (this.version == 2 && resultGrouping.size() > 1) {
            for (String typeName : pruneConclusions(conclusionTypes, matchMultiple, resultGrouping)) {
                saveAnnotation(aJCas, typeName);
            }
        } else {
            if (matchMultiple) {
                for (String typeName : conclusionTypes) {
                    saveAnnotation(aJCas, typeName);
                }
            } else {
                if (conclusionTypes.size() > 0)
                    saveAnnotation(aJCas, conclusionTypes.get(0));
                else
                    logger.warning("MetaDataAnnotator doesn't find any matched rule for: \n" + baseRecordRow.toString());
            }
        }
    }

    private Collection<String> pruneConclusions(ArrayList<String> conclusionTypes, boolean matchMultiple, HashMap<String, String> resultGrouping) {
        if (matchMultiple)
            return conclusionTypes;
        HashMap<String, String> outputTypes = new HashMap<>();
        for (String typeName : conclusionTypes) {
            String groupName = resultGrouping.getOrDefault(typeName, "");
            if (!outputTypes.containsKey(groupName)) {
                outputTypes.put(groupName, typeName);
            }
        }
        return outputTypes.values();

    }

    private void saveAnnotation(JCas aJCas, String typeName) {
        Token anno = JCasUtil.selectByIndex(aJCas, Token.class, 0);
        if (anno == null) {
            anno = new Token(aJCas, 0, 1);
        }
        AnnotationDefinition annoDef=new AnnotationDefinition(typeDefinitions.get(typeName));
        Annotation conclusionAnno = AnnotationOper.createAnnotation(aJCas, annoDef, AnnotationOper.getTypeClass(annoDef.fullTypeName), anno.getBegin(), anno.getEnd());
        conclusionAnno.addToIndexes();
    }

    private ArrayList<String> getAnnotationTypes(RecordRow baseRecordRow) {
        ArrayList<String> types = new ArrayList<>();
        for (ArrayList<Object> rule : rules) {

            String columnName = (String) rule.get(1);
            String value = baseRecordRow.getStrByColumnName(columnName);
            if (value == null || value.length() == 0) {
                logger.finest("Column: " + columnName + " is not included  in the meta data.");
                continue;
            }
            switch ((int) rule.get(0)) {
                case NUMERIC_RULE:
                    if (evalNumericCondition((ArrayList<Object>) rule.get(2), Double.parseDouble(value))) {
                        types.add((String) rule.get(3));
                    }
                    break;
                case CATEGORICAL_RULE:
                    if (evalCategoricalCondition((HashSet<String>) rule.get(2), value)) {
                        types.add((String) rule.get(3));
                    }
                    break;
                case CALCULATION_RULE:
                    String value1 = baseRecordRow.getStrByColumnName((String) rule.get(1));
                    String value2 = baseRecordRow.getStrByColumnName((String) rule.get(3));
                    double d1 = Double.parseDouble(value1);
                    double d2 = Double.parseDouble(value2);
                    switch (((String) rule.get(2)).charAt(0)) {
                        case '-':
                            if (evalNumericCondition((ArrayList<Object>) rule.get(4), d1 - d2)) {
                                types.add((String) rule.get(5));
                            }
                            break;
                        case '+':
                            if (evalNumericCondition((ArrayList<Object>) rule.get(4), d1 + d2)) {
                                types.add((String) rule.get(5));
                            }
                            break;
                    }
                    break;
                case DATE_DIFF_RULE:
                    String date1value = baseRecordRow.getStrByColumnName((String) rule.get(1));
                    String date2value = baseRecordRow.getStrByColumnName((String) rule.get(3));
                    if (date1value == null || date2value == null || date1value.equals("NULL") || date2value.equals("NULL")) {
                        return types;
                    }
                    LocalDateTime date1 = new DateTime(date1value).toTimestamp().toLocalDateTime();
                    LocalDateTime date2 = new DateTime(date2value).toTimestamp().toLocalDateTime();
                    Duration dur = Duration.between(date2, date1);
                    switch (((String) rule.get(2)).charAt(1)) {
                        case 'h':
                            if (evalNumericCondition((ArrayList<Object>) rule.get(4), dur.toHours())) {
                                types.add((String) rule.get(5));
                            }
                            break;
                        case 'm':
                            if (evalNumericCondition((ArrayList<Object>) rule.get(4), dur.toMinutes())) {
                                types.add((String) rule.get(5));
                            }
                            break;
                        case 'd':
                            if (evalNumericCondition((ArrayList<Object>) rule.get(4), dur.toDays())) {
                                types.add((String) rule.get(5));
                            }
                            break;
                    }
                    break;

            }
        }
        return types;
    }

    private boolean evalCategoricalCondition(HashSet<String> valueSet, String value) {
        if (exactMatchCategoryRules) {
            if (valueSet.contains(value))
                return true;
        } else {
            for (String v : valueSet) {
                if (value.contains(v))
                    return true;
            }
        }
        return false;
    }

    private boolean evalNumericCondition(ArrayList<Object> conditions, double value) {
        for (int i = 0; i < conditions.size() - 1; i += 2) {
            char operator = (char) conditions.get(i);
            double boundaryValue = (double) conditions.get(i + 1);
            switch (operator) {
                case '<':
                    if (value >= boundaryValue)
                        return false;
                    break;
                case '>':
                    if (value <= boundaryValue)
                        return false;
                    break;
                case '=':
                    if (value != boundaryValue)
                        return false;
                    break;
            }

        }
        return true;
    }

    private int isNumbericRule(String condition) {
        if (condition.indexOf("<") > -1 || condition.indexOf(">") > -1 || condition.indexOf("=") > -1) {
            return NUMERIC_RULE;
        } else if (condition.equals("-") || condition.equals("+")) {
            return CALCULATION_RULE;
        } else if (condition.startsWith("-") && condition.trim().length() == 2)
            return DATE_DIFF_RULE;
        return CATEGORICAL_RULE;
    }

    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        if (ruleStr.indexOf("|") != -1) {
            ruleStr = ruleStr.replaceAll("\\|", "\n");
        }
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        typeDefinitions = getTypeDefs(ruleStr, ioUtil);
        return typeDefinitions;
    }

    protected LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr, IOUtil ioUtil) {
        if (ioUtil.getInitiations().size() > 1) {
            UIMATypeFunctions.getTypeDefinitions(ruleStr, ruleCells,
                    valueFeatureMap, new HashMap<>(), defaultDocTypes, typeDefinitions);

        }
        return typeDefinitions;
    }
}
