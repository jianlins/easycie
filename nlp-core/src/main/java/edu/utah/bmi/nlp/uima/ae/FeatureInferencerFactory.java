package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.IOUtil;

public class FeatureInferencerFactory {

    public static FeatureInferencerInf getFeatureInferencer(String ruleStr) {
        return getFeatureInferencer(ruleStr, true, false,false);
    }

    public static FeatureInferencerInf getFeatureInferencer(String ruleStr, boolean removeEvidenceConcept,
                                                            boolean strictNameMatch,boolean noteRuleId) {
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        String version = ioUtil.getSetting("version");
        switch (version) {
            case "2":
            case "2.0":
            case "v2":
//              support priority weight for multiple matched rules on the same topic
//              so that inferencer can generate conclusions from mutual exclusive conditions,
//              without explicitly list all the possible conditions.
                return new FeatureAnnotationInferencerEx(ioUtil, removeEvidenceConcept, strictNameMatch, noteRuleId);
            default:
//               legacy version of rule format, no priority weights
                return new FeatureAnnotationInferencer(ioUtil, removeEvidenceConcept, strictNameMatch,noteRuleId);
        }
    }
}

