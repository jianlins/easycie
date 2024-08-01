package edu.utah.bmi.nlp.runner;


import edu.utah.bmi.nlp.easycie.core.ConfigKeys;
import edu.utah.bmi.nlp.core.TaskInf;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.uima.AdaptableCPEDescriptorStringDebugger;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Created by Jianlin Shi on 9/19/16.
 */
public class RunCPEDescriptorDebugTask  {
    public static Logger logger = Logger.getLogger(RunCPEDescriptorDebugTask.class.getCanonicalName());
    protected String inputTableName, snippetResultTable, docResultTable, bunchResultTable, annotator, datasetId;
    public boolean report = false;
    public AdaptableCPEDescriptorStringDebugger runner;
    protected LinkedHashMap<String, String> componentsSettings;
    protected LinkedHashMap<String, String> loggerSettings;
    private String cpeDescriptor;
    private TasksInf tasks;
    private String inputStr, metaStr;
    private String pipelineName;

    protected RunCPEDescriptorDebugTask() {

    }


    public RunCPEDescriptorDebugTask(TasksInf tasks) {
        this.tasks = tasks;
    }


    protected void initiate(TasksInf tasks) {
        TaskInf config = tasks.getTask(ConfigKeys.maintask);
        annotator = config.getValue(ConfigKeys.annotator);
        String rawStringValue = config.getValue(ConfigKeys.reportAfterProcessing);
        report = rawStringValue.length() > 0 && (rawStringValue.charAt(0) == 't' || rawStringValue.charAt(0) == 'T' || rawStringValue.charAt(0) == '1');

        config = tasks.getTask("debug");
        metaStr = config.getValue("log/metaStr");
        runner = AdaptableCPEDescriptorStringDebugger.getInstance(tasks);
    }


    protected Object call(String inputStr) throws Exception {
        logger.info("Initiating pipeline...");
        initiate(tasks);
        runner.process(inputStr, metaStr);
        return null;
    }

}
