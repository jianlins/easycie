package edu.utah.bmi.nlp.runner;


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.easycie.core.ConfigKeys;
import edu.utah.bmi.nlp.core.SettingAb;
import edu.utah.bmi.nlp.core.TaskInf;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.easycie.reader.SQLTextReader;
import edu.utah.bmi.nlp.easycie.writer.SQLWriterCasConsumer;
import edu.utah.bmi.nlp.uima.AdaptableCPEDescriptorRunner;
import edu.utah.bmi.nlp.uima.BunchMixInferenceWriter;
import edu.utah.bmi.nlp.uima.loggers.NLPDBConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class represents a task that runs a CPE (Collection Processing Engine) Descriptor.
 * It initiates the necessary configurations and runs the CPE.
 */
public class RunCPEDescriptorTask {
    public static Logger logger = Logger.getLogger(RunCPEDescriptorTask.class.getCanonicalName());
    protected String readerDBConfigFileName, writerDBConfigFileName, inputTableName, snippetResultTable, docResultTable, bunchResultTable,
            ehostDir, bratDir, xmiDir, annotator, datasetId;
    public boolean report = false;
    public AdaptableCPEDescriptorRunner runner;
    protected LinkedHashMap<String, String> componentsSettings;
    private String cpeDescriptor;
    protected TasksInf tasks;


    public RunCPEDescriptorTask() {

    }

    public RunCPEDescriptorTask(TasksInf tasks) {
        this.tasks = tasks;
    }

    public RunCPEDescriptorTask(TasksInf tasks, String paras) {
        this.tasks = tasks;
    }

    protected void initiate(TasksInf tasks, String option) {
        System.setProperty("uima.framework_impl","org.apache.uima.impl.OpenUIMAFramework_impl");
        System.out.println(System.getProperty("uima.framework_impl"));
        if (System.getProperty("java.util.logging.config.file") == null &&
                new File("logging.properties").exists()) {
            System.setProperty("java.util.logging.config.file", "logging.properties");
        }
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            {
                {
                    System.setProperty("java.util.logging.config.file", "logging.properties");
                }
                System.setProperty("java.util.logging.config.file", "logging.properties");
            }
            e.printStackTrace();
        }
        logger.info("Initiate configurations..");
        TaskInf config = tasks.getTask(ConfigKeys.maintask);
        cpeDescriptor = config.getValue("pipeLineSetting/CpeDescriptor");
        componentsSettings = readPipelineConfigurations(config.getChildSettings("pipeLineSetting"));

        annotator = config.getValue(ConfigKeys.annotator);
        String rawStringValue = config.getValue(ConfigKeys.reportAfterProcessing);
        report = rawStringValue.length() > 0 && (rawStringValue.charAt(0) == 't' || rawStringValue.charAt(0) == 'T' || rawStringValue.charAt(0) == '1');


        config = tasks.getTask("settings");
        readerDBConfigFileName = config.getValue(ConfigKeys.readDBConfigFileName);
        inputTableName = config.getValue(ConfigKeys.inputTableName);
        datasetId = config.getValue(ConfigKeys.datasetId);
        writerDBConfigFileName = config.getValue(ConfigKeys.writeDBConfigFileName);
        snippetResultTable = config.getValue(ConfigKeys.snippetResultTableName);
        docResultTable = config.getValue(ConfigKeys.docResultTableName);
        bunchResultTable = config.getValue(ConfigKeys.bunchResultTableName);
//      allow log table to be configurable from project configuration file.
        String tableName=config.getValue(ConfigKeys.logTableName,"LOG");
        String keyColumnName=config.getValue(ConfigKeys.keyColumnName,"RUN_ID");
        int maxCommentLength=Integer.parseInt(config.getValue(ConfigKeys.maxCommentLength, "-1").trim());
        String pipelineName = new File(cpeDescriptor).getName();
        pipelineName = pipelineName.substring(0, pipelineName.length() - 4);

        runner = AdaptableCPEDescriptorRunner.getInstance(cpeDescriptor, annotator,
                new NLPDBConsoleLogger(writerDBConfigFileName, tableName, keyColumnName, annotator, maxCommentLength),
                componentsSettings, "desc/type/" + pipelineName + "_" + annotator + "_Type.xml", "classes");
        ((NLPDBConsoleLogger) runner.getLogger()).setReportable(report);
        updateReaderConfigurations(runner);
        updateWriterConfigurations(runner);
    }

    protected void updateReaderConfigurations(AdaptableCPEDescriptorRunner runner) {
        runner.updateReadDescriptorsConfiguration(DeterminantValueSet.PARAM_DB_CONFIG_FILE, readerDBConfigFileName);
        runner.updateReadDescriptorsConfiguration(DeterminantValueSet.PARAM_ANNOTATOR, annotator);
        runner.updateReadDescriptorsConfiguration(SQLTextReader.PARAM_DATASET_ID, datasetId);
        runner.updateReadDescriptorsConfiguration(SQLTextReader.PARAM_DOC_TABLE_NAME, inputTableName);
    }


    protected void updateWriterConfigurations(AdaptableCPEDescriptorRunner runner) {
        for (int writerId : runner.getWriterIds().values()) {
            runner.updateDescriptorConfiguration(writerId, DeterminantValueSet.PARAM_DB_CONFIG_FILE, writerDBConfigFileName);
            runner.updateDescriptorConfiguration(writerId, DeterminantValueSet.PARAM_VERSION, runner.getLogger().getRunid() + "");
            runner.updateDescriptorConfiguration(writerId, DeterminantValueSet.PARAM_ANNOTATOR, annotator);
            runner.updateDescriptorConfiguration(writerId, SQLWriterCasConsumer.PARAM_SNIPPET_TABLENAME, snippetResultTable);
            runner.updateDescriptorConfiguration(writerId, SQLWriterCasConsumer.PARAM_DOC_TABLENAME, docResultTable);
            runner.updateDescriptorConfiguration(writerId, BunchMixInferenceWriter.PARAM_TABLENAME, bunchResultTable);
        }
//        }
    }

    protected LinkedHashMap<String, String> readPipelineConfigurations(LinkedHashMap<String, SettingAb> pipelineSettings) {
        LinkedHashMap<String, String> componentsSettings = new LinkedHashMap<>();
        for (SettingAb setting : pipelineSettings.values()) {
            String[] componentConfigure = setting.getSettingName().split("/");
            if (componentConfigure.length < 3)
                continue;
            String key = componentConfigure[1] + "/" + componentConfigure[2];
            String value = setting.getSettingValue();
            componentsSettings.put(key, value);
        }
        return componentsSettings;
    }


    protected Object call() throws Exception {
        initiate(tasks, "db");
        runner.run();
        return null;
    }


}
