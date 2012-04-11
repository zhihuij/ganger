package com.netease.automate.slave;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;

import com.netease.automate.utils.Utils;

/**
 * Message watcher for slave.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class SlaveMessageWatcher implements Watcher {
    private AutoSlave slave;

    public SlaveMessageWatcher(AutoSlave slave) {
        this.slave = slave;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == EventType.NodeChildrenChanged) {
            try {
                String basePath = event.getPath();

                List<String> list = slave.getZooKeeper().getChildren(basePath, this);
                if (list.size() > 0) {
                    byte[] msg = Utils.getHeadMessage(list, slave.getZooKeeper(), basePath);
                    slave.processMessage(msg);
                }
            } catch (KeeperException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
