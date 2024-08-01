package edu.utah.bmi.nlp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TasksInf {
    public abstract List<Map.Entry<String, TaskInf>> getTasksList();

    public abstract void addTask(TaskInf task) ;

    public abstract TaskInf getTask(String taskName);

    public abstract int getTaskId(String taskName);

    public abstract TasksInf clone();
}
