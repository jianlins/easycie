package edu.utah.bmi.nlp.easycie.core;


import edu.utah.bmi.nlp.core.NLPTask;
import edu.utah.bmi.nlp.core.NLPTasks;
import edu.utah.bmi.nlp.core.TaskInf;
import edu.utah.bmi.nlp.core.TasksInf;
import org.dom4j.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Jianlin_Shi on 10/26/15.
 */
public class SettingOper {
    private TasksInf tasks = new NLPTasks();
    protected File settingFile;
    protected File projectDir;
    protected XMLConfigOper config;

    protected void SettingOper(){
        
    }

    public SettingOper(String settingFileName) {
        settingFile = new File(settingFileName);
        projectDir=settingFile;
        while (projectDir.getParentFile()!=null && !projectDir.getName().equals("conf"))
            projectDir=projectDir.getParentFile();
        projectDir=projectDir.getAbsoluteFile().getParentFile();

//        System.out.println(settingFile.getAbsoluteFile());
        config = new XMLConfigOper(settingFileName);
    }

    public TasksInf readSettings() {
        Element root = config.document.getRootElement();
        for (Iterator t = root.elementIterator(); t.hasNext(); ) {
            Element taskEle = (Element) t.next();
            String taskName = taskEle.getName();
            TaskInf task = new NLPTask(taskName);
            iterateTaskElement(taskEle, task, "");
            tasks.addTask(task);
        }
        tasks.addTask(genProjectDir());
        return tasks;
    }

    protected TaskInf genProjectDir(){
        TaskInf task = new NLPTask("projectDir");
        task.setValue("projectDir", projectDir.getAbsolutePath());
        return task;
    }

    protected void iterateTaskElement(Element taskEle, TaskInf task, String prefix) {
        if (prefix.length() > 0)
            prefix = prefix + "/";
        for (Iterator gs = taskEle.elementIterator(); gs.hasNext(); ) {
            Element ele = (Element) gs.next();
            String elementName = prefix + ele.getName();
            String value = ele.getTextTrim().trim();
            String memo = ele.attributeValue("memo");
            String command = ele.attributeValue("doubleClick");
            String openClick = ele.attributeValue("openClick");
            if (!elementName.startsWith("executes")) {
                task.setValue(elementName, value, memo, command, openClick);
                iterateTaskElement(ele, task, elementName);
            } else {
                for (Iterator s = ele.elementIterator(); s.hasNext(); ) {
                    Element setting = (Element) s.next();
                    String settingName = setting.getName();
                    value = setting.getText();
                    memo = setting.attributeValue("memo");
                    if (memo == null)
                        memo = "";
                    task.addExecute(elementName + "/" + settingName, value, memo);
                }
            }
        }
    }

    public void ChangeValues(HashMap<String, String> valueChanges) {
        for (Map.Entry<String, String> entry : valueChanges.entrySet()) {
            config.setValue(entry.getKey(), entry.getValue());
            System.out.println("Set " + entry.getKey() + " to: '" + entry.getValue() + "'");
        }
    }

    public void ChangeMemos(HashMap<String, String> valueChanges) {
        for (Map.Entry<String, String> entry : valueChanges.entrySet()) {
            config.setAttributeValue(entry.getKey(), entry.getValue());
        }
    }

    public void saveConfigs() {
        config.save();
    }

    public void saveConfigs(File file) {
        config.save(file);
    }

    public void writeTasks(TasksInf tasks) {
        Element root = config.document.getRootElement();
        for (Iterator t = root.elementIterator(); t.hasNext(); ) {
            Element taskEle = (Element) t.next();
            String taskName = taskEle.getName();
            TaskInf task = tasks.getTask(taskName);
            for (Iterator gs = taskEle.elementIterator(); gs.hasNext(); ) {
                Element group = (Element) gs.next();
                String groupName = group.getName();
                for (Iterator s = group.elementIterator(); s.hasNext(); ) {
                    Element setting = (Element) s.next();
                    String settingName = setting.getName();
                    String value = setting.getText().trim();
                    String memo = setting.attributeValue("memo");
                    if (!groupName.startsWith("doubleClick")) {
                        if (!task.getValue(groupName + "/" + settingName).equals(value)
//                                && !settingName.endsWith("password")
                                ) {
                            System.out.println("Change value of //" + taskName + "/" + groupName + "/" + settingName + "from:");
                            System.out.println("\t{" + value + "}\n\t{" + task.getValue(groupName + "/" + settingName) + "}");
                            config.setValue("//" + taskName + "/" + groupName + "/" + settingName, task.getValue(groupName + "/" + settingName));
                        }
                        if (memo == null) {
                            if (task.getDesc(groupName + "/" + settingName) != null && task.getDesc(groupName + "/" + settingName).trim().length() > 0)
                                config.addAttribute("//" + taskName + "/" + groupName + "/" + settingName, "memo", task.getDesc(groupName + "/" + settingName));
                        } else if (!task.getDesc(groupName + "/" + settingName).equals(memo)) {
                            System.out.println("write: " + "//" + taskName + "/" + groupName + "/" + settingName + "/@memo" + " : " + task.getDesc(groupName + "/" + settingName));
                            config.setAttributeValue("//" + taskName + "/" + groupName + "/" + settingName + "/@memo", task.getDesc(groupName + "/" + settingName));
                        }
                    }
                }
            }
        }
        config.save();
    }

    public File getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }
}
