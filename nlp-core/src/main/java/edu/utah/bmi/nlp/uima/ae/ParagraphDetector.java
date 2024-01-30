package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.Interval1D;
import edu.utah.bmi.nlp.core.IntervalST;
import edu.utah.bmi.nlp.type.system.Paragraph;
import edu.utah.bmi.nlp.type.system.Sentence;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParagraphDetector extends JCasAnnotator_ImplBase {
    private Pattern pat;
    public static String PARAM_SPLIT_PATTERN = "SplitPattern";
    public static String PARAM_MIN_LENGTH = "MinLength";
    //  avoid paragraph split within sentence.
    public static String PARAM_SENTENCE_COORDINATED = "SentenceCoordinated";
    private int minLength = 0;
    private boolean sentenceCoordinated = true;

    public void initialize(UimaContext cont) {
//      define paragraph splitter

        Object value = cont.getConfigParameterValue(PARAM_SPLIT_PATTERN);
        if (value != null && value instanceof String && ((String) value).trim().length() > 0)
            pat = Pattern.compile((String) value);
        else
            pat = Pattern.compile("\\s*((\r\n|\n\r){2,}|\n{2,}|\r{2,})\\s*");

        value = cont.getConfigParameterValue(PARAM_MIN_LENGTH);
        if (value instanceof Integer)
            minLength = (int) value;
        value = cont.getConfigParameterValue(PARAM_SENTENCE_COORDINATED);
        if (value != null && value instanceof Boolean) {
            sentenceCoordinated = (boolean) value;
        }

    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String text = aJCas.getDocumentText();
        int index = 0;
        int length = text.length();
        Matcher m = pat.matcher(text);
        ArrayList<int[]> matchList = new ArrayList<>();
        while (m.find()) {
            if (index == 0 && index == m.start() && m.start() == m.end()) {
                // no empty leading substring included for zero-width match
                // at the beginning of the input char sequence.
                continue;
            }
            matchList.add(new int[]{index, m.start()});
            index = m.end();
        }
        matchList.add(new int[]{index, length});
        IntervalST<Annotation> sentenceIndex = new IntervalST<>();
        if (sentenceCoordinated)
            sentenceIndex = AnnotationOper.indexAnnotation(aJCas, Sentence.class);
//        merge too short paragraphs
        ArrayList<int[]> mergedList = new ArrayList<>();
        int begin = -1;
        for (int i = 0; i < matchList.size(); i++) {
            int[] pos = matchList.get(i);
            if (begin == -1) {
                begin = pos[0];
            }
            if (notEnoughAlphaCharlength(text, begin, pos[1], minLength)) {
                continue;
            }
            if (sentenceCoordinated) {
                Interval1D overlapped = sentenceIndex.search(new Interval1D(pos[1] - 1, pos[1]));
                if (overlapped != null && (overlapped.min < pos[1] && pos[1] < overlapped.max)) {
                    continue;
                }
            }
            mergedList.add(new int[]{begin, pos[1]});
            begin = -1;
        }

        for (int[] pos : mergedList) {
            if (text.substring(pos[0], pos[1]).trim().length() > 0) {
                Paragraph paragraph = new Paragraph(aJCas, pos[0], pos[1]);
                paragraph.addToIndexes();
            }
        }
    }


    private boolean notEnoughAlphaCharlength(String text, int begin, int end, int minLength) {
        text = text.substring(begin, end);
        int measure = 0;
        for (char c : text.toCharArray()) {
            if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                measure++;
            }
        }
        return measure < minLength;
    }
}
