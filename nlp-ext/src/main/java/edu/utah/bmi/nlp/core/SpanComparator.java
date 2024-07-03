package edu.utah.bmi.nlp.core;

import java.util.Comparator;

public class SpanComparator implements Comparator<Span> {
    public SpanComparator() {
    }

    public int compare(Span o1, Span o2) {
        if (o1.getBegin() > o2.getBegin()) {
            return 1;
        } else {
            return o1.getBegin() < o2.getBegin() ? -1 : 0;
        }
    }
}
