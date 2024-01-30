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
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;

/**
 * For debugging purpose
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class AnnotationPrinter extends JCasAnnotator_ImplBase {

    public static final String PARAM_TYPE_NAME = "TypeName";
    @ConfigurationParameter(name = PARAM_TYPE_NAME, mandatory = false, defaultValue = "Annotation",
            description = "The type name to be printed.")
    private String printTypeName;


    public static final String PARAM_INDICATION = "Indication";
    @ConfigurationParameter(name = PARAM_INDICATION, mandatory = false, defaultValue = "",
            description = "Hint to the printed annotations.")
    private String indication;

    public void initialize(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
        printTypeName = DeterminantValueSet.checkNameSpace(printTypeName);
    }


    public void process(JCas jCas) throws AnalysisEngineProcessException {
        CAS cas = jCas.getCas();
        Type type = CasUtil.getAnnotationType(cas, printTypeName);
//        Collection<AnnotationFS> annotations = CasUtil.select(cas, type);
        Collection<? extends Annotation> annotations = JCasUtil.select(jCas, AnnotationOper.getTypeClass(printTypeName));
        for (AnnotationFS annotation : annotations) {
            System.out.println(indication + "\n Here is a list of annotation '" + printTypeName + "':");
            System.out.println(annotation.getClass());
            System.out.println(annotation + "   Covered Text: \"" + annotation.getCoveredText() + "\"".replaceAll("\n",""));

        }
    }
}
