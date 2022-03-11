package edu.utah.bmi.nlp.runner;


import edu.utah.bmi.nlp.easycie.core.AnnotationLogger;
import edu.utah.bmi.nlp.easycie.core.SettingOper;
import edu.utah.bmi.nlp.easycie.entry.TasksFX;
import edu.utah.bmi.nlp.uima.AdaptableCPEDescriptorStringDebugger;
import org.junit.jupiter.api.Test;

class RunCPEDescriptorDebugTaskTest {


    @Test
    void call() {
        SettingOper settingOper = new SettingOper("conf/test1/test1_config.xml");
        TasksFX tasks = settingOper.readSettings();
        AdaptableCPEDescriptorStringDebugger debugger = AdaptableCPEDescriptorStringDebugger.getInstance(tasks);
        debugger.process("This is a test doc for heart failure patients.", "");
        System.out.println(AnnotationLogger.records);
        System.out.println(AnnotationLogger.records.size());
    }
}