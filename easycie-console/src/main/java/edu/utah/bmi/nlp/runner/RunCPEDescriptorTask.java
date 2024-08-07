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
import edu.utah.bmi.nlp.uima.loggers.UIMALogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    protected String loggerClassName="",compiledClassPath="classes";
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

    /**
     * Constructs a new RunCPEDescriptorTask with the given parameters.
     *
     * @param tasks              the TasksInf object that contains the list of tasks
     * @param compiledClassPath  the class path for compiled classes
     * @param loggerClassName    the fully qualified class name of the desired UIMALogger implementation
     *                           (can be an empty string to use the default NLPDBConsoleLogger)
     *
     */
    public RunCPEDescriptorTask(TasksInf tasks, String compiledClassPath, String loggerClassName) {
        this.tasks = tasks;
        this.loggerClassName = loggerClassName;
        this.compiledClassPath=compiledClassPath;
    }

    protected void initiate(TasksInf tasks, String loggerClassName, String compiledClassPath) {
        System.setProperty("uima.framework_impl", "org.apache.uima.impl.OpenUIMAFramework_impl");
        logger.fine("System property uima.framework_impl: " + System.getProperty("uima.framework_impl"));
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
        String tableName = config.getValue(ConfigKeys.logTableName, "LOG");
        String keyColumnName = config.getValue(ConfigKeys.keyColumnName, "RUN_ID");
        int maxCommentLength = Integer.parseInt(config.getValue(ConfigKeys.maxCommentLength, "-1").trim());
        String pipelineName = new File(cpeDescriptor).getName();
        pipelineName = pipelineName.substring(0, pipelineName.length() - 4);
        runner = AdaptableCPEDescriptorRunner.getInstance(cpeDescriptor, annotator,
                getUIMALogger(loggerClassName, writerDBConfigFileName, tableName, keyColumnName, annotator, maxCommentLength),
                componentsSettings, "desc/type/" + pipelineName + "_" + annotator + "_Type.xml", compiledClassPath);
        ((NLPDBConsoleLogger) runner.getLogger()).setReportable(report);
        updateReaderConfigurations(runner);
        updateWriterConfigurations(runner);
    }

    /**
     * Returns an instance of UIMALogger based on the provided parameters.
     *
     * @param loggerClassName     the fully qualified class name of the desired UIMALogger implementation
     *                            (can be an empty string to use the default NLPDBConsoleLogger)
     * @param writerDBConfigFileName the file name of database configuration for writer
     * @param tableName           the name of the table in the database to write the logs to
     * @param keyColumnName       the name of the column in the log table to be used as a key
     * @param annotator           the name of the annotator
     * @param maxCommentLength    the maximum length of the comments
     * @return a UIMALogger instance
     */
    public UIMALogger getUIMALogger(String loggerClassName, String writerDBConfigFileName, String tableName, String keyColumnName,
                                    String annotator, int maxCommentLength) {
        Class<? extends UIMALogger> uimaLoggerClass = NLPDBConsoleLogger.class;
        UIMALogger uimaLogger = new NLPDBConsoleLogger(writerDBConfigFileName, tableName, keyColumnName, annotator, maxCommentLength);
        try {
            if (!loggerClassName.trim().equals("")) {
                Constructor<? extends UIMALogger> constructor = uimaLoggerClass.getConstructor(String.class, String.class, String.class, String.class, Integer.class);
                uimaLogger = constructor.newInstance(writerDBConfigFileName, tableName, keyColumnName, annotator, maxCommentLength);
            }else{
                logger.fine("loggerClassName is empty, use default NLPDBConsoleLogger instead.");
            }
        } catch (NoSuchMethodException e) {
            logger.warning(e.getMessage());
        } catch (InvocationTargetException e) {
            logger.warning(e.getMessage());
        } catch (InstantiationException e) {
            logger.warning(e.getMessage());
        } catch (IllegalAccessException e) {
            logger.warning(e.getMessage());
        }
        return uimaLogger;
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
        initiate(tasks, loggerClassName, compiledClassPath);
        runner.run();
        return null;
    }


}
