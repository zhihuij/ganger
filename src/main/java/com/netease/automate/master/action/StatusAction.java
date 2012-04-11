package com.netease.automate.master.action;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.master.entity.RuntimeProcess;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;

/**
 * Status action for target process.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class StatusAction implements Action {

    @Override
    public void doAction(AbstractProcess process, String projectName, PackageMeta packageMeta, String target)
            throws AutomateException {
        if (process instanceof RuntimeProcess) {
            LogUtils.logInfoLine(Utils
                    .constructString(process.getProcessName(), "\t\t", process.getStatus().toString()));
        }
    }

    @Override
    public ActionOrder getActionOrder() {
        return ActionOrder.PRE_ORDER;
    }

}
