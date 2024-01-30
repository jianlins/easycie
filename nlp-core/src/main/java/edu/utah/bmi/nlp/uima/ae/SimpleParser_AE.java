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

import edu.utah.bmi.nlp.core.SimpleParser;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.checkNameSpace;


/**
 * This is SimpleParser Wrapper into UIMA analyses engine.
 *
 * @author Jianlin Shi
 */
public class SimpleParser_AE extends JCasAnnotator_ImplBase {

	public static final String PARAM_SENTENCE_TYPE_NAME = "SentenceTypeName";

	public static final String PARAM_TOKEN_TYPE_NAME = "TokenTypeName";

	public static final String PARAM_INCLUDE_PUNCTUATION = "IncludePunctuation";

	protected Class<? extends Annotation> SentenceType, TokenType;
	protected boolean includePunctuation = false;
	protected static Constructor<? extends Annotation> SentenceTypeConstructor, TokenTypeConstructor;


	public void initialize(UimaContext cont) {


		String sentenceTypeName, tokenTypeName;
		Object obj = cont.getConfigParameterValue(PARAM_SENTENCE_TYPE_NAME);
		if (obj != null && obj instanceof String) {
			sentenceTypeName = ((String) obj).trim();
			sentenceTypeName = checkNameSpace(sentenceTypeName);
		} else {
			sentenceTypeName = checkNameSpace("Sentence");
		}
		obj = cont.getConfigParameterValue(PARAM_TOKEN_TYPE_NAME);
		if (obj != null && obj instanceof String) {
			tokenTypeName = ((String) obj).trim();
			tokenTypeName = checkNameSpace(tokenTypeName);
		} else {
			tokenTypeName = checkNameSpace("Token");
		}

		obj = cont.getConfigParameterValue(PARAM_INCLUDE_PUNCTUATION);
		if (obj != null && obj instanceof Boolean && (Boolean) obj)
			includePunctuation = true;

		try {
			SentenceType = AnnotationOper.getTypeClass(sentenceTypeName);
			TokenType = AnnotationOper.getTypeClass(tokenTypeName);
			SentenceTypeConstructor = SentenceType.getConstructor(JCas.class, int.class, int.class);
			TokenTypeConstructor = TokenType.getConstructor(JCas.class, int.class, int.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText();
		ArrayList<ArrayList<Span>> sentences = SimpleParser.tokenizeDecimalSmartWSentences(text, includePunctuation);
		for (ArrayList<Span> sentence : sentences) {
			int thisBegin = sentence.get(0).begin;
			int thisEnd = sentence.get(sentence.size() - 1).end;
			saveSentence(jcas, thisBegin, thisEnd);
			saveTokens(jcas, sentence);
		}
	}

	protected void saveSentence(JCas jcas, int begin, int end) {
		saveAnnotation(jcas, SentenceTypeConstructor, begin, end);

	}

	protected void saveTokens(JCas jcas, ArrayList<Span> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			Span thisSpan = tokens.get(i);
			saveToken(jcas, thisSpan.begin, thisSpan.end);
		}
	}


	protected void saveToken(JCas jcas, int begin, int end) {
		saveAnnotation(jcas, TokenTypeConstructor, begin, end);
	}


	protected void saveAnnotation(JCas jcas, Constructor<? extends Annotation> annoConstructor, int begin, int end) {
		Annotation anno = null;
		try {
			anno = annoConstructor.newInstance(jcas, begin, end);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		anno.addToIndexes();
	}

}