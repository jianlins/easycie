package edu.utah.bmi.nlp;


import edu.utah.bmi.nlp.runner.RunPipelineCommand;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestRunPipelineCommand {
    public static void main(String[] args) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.now();
        System.out.println("Start processing at: " + dtf.format(start));
        String config="conf/scout_edw/dvt_edw/scout_dvt.xml";
        System.out.println(new File(config).exists());
        RunPipelineCommand.main(new String[]{"version=3.0","-c", config});

    }
}