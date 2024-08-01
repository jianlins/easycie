package edu.utah.bmi.nlp.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TaskInf {
    public abstract String getValue(String key);

    public abstract String getValue(String key, String defaultValue);

    public abstract String getDesc(String key);

    public abstract String getName(String key);

    public abstract void setValue(String key, String value, String desc, String doubleClick);

    public abstract void setValue(String key, String value, String desc, String doubleClick, String openClick);

    public abstract void setValue(String key, String value);

    public abstract String getTaskName();

    public abstract void addExecute(String label, String className, String memo);


    public abstract String getExecuteValue(String key);

    public abstract String getExecuteDesc(String key);

    public abstract LinkedHashMap<String, SettingAb> getChildSettings(String parentKey);

    public abstract List<Map.Entry<String, SettingAb>> getExecutes();

    public abstract List<Map.Entry<String, SettingAb>> getSettings();
}
