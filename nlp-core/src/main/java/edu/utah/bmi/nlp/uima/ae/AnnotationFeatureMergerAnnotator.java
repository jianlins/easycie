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
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Merge different instances of each type of annotations.
 * Feature values will be merged by priority (righter value has higher priority)
 *
 * @author Jianlin Shi
 * Created on 7/6/16.
 */
public class AnnotationFeatureMergerAnnotator extends JCasAnnotator_ImplBase implements RuleBasedAEInf {

    public static Logger logger = IOUtil.getLogger(AnnotationFeatureMergerAnnotator.class);

    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    @ConfigurationParameter(name = PARAM_RULE_STR, mandatory = true)
    private String ruleStr;

    public static final String PARAM_IN_SITU = "InSitu";
    @ConfigurationParameter(name = PARAM_IN_SITU, mandatory = false, defaultValue = "true",
            description = "Whether place the merged annotation in the 1st original annotation")
    private boolean insitu;


    @Deprecated
    public static final String PARAM_DEBUG = "EnableDebug";
    //   whether place the merged annotation in the 1st original annotation.

    @Deprecated
    private final boolean debug = false;
    private AnnotationFeatureMerger annotationFeatureMerger;


    public void initialize(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
        annotationFeatureMerger = new AnnotationFeatureMerger(ruleStr, insitu);
    }


    public void process(JCas jCas) throws AnalysisEngineProcessException {
        annotationFeatureMerger.mergeAnnotations(jCas);
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
        return AnnotationFeatureMerger.getTypeDefinitions(ruleStr);
    }


    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        return AnnotationFeatureMerger.getTypeDefinitions(ruleStr);
    }
}
