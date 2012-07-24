package com.netease.automate.master.entity;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;

/**
 * Entity.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public interface Entity {
    /**
     * Do the action
     * 
     * @param action
     *            the action object
     * @throws AutomateException
     *             if there are error
     */
    public void doAction(Action action) throws AutomateException;
}
