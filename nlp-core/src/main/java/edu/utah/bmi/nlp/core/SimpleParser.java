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

import java.util.ArrayList;

/**
 * @author Jianlin on 6/10/15.
 */
public class SimpleParser {

    protected static final int whitespace = -1, punctuation = 0, dot = 1, returnc = 2, letter = 3, digit = 4;


    public static ArrayList<Span> tokenizeOnWhitespaces(String sentence) {
        return tokenizeOnWhitespaces(sentence, 0);
    }

    public static ArrayList<Span> tokenizeOnWhitespaces(String sentence, int offset) {
        ArrayList<Span> tokens = new ArrayList<>();
//        0: punctuation or return, 1: letter, 2: digit,
        int type = whitespace;
        int tokenBegin = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < sentence.length(); i++) {
            char thisChar = sentence.charAt(i);
            if (Character.isWhitespace(thisChar)) {
                if (type > whitespace) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                }
                type = whitespace;
            } else {
                if (type == whitespace) {
                    tokenBegin = i;
                    type = 1;
                }
                tmp.append(thisChar);
            }
        }
        if (type == 1) {
            tokens.add(new Span(tokenBegin + offset, sentence.length() + offset, sentence.substring(tokenBegin)));
        }
        return tokens;
    }

    public static ArrayList<ArrayList<String>> parse(String text, boolean includePunctuation) {
        ArrayList<ArrayList<String>> paragraphs = new ArrayList<ArrayList<String>>();
        ArrayList<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
//        0: punctuation, 1: letter, 2: digit, 3: return
        int type = whitespace;
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (WildCardChecker.isPunctuation(thisChar)) {
                if (type > 0)
                    tokens.add(sb.toString());
                if (includePunctuation)
                    tokens.add(Character.toString(thisChar));
                sb.setLength(0);
                type = 0;
            } else if (thisChar == '\n' || thisChar == '\r') {
                if (type != 3) {
                    paragraphs.add(tokens);
                    tokens = new ArrayList<String>();
                    type = 3;
                }
            } else if (Character.isDigit(thisChar)) {
                if (type == 0 || type == 3) {
                    sb.append(thisChar);
                    type = 2;
                } else if (type == 1) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                    sb.append(thisChar);
                    type = 2;
                } else {
                    sb.append(thisChar);
                }
            } else if (Character.isLetter(thisChar)) {
                if (type == 0 || type == 3) {
                    sb.append(thisChar);
                    type = 1;
                } else if (type == 1) {
                    sb.append(thisChar);
                } else {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                    sb.append(thisChar);
                    type = 1;
                }
            } else {
                if (type != 0 && type != 3)
                    tokens.add(sb.toString());
                sb.setLength(0);
                type = 0;
            }
        }
        if (type > 0)
            tokens.add(sb.toString());
        paragraphs.add(tokens);
        return paragraphs;
    }

    public static ArrayList<ArrayList<Span>> tokenizeWParagraphs(String text, boolean includePunctuation) {
        return tokenizeWParagraphs(text, includePunctuation, 0);
    }

    /**
     * Only consider more than 2 continuous line breakers as a separator of paragraphs, regardless of sentences.
     * Because sentence segmentation often needs a more refined segmenters than simply relying on any punctuation
     * or line break alone.
     * TODO not ready yet
     *
     * @param text               input string for tokenizing
     * @param includePunctuation whether tokenize punctuations
     * @param offset             the snippet (paragraph) offset to the beginning of the document
     * @return an arraylist
     */
    public static ArrayList<ArrayList<Span>> tokenizeWParagraphs(String text, boolean includePunctuation, int offset) {
        ArrayList<ArrayList<Span>> paragraphs = new ArrayList<ArrayList<Span>>();
        ArrayList<Span> tokens = new ArrayList<Span>();
//        0: punctuation, 1: letter, 2: digit, 3: return
        int type = 0;
        StringBuilder tmp = new StringBuilder();
        int tokenBegin = 0, tokenEnd = 0, sentenceBegin = 0, sentenceEnd = 0;
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (WildCardChecker.isPunctuation(thisChar)) {
                if (type > 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                }
                tokenBegin = i;
                if (includePunctuation)
                    tokens.add(new Span(tokenBegin, i + 1, String.valueOf(thisChar)));
                type = 0;
            } else if (thisChar == '\n' || thisChar == '\r') {
                if (type > 0 && type != 3) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                }
                if (tokens.size() > 0 && type == 3) {
                    paragraphs.add(tokens);
                    tokenBegin = i;
                    tokens = new ArrayList<Span>();
                }
                type = 3;
            } else if (Character.isDigit(thisChar)) {
                if (type == 0) {
                    tokenBegin = i;
                    type = 2;
                } else if (type == 1) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                    tokenBegin = i;
                    type = 2;
                }
                tmp.append(thisChar);
            } else if (Character.isLetter(thisChar)) {
                if (type == 0 || type == 3) {
                    tokenBegin = i;
                    type = 1;
                } else if (type == 2) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                    tokenBegin = i;
                    type = 1;
                }
                tmp.append(thisChar);
            } else {
                if (type != 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, tmp.toString()));
                    tmp.setLength(0);
                }
                type = 0;
            }
        }
        if (type == 1 || type == 2)
            tokens.add(new Span(tokenBegin, text.length()));
        paragraphs.add(tokens);
        return paragraphs;
    }


    /**
     * @param text               input text
     * @param includePunctuation whether include punctuation or not
     * @return a list of Span
     */
    public static ArrayList<Span> tokenize2Spans(String text, boolean includePunctuation) {
        return tokenize2Spans(text, includePunctuation, 0, false);
    }

    /**
     * @param text               input text string
     * @param includePunctuation whether include punctuation or not
     * @param caseSensitive      whether lower case all the characters when return the span's text--
     *                           it won't overwrite the original text.
     * @return a list of Span
     */
    public static ArrayList<Span> tokenize2Spans(String text, boolean includePunctuation, boolean caseSensitive) {
        return tokenize2Spans(text, includePunctuation, 0, caseSensitive);
    }

    /**
     * Tokenize to an ArrayList of Spans, regardless of sentences or paragraphs
     *
     * @param text               input string for tokenizing
     * @param includePunctuation whether tokenize punctuations
     * @param offset             the snippet (sentence) offset to the beginning of the document
     * @param caseSensitive      whether lower case all the characters when return the span's text--
     *                           it won't overwrite the original text.
     * @return a sentence
     */
    public static ArrayList<Span> tokenize2Spans(String text, boolean includePunctuation, int offset, boolean caseSensitive) {
        ArrayList<Span> tokens = new ArrayList<Span>();
//        0: punctuation or return, 1: letter, 2: digit,
        int type = 0;
        int tokenBegin = 0, tokenEnd = 0, sentenceBegin = 0, sentenceEnd = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (WildCardChecker.isPunctuation(thisChar)) {
                if (type > 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                }
                tokenBegin = i;
                if (includePunctuation)
                    tokens.add(new Span(tokenBegin, i + 1, String.valueOf(thisChar)));
                type = 0;
            } else if (thisChar == '\n' || thisChar == '\r') {
                if (type > 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                }
                tokenBegin = i;
                type = 0;
            } else if (Character.isDigit(thisChar)) {
                if (type == 0) {
                    tokenBegin = i;
                    type = 2;
                } else if (type == 1) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                    tokenBegin = i;
                    type = 2;
                }
                tmp.append(thisChar);
            } else if (Character.isLetter(thisChar)) {
                if (type == 0) {
                    tokenBegin = i;
                    type = 1;
                } else if (type == 2) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                    tokenBegin = i;
                    type = 1;
                }
                tmp.append(thisChar);
            } else {
                if (type != 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                }
                type = 0;
            }
        }
        if (type == 1 || type == 2) {
            tokens.add(new Span(tokenBegin, text.length(), text.substring(tokenBegin)));
        }
        return tokens;
    }

    /**
     * Tokenize to an ArrayList of Strings
     *
     * @param text               input string for tokenizing
     * @param includePunctuation whether tokenize punctuations
     * @return a sentence
     */
    public static ArrayList<String> tokenize(String text, boolean includePunctuation) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> output = new ArrayList<String>();
        int type = 0;
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (WildCardChecker.isPunctuation(thisChar)) {
                if (type > 0)
                    output.add(sb.toString());
                if (includePunctuation)
                    output.add(Character.toString(thisChar));
                sb.setLength(0);
                type = 0;
            } else if (Character.isDigit(thisChar)) {
                if (type == 0) {
                    sb.append(thisChar);
                    type = 2;
                } else if (type == 1) {
                    output.add(sb.toString());
                    sb.setLength(0);
                    sb.append(thisChar);
                    type = 2;
                } else {
                    sb.append(thisChar);
                }
            } else if (Character.isLetter(thisChar)) {
                if (type == 0) {
                    sb.append(thisChar);
                    type = 1;
                } else if (type == 1) {
                    sb.append(thisChar);
                } else {
                    output.add(sb.toString());
                    sb.setLength(0);
                    sb.append(thisChar);
                    type = 1;
                }
            } else {
                if (type > 0)
                    output.add(sb.toString());
                sb.setLength(0);
                type = 0;
            }
        }
        if (type > 0)
            output.add(sb.toString());
        return output;
    }

    /**
     * @param text               input text string
     * @param includePunctuation whether include punctuation or not
     * @return a list of Span
     */
    public static ArrayList<Span> tokenizeDecimalSmart(String text, boolean includePunctuation) {
        return tokenizeDecimalSmart(text, includePunctuation, 0, false);
    }

    /**
     * @param text               input text string
     * @param includePunctuation whether include punctuation or not
     * @param caseSensitive      whether lower case all the characters when return the span's text--
     *                           it won't overwrite the original text.
     * @return a list of Span
     */
    public static ArrayList<Span> tokenizeDecimalSmart(String text, boolean includePunctuation, boolean caseSensitive) {
        return tokenizeDecimalSmart(text, includePunctuation, 0, caseSensitive);
    }

    /**
     * TODO split include/exclude punctuation to two methods, reduce the times of if check
     * Tokenizer will consider float number as one token.
     *
     * @param text               input string for tokenizing
     * @param includePunctuation whether tokenize punctuations
     * @param offset             the snippet (sentence) offset to the beginning of the document
     * @param caseSensitive      whether lower case all the characters when return the span's text--
     *                           it won't overwrite the original text.
     * @return a sentence
     */
    public static ArrayList<Span> tokenizeDecimalSmart(String text, boolean includePunctuation, int offset,
                                                       boolean caseSensitive) {
        ArrayList<Span> tokens = new ArrayList<Span>();
//        0: punctuation or return, 1: letter, 2: digit, 3: dot
//        whitespace = -1, punctuation = 0, dot = 1, returnc = 2, letter = 3, digit = 4;
        int type_2 = whitespace, type_1 = whitespace, type0 = whitespace;
        int tokenBegin = 0, tokenEnd = 0, sentenceBegin = 0, sentenceEnd = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (thisChar == '.') {
                type0 = dot;
            } else if (WildCardChecker.isPunctuation(thisChar)) {
                type0 = punctuation;
            } else if (thisChar == '\n' || thisChar == '\r') {
                type0 = returnc;
            } else if (Character.isDigit(thisChar)) {
                type0 = digit;
            } else if (Character.isLetter(thisChar)) {
                type0 = letter;
            } else {
//                consider all other characters are whitespace, not included in tokens
                type0 = whitespace;
            }

            switch (type0) {
                case dot:
                    if (type_1 == whitespace) {
                        tokenBegin = i;
                    }
                    if (i == text.length() - 1) {
                        if (tmp.length() > 0) {
                            tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                            tmp.setLength(0);
                        }
                        if (includePunctuation)
                            tokens.add(new Span(i + offset, i + 1 + offset, "."));
                    }
                    break;
                case punctuation:
                    if ((type_1 == letter || type_1 == digit) && tmp.length() > 0) {
                        tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                        tmp.setLength(0);
                        tokenBegin = i;
                    }
                    if (includePunctuation) {
                        tmp.append(thisChar);
                        switch (type_1) {
                            case punctuation:
                                break;
                            case dot:
                                if (tmp.length() > 0)
                                    tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tokens.add(new Span(i - 1 + offset, i + offset, "."));
                                tmp.setLength(0);
                                tokenBegin = i;
                                break;
                            case whitespace:
                            case returnc:
                                tokenBegin = i;
                                break;
                        }
                    }
                    break;
                case whitespace:
                case returnc:
                    switch (type_1) {
                        case digit:
                        case letter:
                            tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                            tmp.setLength(0);
                            break;
                        case punctuation:
                            if (includePunctuation) {
                                tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tmp.setLength(0);
                            }
                            break;
                        case dot:
                            if (tmp.length() > 0) {
                                tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tmp.setLength(0);
                            }
                            if (includePunctuation)
                                tokens.add(new Span(i - 1 + offset, i + offset, "."));
                            break;
                    }
                    tokenBegin = i;
                    break;
                case digit:
                    switch (type_1) {
                        case whitespace:
                        case returnc:
                            tokenBegin = i;
                            break;
                        case punctuation:
                            if (includePunctuation) {
                                tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tmp.setLength(0);
                            }
                            tokenBegin = i;
                            break;
                        case dot:
                            if (type_2 == digit || type_2 == whitespace) {
                                tmp.append('.');
//                              char appended later outside switch
                                break;
                            } else {
                                if (tmp.length() > 0) {
                                    tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                    tmp.setLength(0);
                                }
                                if (includePunctuation) {
                                    tokens.add(new Span(i - 1 + offset, i + offset, "."));
                                }
                                tokenBegin = i;
                            }
                            break;
                        case letter:
                            tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                            tmp.setLength(0);
                            tokenBegin = i;
                            break;
                    }
                    tmp.append(thisChar);
                    break;
                case letter:
                    switch (type_1) {
                        case whitespace:
                        case returnc:
                            tokenBegin = i;
                            break;
                        case dot:
                            if (tmp.length() > 0) {
                                tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tmp.setLength(0);
                            }
                            if (includePunctuation)
                                tokens.add(new Span(i - 1 + offset, i + offset, "."));
                            tokenBegin = i;
                            break;
                        case punctuation:
                            if (includePunctuation) {
                                tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                                tmp.setLength(0);
                            }
                            tokenBegin = i;
                            break;
                        case digit:
                            tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                            tmp.setLength(0);
                            tokenBegin = i;
                            break;
                        case letter:
                            break;
                    }
                    tmp.append(thisChar);
                    break;
            }
            type_2 = type_1;
            type_1 = type0;
        }
        if (tmp.length() > 0)
            tokens.add(new Span(tokenBegin + offset, text.length() + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
        return tokens;
    }


    public static ArrayList<ArrayList<Span>> tokenizeDecimalSmartWSentences(String text, boolean includePunctuation) {
        return tokenizeDecimalSmartWSentences(text, includePunctuation, 0, false);
    }

    public static ArrayList<ArrayList<Span>> tokenizeDecimalSmartWSentences(String text, boolean includePunctuation, boolean caseSensitive) {
        return tokenizeDecimalSmartWSentences(text, includePunctuation, 0, caseSensitive);
    }

    /**
     * TODO this version only seg sentence on returns. Need to seg on punctuations as well. Handle short forms?
     * Tokenizer will consider float number as one token.
     *
     * @param text               input string for tokenizing
     * @param includePunctuation whether tokenize punctuations
     * @param offset             the snippet (paragraph) offset to the beginning of the document
     * @param caseSensitive      whether lower case all the characters when return the span's text--
     *                           it won't overwrite the original text.
     * @return a paragraph
     */
    public static ArrayList<ArrayList<Span>> tokenizeDecimalSmartWSentences(String text, boolean includePunctuation,
                                                                            int offset, boolean caseSensitive) {
        ArrayList<ArrayList<Span>> sentences = new ArrayList<ArrayList<Span>>();
        ArrayList<Span> tokens = new ArrayList<Span>();
//        0: punctuation , 1: letter, 2: digit, 3: dot, 4: return
//      the types of previous 2nd and previous 1st character to current character
        int type0 = whitespace, type1 = whitespace;
        int tokenBegin = 0, tokenEnd = 0, sentenceBegin = 0, sentenceEnd = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char thisChar = text.charAt(i);
            if (thisChar == '.') {
                type0 = type1;
                type1 = dot;
            } else if (WildCardChecker.isPunctuation(thisChar)) {
                if (type1 == letter || type1 == digit) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                } else if (type1 == punctuation && includePunctuation) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                } else if (type1 == dot) {
                    tokens.add(new Span(i - 2, i - 1, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    if (includePunctuation) {
                        tokens.add(new Span(i - 1, i, "."));
                    }
                }
                tmp.setLength(0);
                if (includePunctuation) {
                    tmp.append(thisChar);
                }
                tokenBegin = i;
                type0 = type1;
                type1 = punctuation;
            } else if (thisChar == '\n' || thisChar == '\r') {
                if (type1 == dot) {
                    tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    if (includePunctuation)
                        tokens.add(new Span(i - 1, i, "."));
                } else if (type1 != returnc) {
                    if (tmp.length() > 0) {
                        tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    }
                }
                if (tokens.size() > 0) {
                    sentences.add(tokens);
                    tokens = new ArrayList<Span>();
                }
                tokenBegin = i;
                tmp.setLength(0);
                type0 = type1;
                type1 = returnc;
            } else if (Character.isDigit(thisChar)) {
                if (type1 == punctuation) {
                    if (includePunctuation) {
                        tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                        tmp.setLength(0);
                    }
                } else if (type1 == letter) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                } else if (type1 == dot) {
//                  this is how smart dot works
                    if (type0 == digit) {
//                    find a float
                        tmp.append(".");
                    } else {
                        tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                        if (includePunctuation)
                            tokens.add(new Span(i - 1, i, "."));
                        tmp.setLength(0);
                    }
                }
                if (type1 != digit && !(type1 == dot && type0 == digit)) {
                    tokenBegin = i;
                }
                tmp.append(thisChar);
                type0 = type1;
                type1 = digit;
            } else if (Character.isLetter(thisChar)) {
                if (type1 == digit) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                }
                if (type1 == punctuation && tmp.length() > 0) {
                    if (includePunctuation) {
                        tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                        tmp.setLength(0);
                    }
                } else if (type1 == dot) {
                    tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                    if (includePunctuation)
                        tokens.add(new Span(i - 1, i, "."));
                }
                if (type1 != letter) {
                    tokenBegin = i;
                }
                tmp.append(thisChar);
                type0 = type1;
                type1 = letter;
            } else {
//              current char is whitespace or other unknown character-- exclude it in tokens
                if (type1 == dot) {
                    type0 = type1;
                    tokens.add(new Span(tokenBegin + offset, i - 1 + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                    if (includePunctuation)
                        tokens.add(new Span(i - 1, i, "."));
                } else if (type1 > 0 && type1 != returnc) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                } else if (includePunctuation && tmp.length() > 0) {
                    tokens.add(new Span(tokenBegin + offset, i + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));
                    tmp.setLength(0);
                    type0 = type1;
                }
                type1 = whitespace;
            }
        }
        if (type1 == digit) {
            tokens.add(new Span(tokenBegin, text.length(), text.substring(tokenBegin)));
        } else if (type1 == punctuation || type1 == dot) {
            tokenEnd = text.length() - 1;
            tokens.add(new Span(tokenBegin, tokenEnd, text.substring(tokenBegin, tokenEnd)));
            if (includePunctuation) {
                tokens.add(new Span(tokenEnd, text.length(), text.substring(tokenEnd)));
            }
        } else if (tmp.length() > 0)
            tokens.add(new Span(tokenBegin + offset, text.length() + offset, caseSensitive ? tmp.toString() : tmp.toString().toLowerCase()));

        if (tokens.size() > 0)
            sentences.add(tokens);
        return sentences;
    }

}
