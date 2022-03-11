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

package edu.utah.bmi.nlp.uima;

import edu.utah.bmi.nlp.uima.loggers.UIMALogger;
import org.apache.uima.Constants;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CpePipeline {
    private UIMALogger logger;
    private long mInitCompleteTime;
    private CollectionProcessingEngine engine;
    public int maxCommentLength = 500;


    public CpePipeline(UIMALogger logger) {
        this.logger = logger;
    }

    public void runPipeline(CollectionReaderDescription readerDesc, List<AnalysisEngineDescription> descs) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        AnalysisEngineDescription aaeDesc = createEngineDescription(descs);
        this.logger = logger;
        CpeBuilder builder = new CpeBuilder();
        builder.setReader(readerDesc);
        builder.setAnalysisEngine(aaeDesc);
        builder.setMaxProcessingUnitThreadCount(Runtime.getRuntime().availableProcessors() - 1);
        SimpleStatusCallbackListenerImpl status = new SimpleStatusCallbackListenerImpl(logger);
        builder.setMaxProcessingUnitThreadCount(0);
        engine = builder.createCpe(status);
        status.setCollectionProcessingEngine(engine);
        engine.process();
        try {
            synchronized (status) {
                while (status.isProcessing) {
                    status.wait();
                }
                System.out.println("Pipeline complete");
            }
        } catch (InterruptedException var9) {
            var9.printStackTrace();
        }

        if (status.exceptions.size() > 0) {
            throw new AnalysisEngineProcessException(status.exceptions.get(0));
        }
    }


    public AnalysisEngineDescription createEngineDescription(
            List<AnalysisEngineDescription> analysisEngineDescriptions)
            throws ResourceInitializationException {

        // create the descriptor and set configuration parameters
        AnalysisEngineDescription desc = new AnalysisEngineDescription_impl();
        desc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
        desc.setPrimitive(false);

        // if any of the aggregated analysis engines does not allow multiple
        // deployment, then the
        // aggregate engine may also not be multiply deployed
        boolean allowMultipleDeploy = true;
        for (AnalysisEngineDescription d : analysisEngineDescriptions) {
            allowMultipleDeploy &= d.getAnalysisEngineMetaData().getOperationalProperties()
                    .isMultipleDeploymentAllowed();
        }
        desc.getAnalysisEngineMetaData().getOperationalProperties()
                .setMultipleDeploymentAllowed(allowMultipleDeploy);

        List<String> flowNames = new ArrayList<String>();

        for (int i = 0; i < analysisEngineDescriptions.size(); i++) {
            AnalysisEngineDescription aed = analysisEngineDescriptions.get(i);
            String componentName = aed.getImplementationName() + "-" + i;
            desc.getDelegateAnalysisEngineSpecifiersWithImports().put(componentName, aed);
            flowNames.add(componentName);
        }


        FixedFlow fixedFlow = new FixedFlow_impl();
        fixedFlow.setFixedFlow(flowNames.toArray(new String[flowNames.size()]));
        desc.getAnalysisEngineMetaData().setFlowConstraints(fixedFlow);

//        if (typePriorities != null) {
//            desc.getAnalysisEngineMetaData().setTypePriorities(typePriorities);
//        }
//
//        if (sofaMappings != null) {
//            desc.setSofaMappings(sofaMappings);
//        }

        return desc;
    }
}
