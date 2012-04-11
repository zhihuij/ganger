package com.netease.automate.master;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;

import com.netease.automate.exception.ZooKeeperException;
import com.netease.automate.meta.SlaveStatus;
import com.netease.automate.meta.SlaveTarget;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;

public class SlaveStatusWatcher implements Watcher {
    private static final String SLAVE_ADDR_MATCHER = "/arc/slave/(.+)/status";

    private Map<String, SlaveTarget> slaveTargetMap = null;
    private Pattern pattern = null;

    private ZooKeeper zk;

    public SlaveStatusWatcher(Map<String, SlaveTarget> slaveTargetMap, ZooKeeper zk) {
        this.slaveTargetMap = slaveTargetMap;
        this.pattern = Pattern.compile(SLAVE_ADDR_MATCHER);

        this.zk = zk;
    }

    @Override
    public void process(WatchedEvent event) {
        String nodePath = event.getPath();

        if (event.getType() == EventType.NodeDeleted) {
            // node offline
            Matcher matcher = pattern.matcher(nodePath);

            String slaveAddr = null;
            if (matcher.matches()) {
                slaveAddr = matcher.group(1);
            }

            if (slaveAddr != null) {
                slaveTargetMap.remove(slaveAddr);
                LogUtils.logInfoLine(Utils.constructString("slave -> ", slaveAddr, " offline"));
            }
        } else if (event.getType() == EventType.NodeChildrenChanged) {
            // node added
            try {
                List<String> slaveList = zk.getChildren(nodePath, this);

                for (String slave : slaveList) {
                    SlaveTarget slaveTarget = slaveTargetMap.get(slave);
                    if (slaveTarget == null) {
                        slaveTarget = new SlaveTarget(slave);
                        slaveTarget.setStatus(SlaveStatus.ONLINE);

                        slaveTargetMap.put(slave, slaveTarget);

                        zk.getData(Utils.getSlaveStatusPath(slave), this, null);

                        LogUtils.logInfoLine(Utils.constructString("slave -> ", slave, " online"));
                    }
                }
            } catch (KeeperException e) {
                throw new ZooKeeperException(e);
            } catch (InterruptedException e) {
                throw new ZooKeeperException(e);
            }
        }
    }
}
