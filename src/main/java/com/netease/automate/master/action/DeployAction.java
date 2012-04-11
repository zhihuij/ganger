package com.netease.automate.master.action;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.meta.Global;
import com.netease.automate.utils.Utils;

/**
 * Deploy action.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class DeployAction extends AbstractAction {
    public DeployAction(ZooKeeper zk) {
        super(zk);
    }

    @Override
    protected String getAction() {
        return Global.CMD_DEPLOY;
    }

    @Override
    protected String getActionRootPath(String projectName) {
        return Utils.getProjectDeployRootPath(projectName);
    }

    @Override
    protected void checkNode(String pkgPath) throws KeeperException, InterruptedException {
        if (!Utils.checkAndEmptySubNode(zk, pkgPath)) {
            // if deploy exist, empty exist node, do deploy again
            zk.create(pkgPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    @Override
    protected void checkTarget(String targetPath) {
        // do nothing, target path is not exist (guaranteed by checkNode)
    }

    @Override
    public ActionOrder getActionOrder() {
        return ActionOrder.PRE_ORDER;
    }
}
