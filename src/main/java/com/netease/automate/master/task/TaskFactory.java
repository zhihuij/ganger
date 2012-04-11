package com.netease.automate.master.task;

import org.apache.zookeeper.ZooKeeper;

import com.netease.automate.meta.Global;
import com.netease.automate.meta.PackageMeta;

/**
 * Task factory.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class TaskFactory {
    public static Task createTask(ZooKeeper zk, String projectName, String action, PackageMeta packageMeta,
            String target, String pkgTargetPath) {
        if (Global.CMD_STOP.equals(action)) {
            return new DeleteTask(zk, projectName, action, packageMeta, target, pkgTargetPath);
        } else {
            return new AddTask(zk, projectName, action, packageMeta, target, pkgTargetPath);
        }
    }
}
