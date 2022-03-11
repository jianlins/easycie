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
package edu.utah.bmi.nlp.uima.common;

import org.apache.uima.cas.text.AnnotationFS;

import java.util.Comparator;

/**
 *
 * @author Jianlin Shi
 *
 */
public class AnnotationComparator implements Comparator<AnnotationFS> {
	public int compare(AnnotationFS o1, AnnotationFS o2) {
		if (o1.getBegin() > o2.getBegin()) {
			return 1;
		} else if (o1.getBegin() < o2.getBegin()) {
			return -1;
		}
		return 0;
	}
}
