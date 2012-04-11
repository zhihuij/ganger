package com.netease.automate.master.action;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import com.netease.automate.exception.ProcessNotLaunchedException;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.ProcessStatus;
import com.netease.automate.utils.Utils;

/**
 * Restart action.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class RestartAction extends AbstractAction {
    public RestartAction(ZooKeeper zk) {
        super(zk);
    }

    @Override
    protected String getAction() {
        return Global.CMD_RESTART;
    }

    @Override
    protected String getActionRootPath(String projectName) {
        return Utils.getProjectRuntimeRootPath(projectName);
    }

    @Override
    protected void checkNode(String pkgPath) throws KeeperException, InterruptedException {
        if (!Utils.checkNode(zk, pkgPath)) {
            throw new RuntimeException("target package not launched");
        }
    }

    @Override
    protected void checkTarget(String targetPath) throws ProcessNotLaunchedException {
        if (!Utils.checkNode(zk, targetPath)) {
            throw new ProcessNotLaunchedException("target process not launched");
        }
    }

    @Override
    public ActionOrder getActionOrder() {
        return ActionOrder.POST_ORDER;
    }

    @Override
    protected void beforeDoAction(AbstractProcess process, String pkgTargetPath) throws KeeperException,
            InterruptedException {
        process.setStatus(ProcessStatus.RESTARTING);
    }

    @Override
    protected void afterActionDone(AbstractProcess process, String pkgTargetPath) throws KeeperException,
            InterruptedException {
        process.setStatus(ProcessStatus.RESTARTED);
    }
}
