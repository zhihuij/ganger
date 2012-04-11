package com.netease.automate.master.task;

import org.apache.zookeeper.ZooKeeper;

import com.netease.automate.exception.SlaveOfflineException;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.MessageMeta;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.utils.JsonUtils;
import com.netease.automate.utils.Utils;

/**
 * Common task for packages(processes).
 * 
 * @author jiaozhihui@corp.netease.com
 */
public abstract class CommonPackageTask implements Task {
    protected ZooKeeper zk;
    protected String pkgTargetPath;

    private String projectName;
    private PackageMeta packageMeta;
    private String target;
    private String action;

    public CommonPackageTask(ZooKeeper zk, String projectName, String action, PackageMeta packageMeta, String target,
            String pkgTargetPath) {
        this.zk = zk;
        this.projectName = projectName;
        this.packageMeta = packageMeta;
        this.target = target;
        this.pkgTargetPath = pkgTargetPath;

        this.action = action;
    }

    @Override
    public abstract boolean checkTask();

    @Override
    public void doTask() {
        String slaveMsgChannelRoot = Utils.getSlaveChannelRootPath(target);
        String msgPath = Utils.constructString(slaveMsgChannelRoot, Global.PATH_SEPARATOR, Global.QUEUE_REFIX);

        MessageMeta message = new MessageMeta(action, projectName, JsonUtils.getObjectData(packageMeta));
        byte[] data = JsonUtils.getObjectData(message);

        if (Utils.checkNode(zk, Utils.getSlaveStatusPath(target))) {
            ZKMessageTask msgTask = new ZKMessageTask(zk, msgPath, data);
            msgTask.doTask();
        } else {
            throw new SlaveOfflineException("target slave offline: " + target);
        }
    }
}
