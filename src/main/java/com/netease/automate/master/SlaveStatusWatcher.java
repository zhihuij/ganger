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
import com.netease.automate.meta.Global;
import com.netease.automate.meta.SlaveStatus;
import com.netease.automate.meta.SlaveTarget;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;

/**
 * Watcher for slave status.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class SlaveStatusWatcher implements Watcher {
    /**
     * Path pattern for slave status.
     */
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

                try {
                    // add watcher for this slave, check if it is restarted
                    zk.getChildren(Utils.getSlaveRootPath(slaveAddr), this);
                } catch (KeeperException e) {
                    throw new ZooKeeperException(e);
                } catch (InterruptedException e) {
                    throw new ZooKeeperException(e);
                }
            }
        } else if (event.getType() == EventType.NodeChildrenChanged) {
            try {
                if (!nodePath.equals(Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE))) {
                    // node restart
                    List<String> slaveSubNode = zk.getChildren(nodePath, false);
                    for (String subNode : slaveSubNode) {
                        if (Global.SLAVE_STATUS.indexOf(subNode) > 0) {
                            // status node
                            String statusPath = Utils.constructString(nodePath, Global.PATH_SEPARATOR, subNode);

                            Matcher matcher = pattern.matcher(statusPath);

                            String slaveAddr = null;
                            if (matcher.matches()) {
                                slaveAddr = matcher.group(1);
                            }

                            checkSlave(slaveAddr);
                        }
                    }
                } else {
                    // node added
                    List<String> slaveList = zk.getChildren(nodePath, this);

                    for (String slave : slaveList) {
                        checkSlave(slave);
                    }
                }
            } catch (KeeperException e) {
                throw new ZooKeeperException(e);
            } catch (InterruptedException e) {
                throw new ZooKeeperException(e);
            }
        }
    }

    private void checkSlave(String slave) throws KeeperException, InterruptedException {
        SlaveTarget slaveTarget = slaveTargetMap.get(slave);
        if (slaveTarget == null) {
            slaveTarget = new SlaveTarget(slave);
            slaveTarget.setStatus(SlaveStatus.ONLINE);

            slaveTargetMap.put(slave, slaveTarget);

            zk.getData(Utils.getSlaveStatusPath(slave), this, null);

            LogUtils.logInfoLine(Utils.constructString("slave -> ", slave, " online"));
        }
    }
}
