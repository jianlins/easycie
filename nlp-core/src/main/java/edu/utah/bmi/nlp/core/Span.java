/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.utah.bmi.nlp.core;

/**
 * This class stores the span information of the evidence support the corresponding Determinants
 * <p>
 * The field width is used to prioritize the wider rulesMap. Instead of using width, you can implement your own scores to
 * prioritize the rulesMap.
 *
 * @author Jianlin Shi
 */
public class Span implements Comparable<Span> {

    public int begin, end, width, ruleId;
    public String text;
    public double score = 0d;

    public Span() {

    }

    public Span(int begin, int end) {
        this.begin = begin;
        this.end = end;
        this.width = end - begin + 1;
    }

    public Span(int begin, int end, String text) {
        this.begin = begin;
        this.end = end;
        this.width = end - begin + 1;
        this.text = text;
    }

    public Span(int begin, int end, int ruleId) {
        this.begin = begin;
        this.end = end;
        this.ruleId = ruleId;
    }

    public Span(int begin, int end, int ruleId, double score) {
        this.begin = begin;
        this.end = end;
        this.ruleId = ruleId;
        this.score = score;
    }

    public Span(int begin, int end, int ruleId, double score, String text) {
        this.begin = begin;
        this.end = end;
        this.ruleId = ruleId;
        this.score = score;
        this.text = text;
    }

    @Override
    public int compareTo(Span o) {
        if (o == null)
            return -1;
        if (begin != o.begin)
            return begin - o.begin;
        else
            return end - o.end;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String toString() {
        String text=getText();
        if (text==null)
        return String.format("Span(%s-%s)", getBegin(), getEnd());
        else
            return String.format("Span(%s-%s): '%s'", getBegin(), getEnd(), text);
    }

    public String serialize() {
        return "(Rule" + this.ruleId + ": " + getBegin() + "-" + getEnd() + ":" + score + "):" + getText();
    }

    public static Span deserialize(String serialzied) {
        String[] items = serialzied.trim().split("(:\\s*|\\):|-)");
        if (items.length < 5) {
            System.err.println("Format error to deserialize string to Span:\n" + serialzied);
            return null;
        }
        int ruleId = Integer.parseInt(items[0].substring(5));
        int begin = Integer.parseInt(items[1]);
        int end = Integer.parseInt(items[2]);
        double score = Double.parseDouble(items[3]);
        String text = items[4];
        return new Span(begin, end, ruleId, score, text);
    }
}
