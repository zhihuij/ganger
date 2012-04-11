package com.netease.automate.master;

import java.util.concurrent.Semaphore;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;

import com.netease.automate.exception.ZooKeeperException;
import com.netease.automate.master.task.Task;

/**
 * Watcher of the action result of slave node.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class SlaveActionWatcher implements Watcher {
    private ZooKeeper zk;
    private Task task;
    private Semaphore sep;

    public SlaveActionWatcher(ZooKeeper zk, Task task, Semaphore sep) {
        this.zk = zk;
        this.task = task;
        this.sep = sep;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == EventType.NodeChildrenChanged) {
            if (task.checkTask()) {
                sep.release();
                return;
            }
        }

        try {
            zk.getChildren(event.getPath(), this);
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }
    }
}
