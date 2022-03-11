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

package edu.utah.bmi.nlp.uima.ae;


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.Interval1D;
import edu.utah.bmi.nlp.core.IntervalST;
import edu.utah.bmi.nlp.core.TypeDefinition;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * From one type of annotation's feature values, infer another type of annotation.
 * For instance, a negated Concept can be inferred to PseudoConcept;
 * a historical Cocnept can be infered to PastConcept.
 *
 * @author Jianlin Shi
 * Created on 7/6/16.
 */
public class FeatureInferenceAnnotator extends JCasAnnotator_ImplBase implements RuleBasedAEInf {


    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    @ConfigurationParameter(name = PARAM_RULE_STR, mandatory = true)
    private String ruleStr;

    public static final String PARAM_REMOVE_EVIDENCE_CONCEPT = "RemoveEvidenceConcept";
    @ConfigurationParameter(name = PARAM_REMOVE_EVIDENCE_CONCEPT, mandatory = false, defaultValue = "true",
            description = "whether remove the evidence concept annotation.")
    private boolean removeEvidenceConcept;


    public static final String PARAM_REMOVE_OVERLAP = "RemoveOverlap";
    @ConfigurationParameter(name = PARAM_REMOVE_OVERLAP, mandatory = false, defaultValue = "true",
            description = "whether remove overlapped results.")
    private boolean removeOverlap;

    //  The default is false. If use strict name match, no child types of an evidence annotation type will be considered.
    public static final String PARAM_STRICT_NAME_MATCH = "StrictNameMatch";
    @ConfigurationParameter(name = PARAM_STRICT_NAME_MATCH, mandatory = false, defaultValue = "false",
            description = "whether use strict name match.")
    private boolean strictNameMatch;

    public static final String PARAM_NOTE_RULE_ID = "NoteRuleId";
    @ConfigurationParameter(name = PARAM_NOTE_RULE_ID, mandatory = false, defaultValue = "false",
            description = "whether add matched rule id into Note feature.")
    private boolean noteRuleId;


    @Deprecated
    public static final String PARAM_DEBUG = "EnableDebug";

    @Deprecated
    private boolean debug = false;
    private FeatureInferencerInf featureAnnotationInferencer;


    public void initialize(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
        featureAnnotationInferencer =
                FeatureInferencerFactory.getFeatureInferencer(ruleStr, removeEvidenceConcept, strictNameMatch,noteRuleId);
        featureAnnotationInferencer.init();
    }


    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        featureAnnotationInferencer.processInferences(jCas);
        if (removeOverlap) {
            removeOverlap(jCas);
        }
    }


    private void removeOverlap(JCas jCas) {
        HashMap<String, IntervalST> typeSpanMap = new HashMap<>();
        String typeName="";
        for(Annotation anno: JCasUtil.select(jCas, Annotation.class)){
            typeName = anno.getType().getName();
            IntervalST thisSpanTree = typeSpanMap.getOrDefault(typeName, new IntervalST());
            checkOverlap(thisSpanTree, anno);
        }
    }

    /**
     * Because FastNER and FastCNER may have overlapped matches.
     *
     * @param intervalTree
     * @param concept
     */
    private void checkOverlap(IntervalST intervalTree, Annotation concept) {
        Interval1D interval = new Interval1D(concept.getBegin(), concept.getEnd());
        Annotation overlapped = (Annotation) intervalTree.get(interval);
        if (overlapped != null && (overlapped.getEnd() != concept.getBegin() && concept.getEnd() != overlapped.getBegin())) {
            if ((overlapped.getEnd() - overlapped.getBegin()) < (concept.getEnd() - concept.getBegin())) {
                overlapped.removeFromIndexes();
                intervalTree.remove(new Interval1D(overlapped.getBegin(), overlapped.getEnd()));
                intervalTree.put(interval, concept);
            } else {
                concept.removeFromIndexes();
            }
        } else {
            intervalTree.put(interval, concept);
        }
    }

    /**
     * Because implement a reinforced interface method (static is not reinforced), this is deprecated, just to
     * enable back-compatibility.
     *
     * @param ruleStr Rule file path or rule content string
     * @return Type name--Type definition map
     */
    @Deprecated
    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleStr) {
        return new FeatureInferenceAnnotator().getTypeDefs(ruleStr);
    }


    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        return FeatureInferencerFactory.getFeatureInferencer(ruleStr).getTypeDefinitions();
    }
}
