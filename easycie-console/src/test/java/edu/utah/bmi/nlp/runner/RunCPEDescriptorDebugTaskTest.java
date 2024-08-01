package edu.utah.bmi.nlp.runner;


import edu.utah.bmi.nlp.easycie.core.AnnotationLogger;
import edu.utah.bmi.nlp.easycie.core.SettingOper;
import edu.utah.bmi.nlp.core.TasksInf;
import edu.utah.bmi.nlp.uima.AdaptableCPEDescriptorStringDebugger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RunCPEDescriptorDebugTaskTest {
    //passed single test, but not successful when testing with other tests.
    @Disabled
    @Test
    public void test1() {
        SettingOper settingOper = new SettingOper("conf/test1/test1_config.xml");
        TasksInf tasks = settingOper.readSettings();
        AdaptableCPEDescriptorStringDebugger debugger = AdaptableCPEDescriptorStringDebugger.getInstance(tasks);
        debugger.process("This is a test doc for heart failure patients.", "");
        System.out.println(AnnotationLogger.records);
        assert (AnnotationLogger.records.size()==10);
    }
}