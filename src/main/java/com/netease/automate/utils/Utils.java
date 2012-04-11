package com.netease.automate.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import com.netease.automate.exception.ExecuteOsCmdException;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.PackageMeta;

/**
 * Utility.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class Utils {
    private static final String[] SHELL_CMD = { "/bin/sh", "-c" };

    public static void executeOsCmd(String cmd) {
        try {
            String[] fullCmd = { SHELL_CMD[0], SHELL_CMD[1], cmd };
            ProcessBuilder builder = new ProcessBuilder(fullCmd);
            builder.redirectErrorStream(true);

            System.err.println(Utils.constructString(fullCmd));

            Process p = builder.start();
            int exit = p.waitFor();
            if (exit != 0) {
                throw new RuntimeException("failed executed cmd: " + exit + " -> " + constructString(cmd));
            }
        } catch (IOException e) {
            throw new ExecuteOsCmdException(e);
        } catch (InterruptedException e) {
            throw new ExecuteOsCmdException(e);
        }
    }

    public static String executeOsCmdWithResult(String cmd) {
        try {
            String[] fullCmd = { SHELL_CMD[0], SHELL_CMD[1], cmd };

            ProcessBuilder builder = new ProcessBuilder(fullCmd);
            builder.redirectErrorStream(true);

            System.err.println(Utils.constructString(fullCmd));

            Process p = builder.start();

            BufferedReader bfInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String result = bfInput.readLine();

            // line include the target process id
            p.waitFor();

            return result;
        } catch (IOException e) {
            throw new ExecuteOsCmdException(e);
        } catch (InterruptedException e) {
            throw new ExecuteOsCmdException(e);
        }
    }

    public static String constructString(String... args) {
        StringBuilder sb = new StringBuilder();

        for (String str : args) {
            sb.append(str);
        }

        return sb.toString();
    }

    public static String getPropValue(Properties prop, String key) {
        String value = prop.getProperty(key);

        if (value == null) {
            throw new IllegalArgumentException("config: " + key + " cann't be null");
        }
        
        return value.trim();
    }

    public static boolean checkAndEmptySubNode(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
        if (Utils.checkNode(zk, path)) {
            List<String> subNodes = zk.getChildren(path, false);

            if (subNodes.size() > 0) {
                for (String s : subNodes) {
                    checkAndEmptySubNode(zk, Utils.constructString(path, Global.PATH_SEPARATOR, s));
                    zk.delete(Utils.constructString(path, Global.PATH_SEPARATOR, s), -1);
                }
            }

            return true;
        }

        return false;
    }

    public static void checkAndCreateNode(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
        Stat s = zk.exists(path, false);
        if (s == null) {
            zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public static boolean checkNode(ZooKeeper zk, String path) {
        Stat s;
        try {
            s = zk.exists(path, false);

            if (s != null) {
                return true;
            }
        } catch (KeeperException e) {
            // do nothing
        } catch (InterruptedException e) {
            // do nothing
        }

        return false;
    }

    public static byte[] getHeadMessage(List<String> msgList, ZooKeeper zk, String basePath) throws KeeperException,
            InterruptedException {
        // list size > 0
        Integer min = new Integer(msgList.get(0).substring(Global.QUEUE_REFIX.length()));
        String headMsgPath = msgList.get(0);

        for (String msg : msgList) {
            Integer tempValue = new Integer(msg.substring(Global.QUEUE_REFIX.length()));
            if (tempValue < min) {
                min = tempValue;
                headMsgPath = msg;
            }
        }

        String fullPath = Utils.constructString(basePath, Global.PATH_SEPARATOR, headMsgPath);
        byte[] b = zk.getData(fullPath, false, null);
        zk.delete(fullPath, -1);

        return b;
    }

    public static String getSlaveRootPath(String slave) {
        return Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE, Global.PATH_SEPARATOR, slave);
    }

    public static String getSlaveTargetRootPath(String slave) {
        return Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE, Global.PATH_SEPARATOR, slave,
                Global.SLAVE_TARGET);
    }

    public static String getSlaveStatusPath(String slave) {
        return Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE, Global.PATH_SEPARATOR, slave,
                Global.SLAVE_STATUS);
    }

    public static String getSlaveChannelRootPath(String slave) {
        return Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE, Global.PATH_SEPARATOR, slave,
                Global.SLAVE_MSG_CHANNEL);
    }

    public static String getProjectRootPath(String projectName) {
        return Utils.constructString(Global.PROJECTS_ROOT, Global.PATH_SEPARATOR, projectName);
    }

    public static String getProjectMetaRootPath(String projectName) {
        return Utils.constructString(Global.PROJECTS_ROOT, Global.PATH_SEPARATOR, projectName, Global.PROJECT_META);
    }

    public static String getProjectDeployRootPath(String projectName) {
        return Utils.constructString(Global.PROJECTS_ROOT, Global.PATH_SEPARATOR, projectName, Global.PROJECT_DEPLOY);
    }

    public static String getProjectRuntimeRootPath(String projectName) {
        return Utils.constructString(Global.PROJECTS_ROOT, Global.PATH_SEPARATOR, projectName, Global.PROJECT_RUNTIME);
    }

    public static String getPackageFullPath(PackageMeta meta) {
        StringBuilder sb = new StringBuilder(meta.getPkgRoot());
        sb.append(File.separator);
        sb.append(meta.getPkgName());
        sb.append(File.separator);
        sb.append(meta.getPkgVersion());
        sb.append(File.separator);
        sb.append(meta.getPkgFullName());

        return sb.toString();
    }

    public static String getDeployFullPath(PackageMeta meta) {
        StringBuilder sb = new StringBuilder(meta.getDeployPath());
        sb.append(File.separator);
        sb.append(meta.getPkgName());
        sb.append(File.separator);
        sb.append(meta.getPkgVersion());

        return sb.toString();
    }
}
