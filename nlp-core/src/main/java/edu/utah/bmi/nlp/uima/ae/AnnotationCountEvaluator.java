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
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * For debugging purpose
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class AnnotationCountEvaluator extends JCasAnnotator_ImplBase {

    public static final String PARAM_TYPE_NAME = "TypeName";
    public static final String PARAM_TYPE_COUNT = "TypeCount";
    //  feature value pairs in a format like: featurename1, value1, featurename2, value2...
    public static final String PARAM_FEATURE_VALUES = "FeatureValues";
    private String typeName;
    public static boolean pass = true;
    private int count;
    private LinkedHashMap<String, String> featureValues = new LinkedHashMap<>();

    public void initialize(UimaContext cont) {
        typeName = "";
        Object obj = cont.getConfigParameterValue(PARAM_TYPE_NAME);
        if (obj != null && obj instanceof String) {
            typeName = DeterminantValueSet.checkNameSpace((String) obj);
        }
        obj = cont.getConfigParameterValue(PARAM_TYPE_COUNT);
        if (obj != null && obj instanceof Integer) {
            count = (int) obj;
        }
        obj = cont.getConfigParameterValue(PARAM_FEATURE_VALUES);
        if (obj != null && obj instanceof String) {
            String[] fvs = ((String) obj).split(",\\s*");
            for (int i = 0; i < fvs.length / 2; i += 2) {
                featureValues.put(fvs[i], fvs[i + 1].trim());
            }
        }
    }


    public void process(JCas jCas) throws AnalysisEngineProcessException {
        CAS cas = jCas.getCas();
        Type type = CasUtil.getAnnotationType(cas, typeName);
        Collection<AnnotationFS> annos = CasUtil.select(cas, type);
        int counter = 0;
        for (AnnotationFS anno : annos) {
            if (checkFeatureValues(type, anno))
                counter++;
        }
        if (counter != count)
            pass = false;
        else
            pass = true;
    }

    private boolean checkFeatureValues(Type type, AnnotationFS anno) {
        for (String featureName : featureValues.keySet()) {
            switch (featureName) {
                case "Text":
                case "CoveredText":
                    if (!anno.getCoveredText().equals(featureValues.get(featureName))) {
                        return false;
                    }
                    break;
                case "Begin":
                    if (!(anno.getBegin() + "").equals(featureValues.get(featureName))) {
                        return false;
                    }
                    break;
                case "End":
                    if (!(anno.getEnd() + "").equals(featureValues.get(featureName))) {
                        return false;
                    }
                    break;
                default:
                    Feature fObj = type.getFeatureByBaseName(featureName);
                    if (fObj == null)
                        return false;
                    String value = anno.getFeatureValueAsString(fObj);
                    if (!value.equals(featureValues.get(featureName)))
                        return false;

            }
        }
        return true;
    }

}
