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
        String config = "conf/scout_edw/dvt_edw/scout_dvt.xml";
//        config = "conf/scout_edw/ssi_edw/scout_ssi.xml";
        System.out.println(config+" exists: "+new File(config).exists());
        String conf_folder=new File(config).getParentFile().getName();
        RunPipelineCommand.main(new String[]{"-c", config, "-p", "classes/"+conf_folder,"-l","edu.utah.bmi.nlp.uima.loggers.NLPDBEDWLogger"});

    }
}