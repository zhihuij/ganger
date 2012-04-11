package com.netease.automate.master.event;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;

import com.netease.automate.exception.ZooKeeperException;
import com.netease.event.Reactor;

/**
 * Watcher for target process event, based on ZooKeeper.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class EventWatcher implements Watcher {
    private ZooKeeper zk;
    private Reactor reactor;

    public EventWatcher(Reactor reactor) {
        this.reactor = reactor;
    }

    public void bind(ZooKeeper zk) {
        this.zk = zk;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getPath() == null) {
            // do nothing
            return;
        }

        try {
            if (event.getType() == EventType.NodeCreated) {
                reactor.dispatchEvent(new ProcessEventContext(EventTypes.PROCESS_STARTED, event.getPath()));
            } else if (event.getType() == EventType.NodeDeleted) {
                reactor.dispatchEvent(new ProcessEventContext(EventTypes.PROCESS_STOPPED, event.getPath()));
            }

            if (event.getType() != EventType.NodeDeleted) {
                zk.getData(event.getPath(), true, null);
            }
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }
    }
}
