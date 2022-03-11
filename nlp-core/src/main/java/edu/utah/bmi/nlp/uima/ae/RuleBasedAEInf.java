package edu.utah.bmi.nlp.uima.ae;

import edu.utah.bmi.nlp.core.TypeDefinition;

import java.util.LinkedHashMap;

public interface RuleBasedAEInf {

    /**
     * Use this method to generate rule-based type definitions from ruleStr. Will need to initiate an empty instance.
     * Because type definition is case sensitive, no matter whether the actual rules are case sensitive, the case
     * sensitive parameter is not necessary for this method.
     *
     * @param ruleStr The rule file path or rule content string
     * @return A map of type name to type definitions.
     */
    LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr);


}
