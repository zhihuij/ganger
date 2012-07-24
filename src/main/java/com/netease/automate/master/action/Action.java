package com.netease.automate.master.action;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.meta.PackageMeta;

/**
 * Action interface.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public interface Action {
    /**
     * Do the action.
     * 
     * @param process
     *            the target process
     * @param projectName
     *            name of the project
     * @param packageMeta
     *            project meta
     * @param target
     *            the target address
     * @throws AutomateException
     *             when there are errors
     */
    public void doAction(AbstractProcess process, String projectName, PackageMeta packageMeta, String target)
            throws AutomateException;

    /**
     * Get the action order.
     * 
     * @return the action order object
     */
    public ActionOrder getActionOrder();
}
