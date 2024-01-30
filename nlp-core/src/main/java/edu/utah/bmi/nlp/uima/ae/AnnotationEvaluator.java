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

import java.util.Iterator;

/**
 * For debugging purpose
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class AnnotationEvaluator extends JCasAnnotator_ImplBase {

    public static final String PARAM_TYPE_NAME = "TypeName";
    public static final String PARAM_ANNO_IND = "AnnotationIndex";
    public static final String PARAM_FEATURE_NAME = "FeatureName";
    public static final String PARAM_FEATURE_VALUE = "FeatureValue";
    public static final String PARAM_BEGIN = "Begin";
    public static final String PARAM_END = "End";
    private String typeName, featureName, featureValue;
    private int begin, end;
    public static boolean pass = true;
    private int ind;


    public void initialize(UimaContext cont) {
        typeName = "";
        Object obj = cont.getConfigParameterValue(PARAM_TYPE_NAME);
        if (obj != null && obj instanceof String) {
            typeName = DeterminantValueSet.checkNameSpace((String) obj);
        }
        obj = cont.getConfigParameterValue(PARAM_ANNO_IND);
        if (obj != null && obj instanceof Integer) {
            ind = (int) obj;
        }
        obj = cont.getConfigParameterValue(PARAM_FEATURE_NAME);
        if (obj != null && obj instanceof String) {
            featureName = (String) obj;
        }
        obj = cont.getConfigParameterValue(PARAM_FEATURE_VALUE);
        if (obj != null && obj instanceof String) {
            featureValue = (String) obj;
        }
        obj = cont.getConfigParameterValue(PARAM_BEGIN);
        if (obj != null && obj instanceof Integer) {
            begin = (int) obj;
        } else {
            begin = -1;
        }
        obj = cont.getConfigParameterValue(PARAM_END);
        if (obj != null && obj instanceof Integer) {
            end = (int) obj;
        } else {
            end = -1;
        }


    }


    public void process(JCas jCas) {
        CAS cas = jCas.getCas();
        Type type = CasUtil.getAnnotationType(cas, typeName);
        Iterator<AnnotationFS> iter = CasUtil.iterator(cas, type);
        AnnotationFS anno = null;
        int i = 0;
        while (iter.hasNext() && i <= ind) {
            anno = iter.next();
        }
        pass = anno != null;
        Feature fObj = type.getFeatureByBaseName(featureName);
        if (fObj == null)
            pass = false;
        else {
            String value = anno.getFeatureValueAsString(fObj);
            if (!value.equals(featureValue))
                pass = false;
        }
        if (begin > -1 && end > -1) {
            if (anno.getBegin() != begin || anno.getEnd() != end)
                pass = false;
        }
    }
}
