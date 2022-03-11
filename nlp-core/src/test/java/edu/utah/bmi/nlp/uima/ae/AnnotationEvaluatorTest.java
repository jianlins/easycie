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

import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Created by Jianlin Shi on 6/25/17.
 */
public class AnnotationEvaluatorTest {
    @Test
    public void process() throws Exception {
        String typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        AdaptableUIMACPERunner runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        runner.reInitTypeSystem("target/generated-test-sources/customized");
        JCas jCas = runner.initJCas();
        jCas.setDocumentText("test documents");
        Concept concept = new Concept(jCas, 1, 2);
        concept.setNegation("negated");
        concept.addToIndexes();
//        runner.addAnalysisEngine(AnnotationEvaluator.class, new Object[]{AnnotationEvaluator.PARAM_TYPE_NAME, "Concept",
//                AnnotationEvaluator.PARAM_ANNO_IND, 0, AnnotationEvaluator.PARAM_FEATURE_NAME, "Negation", AnnotationEvaluator.PARAM_FEATURE_VALUE, "negated"});
        AnalysisEngine evaluator = AnalysisEngineFactory.createEngine(AnnotationEvaluator.class,
                AnnotationEvaluator.PARAM_TYPE_NAME, "Concept",
                AnnotationEvaluator.PARAM_ANNO_IND, 0,
                AnnotationEvaluator.PARAM_FEATURE_NAME, "Negation",
                AnnotationEvaluator.PARAM_FEATURE_VALUE, "negated");
        evaluator.process(jCas);
        System.out.println(AnnotationEvaluator.pass);

    }

}