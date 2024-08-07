package edu.utah.bmi.nlp.runner;

import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.easycie.core.SettingOper;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

public class RunPipelineCommand {
    public static Logger logger = IOUtil.getLogger(RunPipelineCommand.class);

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("c", "config_file", true, "Path of project configuration file.");
        /* This location is for UIMA type system related classes.
        Set to different path for different pipelines to avoid any class conflicts. */
        options.addOption("p", "compiled_path", true, "Location to store the dynamically compiled Java classes, by default 'classes' is used.");
        options.addOption("l", "logger_class", true, "Logger class name to be used, must implement 'edu.utah.bmi.nlp.uima.loggers.UIMALogger' interface. " +
                "By default (if not specified or empty string), 'edu.utah.bmi.nlp.uima.loggers.NLPDBConsoleLogger' is used");
        CommandLineParser parser = new DefaultParser();
        String configFile = "";
        String compiledClassPath = "classes";
        String loggerClassName = "";
        try {
            CommandLine cmd = parser.parse(options, args);
            configFile = cmd.getOptionValue("c", "conf/test/test_config.xml");
            compiledClassPath = cmd.getOptionValue("p", "classes");
            loggerClassName = cmd.getOptionValue("l", "");
        } catch (ParseException e) {
            logger.fine(e.getMessage());
        }
        if (configFile.length() == 0)
            logger.warning("config_file has not been specified");
        else if (!new File(configFile).exists()) {
            logger.warning(new File(configFile).getAbsolutePath() + " doesn't exist.");
        } else
            runpipe(new File(configFile), compiledClassPath, loggerClassName);

    }


    private static void runpipe(File configFile, String compiledClassPath, String loggerClassName) {
        SettingOper settingOper = new SettingOper(configFile.getAbsolutePath());
        TasksInf tasks = settingOper.readSettings();
        RunCPEDescriptorTask runTask = new RunCPEDescriptorTask(tasks, compiledClassPath, loggerClassName);
        runTask.tasks = tasks;
        try {
            runTask.call();
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }
}
