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

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.XMLInputSource;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Jianlin Shi on 2/25/16.
 */

/**
 * Main Class that runs a Collection Processing Engine (CPE). This class reads a CPE Descriptor as a
 * command-line argument and instantiates the CPE. It also registers a callback listener with the
 * CPE, which will print progress and statistics to System.out.
 */
public class SimpleRunCPEReporter extends Thread {
    /**
     * The CPE instance.
     */
    private final CollectionProcessingEngine mCPE;

    /**
     * Start time of CPE initialization
     */
    private final long mStartTime;

    /**
     * Start time of the processing
     */
    private long mInitCompleteTime;

    private final String logFile;


    public SimpleRunCPEReporter(String[] args) throws Exception {
        mStartTime = System.currentTimeMillis();

        // check command line args
        if (args.length < 2) {
            printUsageMessage();
            System.exit(1);

        }
        logFile = args[1];

        // parse CPE descriptor
        System.out.println("Parsing CPE Descriptor");
        CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
                new XMLInputSource(args[0]));
        // instantiate CPE
        System.out.println("Instantiating CPE");
        mCPE = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);

        // Create and register a Status Callback Listener
        mCPE.addStatusCallbackListener(new StatusCallbackListenerImpl());

        // Start Processing
        System.out.println("Running CPE");
        mCPE.process();

        // Allow user to abort by pressing Enter
        System.out.println("To abort processing, type \"abort\" and press enter.");
        while (true) {
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if ("abort".equals(line) && mCPE.isProcessing()) {
                System.out.println("Aborting...");
                mCPE.stop();
                break;
            }
        }
    }


    private static void printUsageMessage() {
        System.out.println(" Arguments to the program are as follows : \n"
                + "args[0] : path to CPE descriptor file");
    }


    public static void main(String[] args) throws Exception {
        new SimpleRunCPEReporter(args);
    }

    // Callback Listener. Receives event notifications from CPE.

    class StatusCallbackListenerImpl implements StatusCallbackListener {
        int entityCount = 0;

        long size = 0;


        public void initializationComplete() {
            System.out.println("CPM Initialization Complete");
            mInitCompleteTime = System.currentTimeMillis();
        }


        public void batchProcessComplete() {
            System.out.print("Completed " + entityCount + " documents");
            if (size > 0) {
                System.out.print("; " + size + " characters");
            }
            System.out.println();
            long elapsedTime = System.currentTimeMillis() - mStartTime;
            System.out.println("Time Elapsed : " + elapsedTime + " ms ");
        }


        public void collectionProcessComplete() {
            long time = System.currentTimeMillis();
            System.out.print("Completed " + entityCount + " documents");
            if (size > 0) {
                System.out.print("; " + size + " characters");
            }
            System.out.println();
            long initTime = mInitCompleteTime - mStartTime;
            long processingTime = time - mInitCompleteTime;
            long elapsedTime = initTime + processingTime;
            System.out.println("Total Time Elapsed: " + elapsedTime + " ms ");
            System.out.println("Initialization Time: " + initTime + " ms");
            System.out.println("Processing Time: " + processingTime + " ms");
            String reportContent = "";
            reportContent += "Total Time\t" + elapsedTime + "\t";
            reportContent += "Initialization Time\t" + initTime + "\t";
            reportContent += "Processing Time\t" + processingTime + "\t";
            for (ProcessTraceEvent event : mCPE.getPerformanceReport().getEvents()) {
                reportContent += event.getComponentName() + "-" + event.getType() + "\t" + event.getDuration() + "\t";
            }
            try {
                IOUtils.write(reportContent, new FileWriter(logFile));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("\n\n ------------------ PERFORMANCE REPORT ------------------\n");
            System.out.println(mCPE.getPerformanceReport().toString());
            // stop the JVM. Otherwise main thread will still be blocked waiting for
            // user to press Enter.
            System.exit(1);

        }


        public void paused() {
            System.out.println("Paused");
        }


        public void resumed() {
            System.out.println("Resumed");
        }


        public void aborted() {
            System.out.println("Aborted");
            // stop the JVM. Otherwise main thread will still be blocked waiting for
            // user to press Enter.
            System.exit(1);
        }

        /**
         * Called when the processing of a Document is completed. <br>
         * The process status can be looked at and corresponding actions taken.
         *
         * @param aCas    CAS corresponding to the completed processing
         * @param aStatus EntityProcessStatus that holds the status of all the events for aEntity
         */
        public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
            if (aStatus.isException()) {
                List exceptions = aStatus.getExceptions();
                for (int i = 0; i < exceptions.size(); i++) {
                    ((Throwable) exceptions.get(i)).printStackTrace();
                }
                return;
            }
            entityCount++;
            String docText = aCas.getDocumentText();
            if (docText != null) {
                size += docText.length();
            }
        }
    }

}
