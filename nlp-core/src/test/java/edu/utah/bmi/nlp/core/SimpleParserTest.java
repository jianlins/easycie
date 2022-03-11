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

package edu.utah.bmi.nlp.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * Created by
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class SimpleParserTest {
    String text;

    @BeforeEach
    public void init() {
        text = " The patient was admitted on 03/26/08\n and was started on IV antibiotics elevation" +
                ", was also counseled to minimizing the cigarette smoking. The patient had edema\n\n" +
                "\n of his bilateral lower extremities. The hospital consult was also obtained to " +
                "address edema issue question was related to his liver hepatitis C. Hospital consult" +
                " was obtained. This included an ultrasound of his abdomen, which showed just mild " +
                "cirrhosis. ";
    }

    @Test
    public void tokenizeDecimalSmartWSentences1() throws Exception {
        ArrayList<ArrayList<Span>> sentences = SimpleParser.tokenizeDecimalSmartWSentences(text, false);
        for (ArrayList<Span> sentence : sentences) {
            System.out.println(text.substring(sentence.get(0).begin, sentence.get(sentence.size() - 1).end));
            System.out.print("\"");
            for (Span token : sentence) {
                System.out.print(token.text + " ");
            }
            System.out.println("\"");
        }
    }

    @Test
    public void tokenizeDecimalSmartWSentences2() throws Exception {
        ArrayList<ArrayList<Span>> sentences = SimpleParser.tokenizeDecimalSmartWSentences(text, true);
        for (ArrayList<Span> sentence : sentences) {
            System.out.println(text.substring(sentence.get(0).begin, sentence.get(sentence.size() - 1).end));
            System.out.print("\"");
            for (Span token : sentence) {
                System.out.print(token.text + " ");
            }
            System.out.println("\"");
        }
    }


}