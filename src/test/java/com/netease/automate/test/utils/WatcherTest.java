package com.netease.automate.test.utils;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.utils.Utils;

import junit.framework.TestCase;

class RootWatcher implements Watcher {
    private ZooKeeper zk = null;

    public void setZK(ZooKeeper zk) {
        this.zk = zk;
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("root watcher: " + event.getPath() + ", " + event.getType().toString());

        try {
            if (!(event.getType() == EventType.NodeDeleted) && event.getPath() != null) {
                zk.getChildren(event.getPath(), true);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class NodeWatcher implements Watcher {
    private ZooKeeper zk;

    public NodeWatcher(ZooKeeper zk) {
        this.zk = zk;
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("node watcher: " + event.getPath() + ", " + event.getType().toString());
        try {
            if (!(event.getType() == EventType.NodeDeleted)) {
                zk.getChildren(event.getPath(), this);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class WatcherTest extends TestCase {
    private static final String TEST_ROOT = "/test";
    private static final String NODE_L1 = "/level1";
    private static final String NODE_L2 = "/level2";
    private static final String NODE_L3 = "/level3";
    private static final String NODE_L4 = "/level4";

    public void testWatcher() throws Exception {
        String zkAddress = "10.100.82.217:2181";
        RootWatcher rootWatcher = new RootWatcher();
        ZooKeeper zk = new ZooKeeper(zkAddress, 3000, rootWatcher);
        rootWatcher.setZK(zk);

        zk.create(TEST_ROOT, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        String l1FullPath = Utils.constructString(TEST_ROOT, NODE_L1);
        zk.create(l1FullPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        String l2FullPath = Utils.constructString(TEST_ROOT, NODE_L2);
        zk.create(l2FullPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.getChildren(l2FullPath, new NodeWatcher(zk));
//        zk.getChildren(l2FullPath, true);

        String l3FullPath = Utils.constructString(l2FullPath, NODE_L3);
        zk.create(l3FullPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.setData(l3FullPath, "test1".getBytes(), -1);
        zk.setData(l3FullPath, "test2".getBytes(), -1);
        zk.setData(l3FullPath, "test3".getBytes(), -1);
        zk.setData(l3FullPath, "test4".getBytes(), -1);

        String l4FullPath = Utils.constructString(l2FullPath, NODE_L4);
        zk.create(l4FullPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        System.out.println("created");

        zk.delete(l4FullPath, -1);
        zk.delete(l3FullPath, -1);
        zk.delete(l2FullPath, -1);
        zk.delete(l1FullPath, -1);
        zk.delete(TEST_ROOT, -1);
    }
}
