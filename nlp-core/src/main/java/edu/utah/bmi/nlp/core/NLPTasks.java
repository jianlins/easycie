package edu.utah.bmi.nlp.core;


import java.util.*;

/**
 * Created by Jianlin_Shi on 10/26/15.
 */
public class NLPTasks {
    protected final LinkedHashMap<String, NLPTask> obtasks = new LinkedHashMap<String, NLPTask>();
    protected HashMap<String, Integer> nameId = new HashMap<>();
    private int lastId = 0;

    public List<Map.Entry<String, NLPTask>> getTasksList() {
        return new ArrayList(obtasks.entrySet());
    }

    public void addTask(NLPTask task) {
        nameId.put(task.getTaskName(), lastId);
        obtasks.put(task.getTaskName(), task);
        lastId++;
    }

    public NLPTask getTask(String taskName) {
        if (obtasks.containsKey(taskName)) {
            return obtasks.get(taskName);
        } else {
            return null;
        }
    }

    public int getTaskId(String taskName) {
        return nameId.get(taskName);
    }

    public NLPTasks clone() {
        NLPTasks newTasksFX = new NLPTasks();
        for (NLPTask task : this.obtasks.values()) {
            newTasksFX.addTask(task);
        }
        return newTasksFX;
    }
}
