package edu.utah.bmi.nlp.runner;

import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.easycie.core.SettingOper;

import java.io.File;

import org.apache.commons.cli.*;

public class RunPipelineCommand {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        // add options
        options.addOption("c", "config_file", true, "Path of project configuration file.");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String configFile = cmd.getOptionValue("c", "conf/test/test_config.xml");
            runpipe(new File(configFile));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private static void runpipe(File configFile) throws Exception {
        SettingOper settingOper = new SettingOper(configFile.getAbsolutePath());
        TasksInf tasks = settingOper.readSettings();
        RunCPEDescriptorTask runTask = new RunCPEDescriptorTask(tasks);
        runTask.tasks = tasks;
        runTask.call();
    }
}
