/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.collection.impl;

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.cpm.BaseCPMImpl;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.ProcessControllerAdapter;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;

import java.util.Map;
import java.util.Properties;


public class OpenCollectionProcessingEngine_impl extends CollectionProcessingEngine1_impl {
    /**
     * CPM instance that handles the processing
     */


    public void setReader(BaseCollectionReader reader) {
        this.mCPM.setCollectionReader(reader);

    }


    public void removeStatusCallbackListener() {
        mCPM.removeStatusCallbackListener();
    }


    public BaseCPMImpl getCPM() {
        return mCPM;
    }


}
