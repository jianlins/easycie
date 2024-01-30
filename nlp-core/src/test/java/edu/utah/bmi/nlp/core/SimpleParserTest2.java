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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by Jianlin Shi on 4/20/16.
 */
public class SimpleParserTest2 {
    private final boolean printout = true;
    private final ArrayList<String> testCases = new ArrayList<String>();

    @BeforeEach
    public void initiateTestCases() {


//        testCases.add("At 10:30am examine.\n\nThe reports are done due to 20.5% \n\nH1N1 overload.");
//        testCases.add("-Encephalopathy:  Lactulose titrated to 4 BM per day33.3");
        testCases.add("0.5% \n\nH1N1 overload");
//        testCases.add("At 8.5 kg.");
//        testCases.add("1. Test this case: green trees! 7.OK,this is right.3. Are you serious?");
//        testCases.add("air .\n\n" +
//                "She was sent to New England Sinai Hospital & Rehab Center");
//        testCases.add(" •  Coagulopathy (HCC)    \n" +
//                "\n" +
//                "\n" +
//                "\n" +
//                " •  Hepatic encephalopathy (HCC)    \n");
    }


    @Test
    public void testParse() throws Exception {

    }

    @Test
    // TODO: 5/3/16  need to test paragraphs
    public void testParseSpan() throws Exception {
        String input = testCases.get(0);
        if (printout)
            System.out.println("Include punctuations");
        ArrayList<ArrayList<Span>> paragraphs = SimpleParser.tokenizeWParagraphs(input, true);
        assertEquals(2, paragraphs.size(), "Didn't get correct number of paragraphs. ");
        printParagraphs(input, paragraphs);
        if (printout)
            System.out.println("Exclude punctuations");
        paragraphs = SimpleParser.tokenizeWParagraphs(input, false);
        assertEquals(2, paragraphs.size(), "Didn't get correct number of paragraphs. ");
        printParagraphs(input, paragraphs);
    }

    @Test
    public void testTokenize2Spans() throws Exception {
        for (String input : testCases) {
            if (printout)
                System.out.println("Include punctuations");
            ArrayList<Span> spans = SimpleParser.tokenize2Spans(input, true,true);
            for (Span token : spans) {
                if (printout)
                    System.out.println(token.text + "\t" + token.begin + " - " + token.end + "\t" + input.substring(token.begin, token.end));
                assert (token.text.equals(input.substring(token.begin, token.end)));
            }
            if (printout)
                System.out.println("Exclude punctuations");
            spans = SimpleParser.tokenize2Spans(input, false);
            for (Span token : spans) {
                if (printout)
                    System.out.println(token.text + "\t" + token.begin + " - " + token.end + "\t" + input.substring(token.begin, token.end));
                assert (token.text.equals(input.substring(token.begin, token.end).toLowerCase()));
            }
        }
    }

    @Test
    public void testTokenize() throws Exception {
        for (String input : testCases) {
            if (printout)
                System.out.println("Include punctuations");
            ArrayList<Span> spans = SimpleParser.tokenize2Spans(input, true);
            ArrayList<String> tokens = SimpleParser.tokenize(input, true);
            assert (spans.size() == tokens.size());
            for (int i = 0; i < spans.size(); i++) {
                Span token = spans.get(i);
                assert (input.substring(token.begin, token.end).equals(tokens.get(i)));
            }
            if (printout)
                System.out.println("Exclude punctuations");
            spans = SimpleParser.tokenize2Spans(input, false);
            tokens = SimpleParser.tokenize(input, false);
            assert (spans.size() == tokens.size());
            for (int i = 0; i < spans.size(); i++) {
                Span token = spans.get(i);
                assert (input.substring(token.begin, token.end).equals(tokens.get(i)));
            }
        }

    }

    @Test
    public void testTokenizeDecimalSmart() throws Exception {
        for (String input : testCases) {
            if (printout)
                System.out.println("Include punctuations");
            ArrayList<Span> spans = SimpleParser.tokenizeDecimalSmart(input, true);
            printSpan(input,spans);
            if (printout)
                System.out.println("Exclude punctuations");
            spans = SimpleParser.tokenizeDecimalSmart(input, false);
            printSpan(input,spans);
        }
    }

    @Test
    public void test5() {
        String input ="4 . End-stage chronic 6 obstructive pulmonary disease.";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmart(input,true);
        printSpan(input,tokens);
    }

    @Test
    public void test6() {
        String input ="TO .70 DURING ";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmart(input,true);
        printSpan(input,tokens);
    }


   @Test
   public void test7(){
       String input = "hearing changes.";
       ArrayList<ArrayList<Span>> tokens = SimpleParser.tokenizeDecimalSmartWSentences(input,true);
       printSpan(input,tokens.get(0));
   }
    @Test
    public void testTokenizeDecimalSmartWParagraphs() throws Exception {
        for (String input : testCases) {
            if (printout)
                System.out.println("Include punctuations");
            ArrayList<ArrayList<Span>> spans = SimpleParser.tokenizeDecimalSmartWSentences(input, true);
            printParagraphs(input, spans);
//            if (printout)
//                System.out.println("Exclude punctuations");
//            spans = SimpleParser.tokenizeDecimalSmartWSentences(input, false);
//            printParagraphs(input, spans);
        }
    }


    private void printParagraphs(String input, ArrayList<ArrayList<Span>> paragraphs) {
        for (ArrayList<Span> paragraph : paragraphs) {
            printSpan(input, paragraph);
            if (printout)
                System.out.println("\n\t*********End of paragraph*********\n");
        }
    }

    private void printSpan(String input, ArrayList<Span> spans) {
        for (Span span : spans) {
            if (printout)
                System.out.print(span.text + "\t" + span.begin + " - " + span.end + "\t");
            System.out.println(">"+input.substring(span.begin, span.end)+"<");

//            assert (token.text.equals(input.substring(token.begin, token.end)));
        }
    }


}