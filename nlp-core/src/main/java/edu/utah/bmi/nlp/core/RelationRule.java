package edu.utah.bmi.nlp.core;

public class RelationRule extends Rule {
    public TriggerTypes triggerType;
    public TriggerTypes direction;
    public int scopeBegin, scopeEnd;

    public RelationRule(int id, String rule, String ruleName, DeterminantValueSet.Determinants type) {
        super(id, rule, ruleName, 0, type);
    }

    public RelationRule(int id, String rule, String ruleName, TriggerTypes direction, TriggerTypes triggerType) {
        super(id, rule, ruleName, 0, DeterminantValueSet.Determinants.ACTUAL);
        this.triggerType = triggerType;
        this.direction = direction;
    }

    public RelationRule(int id, String rule, String ruleName, TriggerTypes direction, TriggerTypes triggerType, int scopeBegin, int scopeEnd) {
        super(id, rule, ruleName, 0, DeterminantValueSet.Determinants.ACTUAL);
        this.triggerType = triggerType;
        this.direction = direction;
        this.scopeBegin = scopeBegin;
        this.scopeEnd = scopeEnd;
    }

    public int getScopeBegin() {
        return scopeBegin;
    }

    public void setScopeBegin(int scopeBegin) {
        this.scopeBegin = scopeBegin;
    }

    public int getScopeEnd() {
        return scopeEnd;
    }

    public void setScopeEnd(int scopeEnd) {
        this.scopeEnd = scopeEnd;
    }

    public enum TriggerTypes {
        forward, backward, both, termination, pseudo, trigger
    }

    public RelationRule clone() {
        return new RelationRule(id, rule, ruleName, direction, triggerType, scopeBegin, scopeEnd);
    }
}
