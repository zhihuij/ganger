package com.netease.automate.master.entity;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;

/**
 * Entity.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public interface Entity {
    public void doAction(Action action) throws AutomateException;
}
