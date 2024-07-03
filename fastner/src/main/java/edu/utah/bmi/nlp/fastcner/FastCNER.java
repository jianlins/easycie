//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastner.FastNER;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.uima.jcas.tcas.Annotation;

public class FastCNER extends FastNER {
    public FastCNER(String ruleFile) {
        this.initiate(ruleFile, true);
    }

    public FastCNER(String ruleFile, boolean constructRuleMap) {
        this.initiate(ruleFile, true, constructRuleMap);
    }

    public FastCNER(HashMap<Integer, NERRule> ruleStore) {
        this.initiate(ruleStore);
    }

    protected void initiate(HashMap<Integer, NERRule> ruleStore) {
        this.fastRule = new FastCRuleSB(ruleStore);
    }

    public HashMap<String, ArrayList<Span>> processString(String text) {
        return this.fastRule.processString(text);
    }

    public HashMap<String, ArrayList<Span>> processSpan(Span span) {
        return ((FastCRule)this.fastRule).processSpan(span);
    }

    public HashMap<String, ArrayList<Span>> processAnnotation(Annotation sentence) {
        Span span = new Span(sentence.getBegin(), sentence.getEnd(), sentence.getCoveredText());
        return this.processSpan(span);
    }

    public void setReplicationSupport(boolean support) {
        ((FastCRule)this.fastRule).setReplicationSupport(support);
    }

    public void setCompareMethod(String method) {
        ((FastCRule)this.fastRule).setCompareMethod(method);
    }

    public void setSpecialCharacterSupport(Boolean scSupport) {
        ((FastCRule)this.fastRule).setSpecialCharacterSupport(scSupport);
    }

    public void setMaxRepeatLength(int maxRepeatLength) {
        ((FastCRule)this.fastRule).setMaxRepeatLength(maxRepeatLength);
    }
}
