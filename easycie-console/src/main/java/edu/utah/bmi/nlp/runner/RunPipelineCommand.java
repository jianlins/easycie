package edu.utah.bmi.nlp.runner;

import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.easycie.core.SettingOper;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

public class RunPipelineCommand {
    public static Logger logger= IOUtil.getLogger(RunPipelineCommand.class);
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        // add options
        options.addOption("c", "config_file", true, "Path of project configuration file.");
        CommandLineParser parser = new DefaultParser();
        String configFile = "";
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("c"))
                configFile = cmd.getOptionValue("c", "conf/test/test_config.xml");
        } catch (ParseException e) {
            logger.fine(e.getMessage());
        }
        if (configFile.length() > 0)
            runpipe(new File(configFile));
        else
            logger.warning("config_file has not been specified");
    }


    private static void runpipe(File configFile) throws Exception {
        SettingOper settingOper = new SettingOper(configFile.getAbsolutePath());
        TasksInf tasks = settingOper.readSettings();
        RunCPEDescriptorTask runTask = new RunCPEDescriptorTask(tasks);
        runTask.tasks = tasks;
        runTask.call();
    }
}
