package edu.utah.bmi.nlp.sectiondectector;

import edu.utah.bmi.nlp.core.Span;

import java.util.Comparator;

public class SpanComparator implements Comparator<Span> {
    public int compare(Span o1, Span o2) {
        if (o1.getBegin() > o2.getBegin()) {
            return 1;
        } else if (o1.getBegin() < o2.getBegin()) {
            return -1;
        }
        return 0;
    }
}
