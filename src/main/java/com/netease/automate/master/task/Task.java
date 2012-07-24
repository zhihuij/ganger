package com.netease.automate.master.task;

/**
 * Task interface.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public interface Task {
    /**
     * Do the task.
     */
    public void doTask();

    /**
     * Check the task status.
     * 
     * @return true if task has been done successfully, false otherwise
     */
    public boolean checkTask();
}
