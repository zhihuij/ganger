package com.netease.automate.master.action;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.exception.ProcessLaunchedException;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.ProcessStatus;
import com.netease.automate.utils.Utils;

/**
 * Launch action.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class LaunchAction extends AbstractAction {
    public LaunchAction(ZooKeeper zk) {
        super(zk);
    }

    @Override
    protected String getAction() {
        return Global.CMD_LAUNCH;
    }

    @Override
    protected String getActionRootPath(String projectName) {
        return Utils.getProjectRuntimeRootPath(projectName);
    }

    protected void checkNode(String pkgPath) throws KeeperException, InterruptedException {
        if (!Utils.checkNode(zk, pkgPath)) {
            // if not exist, create package path
            zk.create(pkgPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    protected void checkTarget(String targetPath) throws ProcessLaunchedException {
        if (Utils.checkNode(zk, targetPath)) {
            // if target path exist, process is launched
            throw new ProcessLaunchedException("target process launched: " + targetPath);
        }
    }

    @Override
    public ActionOrder getActionOrder() {
        return ActionOrder.PRE_ORDER;
    }

    @Override
    protected void afterActionDone(AbstractProcess process, String pkgTargetPath) throws KeeperException, InterruptedException {
        process.setStatus(ProcessStatus.STARTED);
        zk.getData(pkgTargetPath, true, null);
    }
}
