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
    public void doAction(AbstractProcess process, String projectName, PackageMeta packageMeta, String target) throws AutomateException;

    public ActionOrder getActionOrder();
}
