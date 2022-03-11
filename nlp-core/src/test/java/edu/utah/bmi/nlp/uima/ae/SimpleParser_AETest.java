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

import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.reader.StringMetaReader;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Created by
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class SimpleParser_AETest {
    private AdaptableUIMACPERunner runner;

    @BeforeEach
    public void init() throws ResourceInitializationException {
        String typeDescriptor = "desc/type/customized";
        if (!new File(typeDescriptor + ".xml").exists()) {
            typeDescriptor = "desc/type/All_Types";
        }
        runner = new AdaptableUIMACPERunner(typeDescriptor);
        String text = "The patient was admitted on 03/26/08\n and was started on IV antibiotics elevation" +
                ", was also counseled to minimizing the cigarette smoking. The patient had edema\n\n" +
                "\n of his bilateral lower extremities. The hospital consult was also obtained to " +
                "address edema issue question was related to his liver hepatitis C. Hospital consult" +
                " was obtained. This included an ultrasound of his abdomen, which showed just mild " +
                "cirrhosis. ";
        runner.setCollectionReader(StringMetaReader.class, new Object[]{StringMetaReader.PARAM_INPUT, text});
        runner.addAnalysisEngine(SimpleParser_AE.class,
                new Object[]{SimpleParser_AE.PARAM_SENTENCE_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Sentence",
                        SimpleParser_AE.PARAM_TOKEN_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Token"});
        runner.addAnalysisEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Sentence"});
    }

    @Test
    public void test1() {

//        runner.run();
    }
    @Test
    public void test2(){
        runner.removeAnalysisEngine(1);
        runner.addAnalysisEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Token"});
//        runner.run();
    }
}