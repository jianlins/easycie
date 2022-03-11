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

package edu.utah.bmi.nlp.uima.reader;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.sql.RecordRow;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.IOException;

/**
 * @author Jianlin Shi
 * Created on 7/13/16.
 */

/**
 * This is a simple String ArrayList reader to convert a String ArrayList to UIMA Jcas.
 * It is convenient class for testing.
 */
public class StringMetaReader extends CollectionReader_ImplBase {
    /**
     * Name of configuration parameter that must be set to the path of a directory containing input
     * files.
     */
    public static final String PARAM_INPUT = "InputString";
    @ConfigurationParameter(name = PARAM_INPUT, mandatory = false, defaultValue = "", description = "A string as the input to be processed")
    protected String input;

    public static final String PARAM_META = "Meta";
    @ConfigurationParameter(name = PARAM_META, mandatory = false, defaultValue = "", description = "A string that stores the meta data columns, " +
            "where columns is separated by a pipe character(|); within each column," +
            " the column name and value are separated by comma(,).")
    protected String meta;

    public static final String PARAM_LANGUAGE = "Language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false, defaultValue = "en", description = "Name of optional configuration parameter that contains the language of the input string.")
    protected String mLanguage;


    protected int mCurrentIndex;


    public void initialize() throws ResourceInitializationException {
        super.initialize();
        ConfigurationParameterInitializer.initialize(this, getUimaContext());
        ExternalResourceInitializer.initialize(this, getUimaContext());
//        input = (String) getConfigParameterValue(PARAM_INPUT);
//        meta = (String) getConfigParameterValue(PARAM_META);
//        mLanguage = (String) getConfigParameterValue(PARAM_LANGUAGE);
        mCurrentIndex = 0;
    }


    public boolean hasNext() {
        return mCurrentIndex < 1;
    }


    public void getNext(CAS aCAS) throws IOException, CollectionException {
        RecordRow recordRow = new RecordRow();
        if (meta != null && meta.length()>0)
            for (String metaInfor : meta.split("\\|")) {
                String[] pair = metaInfor.split(",");
                recordRow.addCell(pair[0], pair[1]);
            }
        String metaInfor = recordRow.serialize();
        JCas jcas=null;
        try {
            jcas = aCAS.getJCas();
        } catch (CASException var6) {
            System.out.println(var6.toString());
        }
        jcas.setDocumentText(input);
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jcas, 0, input.length());
        srcDocInfo.setUri(metaInfor);
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize(input.length());
        srcDocInfo.setLastSegment(true);
        srcDocInfo.addToIndexes();

        // set language if it was explicitly specified as a configuration parameter
        if (mLanguage != null) {
            jcas.setDocumentLanguage(mLanguage);
        }
        mCurrentIndex++;

    }


    public void close() throws IOException {
    }

    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(mCurrentIndex, 1, Progress.ENTITIES)};
    }


    public int getNumberOfDocuments() {
        return 1;
    }

}
