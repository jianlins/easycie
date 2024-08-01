package edu.utah.bmi.nlp.easycie.tasks;


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.easycie.core.ConfigKeys;
import edu.utah.bmi.nlp.easycie.entry.EasyCIETask;
import edu.utah.bmi.nlp.core.SettingAb;
import edu.utah.bmi.nlp.core.TaskInf;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.easycie.reader.SQLTextReader;
import edu.utah.bmi.nlp.easycie.writer.SQLWriterCasConsumer;
import edu.utah.bmi.nlp.uima.AdaptableCPEDescriptorRunner;
import edu.utah.bmi.nlp.uima.BunchMixInferenceWriter;
import edu.utah.bmi.nlp.uima.loggers.NLPDBConsoleLogger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by Jianlin Shi on 9/19/16.
 */
public class RunCPEDescriptorTask extends EasyCIETask {
    public static Logger logger = Logger.getLogger(RunCPEDescriptorTask.class.getCanonicalName());
    protected String readerDBConfigFileName, writerDBConfigFileName, inputTableName, snippetResultTable, docResultTable, bunchResultTable,
            ehostDir, bratDir, xmiDir, annotator, datasetId;
    public boolean report = false;
    public AdaptableCPEDescriptorRunner runner;
    protected LinkedHashMap<String, String> componentsSettings;
    private String cpeDescriptor;
    protected TasksInf tasks;
    private File appDir;


    public RunCPEDescriptorTask() {

    }

    public RunCPEDescriptorTask(TasksInf tasks) {
        this.tasks = tasks;
    }

    public RunCPEDescriptorTask(TasksInf tasks, String paras) {
        this.tasks = tasks;
    }

    protected void initiate(TasksInf tasks, String option) {
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

        appDir = new File("./");
        if (tasks.getTask("projectDir") != null)
            appDir = new File(tasks.getTask("projectDir").getValue("projectDir"));

        logger.fine("AppDir: " + appDir.getAbsolutePath());

        updateGUIMessage("Initiate configurations..");
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
        String pipelineName = new File(cpeDescriptor).getName();
        pipelineName = pipelineName.substring(0, pipelineName.length() - 4);

        readerDBConfigFileName = adjustConfigPath(appDir, readerDBConfigFileName);
        writerDBConfigFileName = adjustConfigPath(appDir, writerDBConfigFileName);
//        updateReaderDBConfig(componentsSettings, readerDBConfigFileName, cpeDescriptor);
//        updateWriterDBConfig(componentsSettings, writerDBConfigFileName);
        runner = AdaptableCPEDescriptorRunner.getInstance(cpeDescriptor, annotator, new NLPDBConsoleLogger(writerDBConfigFileName, annotator),
                componentsSettings, "desc/type/" + pipelineName + "_" + annotator + "_Type.xml", "classes");
        ((NLPDBConsoleLogger) runner.getLogger()).setReportable(report);
        ((NLPDBConsoleLogger) runner.getLogger()).setTask(this);
        updateReaderConfigurations(runner);
        updateWriterConfigurations(runner);
    }

    private void updateReaderDBConfig(LinkedHashMap<String, String> componentsSettings, String readerDBConfigFileName, String cpeDescriptor) {
        try {
            CpeDescription currentCpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(new XMLInputSource(cpeDescriptor));
            CpeCollectionReader[] collRdrs = currentCpeDesc.getAllCollectionCollectionReaders();
            File cpeRootFolder = new File(currentCpeDesc.getSourceUrl().getFile()).getParentFile();
            for (CpeCollectionReader collReader : collRdrs) {
                CpeComponentDescriptor desc = collReader.getDescriptor();
                File descFile = new File(cpeRootFolder, collReader.getDescriptor().getImport().getLocation());
                CollectionReaderDescription crd = UIMAFramework.getXMLParser().parseCollectionReaderDescription(new XMLInputSource(descFile));
                String readerName = crd.getMetaData().getName();
                if (readerName == null || readerName.equals("")) {
                    readerName = crd.getImplementationName();
                    readerName = readerName.substring(readerName.lastIndexOf(".") + 1);
                }
                componentsSettings.put(readerName+"/"+DeterminantValueSet.PARAM_DB_CONFIG_FILE, readerDBConfigFileName);
            }
        } catch (InvalidXMLException | IOException e) {
            e.printStackTrace();
        } catch (CpeDescriptorException e) {
            e.printStackTrace();
        }
    }

    private void updateWriterDBConfig(LinkedHashMap<String, String> componentsSettings, String writerDBConfigFileName) {
        for (String key : componentsSettings.keySet()) {
            String lowered = key.toLowerCase(Locale.ROOT);
            if (lowered.contains("writer") && lowered.endsWith("dbconfigfile")) {
                componentsSettings.put(key, writerDBConfigFileName);
            }
        }
    }

    private String adjustConfigPath(File appDir, String configFilePath) {
        if (new File(configFilePath).exists()) {
            return configFilePath;
        } else if (new File(appDir, configFilePath).exists()) {
            return new File(appDir, configFilePath).getAbsolutePath();
        } else {
            try {
                throw new FileNotFoundException("configFilePath cannot be found either in: " + new File(configFilePath).getAbsolutePath()
                        + "\n\tor " + new File(appDir, configFilePath).getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return "";
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
            if (key.contains(DeterminantValueSet.PARAM_RULE_STR)) {
                if (value.contains("/") || value.contains("\\")) {
                    if (!new File(value).exists()) {
                        value = new File(appDir, value).getAbsolutePath();
                    }
                }
            }
            componentsSettings.put(key, value);
        }
        return componentsSettings;
    }


    @Override
    protected Object call() throws Exception {
        initiate(tasks, "db");
        runner.run();
        return null;
    }


}
