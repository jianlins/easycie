package edu.utah.bmi.nlp.core;


import java.util.*;

/**
 * Created by Jianlin_Shi on 10/26/15.
 */
public class NLPTasks implements TasksInf {
    protected final LinkedHashMap<String, TaskInf> obtasks = new LinkedHashMap<>();
    protected HashMap<String, Integer> nameId = new HashMap<>();
    protected int lastId = 0;

    public List<Map.Entry<String, TaskInf>> getTasksList() {
        return new ArrayList(obtasks.entrySet());
    }

    public void addTask(TaskInf task) {
        nameId.put(task.getTaskName(), lastId);
        obtasks.put(task.getTaskName(), task);
        lastId++;
    }

    public TaskInf getTask(String taskName) {
        if (obtasks.containsKey(taskName)) {
            return obtasks.get(taskName);
        } else {
            return null;
        }
    }

    public int getTaskId(String taskName) {
        return nameId.get(taskName);
    }

    public TasksInf clone() {
        TasksInf newTasksFX = new NLPTasks();
        for (TaskInf task : this.obtasks.values()) {
            newTasksFX.addTask(task);
        }
        return newTasksFX;
    }
}
