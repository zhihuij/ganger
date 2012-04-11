package com.netease.automate.master.task;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.exception.ZooKeeperException;

/**
 * Message task for communication between master and slave, can replaced by other communication
 * method.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ZKMessageTask implements Task {
    private ZooKeeper zk;
    private String targetPath;
    private byte[] message;

    public ZKMessageTask(ZooKeeper zk, String targetPath, byte[] message) {
        this.zk = zk;
        this.targetPath = targetPath;
        this.message = message;
    }

    @Override
    public boolean checkTask() {
        // do nothing
        return true;
    }

    @Override
    public void doTask() {
        try {
            zk.create(targetPath, message, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }
    }
}
