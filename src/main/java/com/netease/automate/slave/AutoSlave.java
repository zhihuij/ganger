package com.netease.automate.slave;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.exception.ExecuteOsCmdException;
import com.netease.automate.exception.ProcessLaunchedException;
import com.netease.automate.exception.ProcessNotLaunchedException;
import com.netease.automate.exception.ZooKeeperException;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.MasterMeta;
import com.netease.automate.meta.MessageMeta;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProcessMeta;
import com.netease.automate.utils.JsonUtils;
import com.netease.automate.utils.Utils;

/**
 * Slave of automate tools.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class AutoSlave {
    private static Logger logger = Logger.getLogger(AutoSlave.class);

    private static final String SLAVE_HOME_ENV = "slaveHome";

    private static final String DEFAULT_LAUNCH_SCRIPT = "launch.sh";
    private static final String DEFAULT_LAUNCH_DIR = "bin";

    private ZooKeeper zk = null;
    private String zkAddress;
    private String localAddr;
    private String slaveBasePath;
    private String slaveTargetPath;
    private String slaveChannelPath;

    private String launcher;

    private ProcessHolder processHolder;

    public AutoSlave(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public void startSlave() {
        String slaveHome = System.getProperty(SLAVE_HOME_ENV);
        String binPath = Utils.constructString(slaveHome, File.separator, DEFAULT_LAUNCH_DIR);
        File launchFile = new File(binPath, DEFAULT_LAUNCH_SCRIPT);
        if (!launchFile.exists()) {
            throw new IllegalArgumentException("launch script: " + launchFile.getAbsolutePath() + " not found");
        } else {
            if (!launchFile.canExecute()) {
                launchFile.setExecutable(true);
            }
            launcher = launchFile.getAbsolutePath();
        }

        processHolder = ProcessHolder.getInstance(launcher, this);

        try {
            zk = new ZooKeeper(zkAddress, 3000, null);
        } catch (IOException e) {
            System.err.println("Create zk error: " + e.getMessage());
            zk = null;
            return;
        }

        try {
            localAddr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // do nothing
        }

        slaveBasePath = Utils.getSlaveRootPath(localAddr);
        slaveTargetPath = Utils.getSlaveTargetRootPath(localAddr);
        slaveChannelPath = Utils.getSlaveChannelRootPath(localAddr);

        zk.sync(slaveBasePath, null, null);

        try {
            Utils.checkAndCreateNode(zk, Global.ARC_ROOT);
            Utils.checkAndCreateNode(zk, Utils.constructString(Global.ARC_ROOT, Global.ARC_MASTER));
            Utils.checkAndCreateNode(zk, Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE));

            Utils.checkAndCreateNode(zk, slaveBasePath);
            Utils.checkAndCreateNode(zk, slaveTargetPath);
            Utils.checkAndCreateNode(zk, slaveChannelPath);

            List<String> pidList = zk.getChildren(slaveTargetPath, false);

            if (pidList.size() != 0) {
                // rebooted slave
                for (String pid : pidList) {
                    String pidPath = Utils.constructString(slaveTargetPath, Global.PATH_SEPARATOR, pid);

                    byte[] data = zk.getData(pidPath, false, null);
                    ProcessMeta processMeta = JsonUtils.getObject(data, ProcessMeta.class);

                    processHolder.addLiveProcess(processMeta.getProjectName(), processMeta.getProcessName(),
                            processMeta.getLaunchScript(), pid);
                }
            }

            List<String> msgList = zk.getChildren(slaveChannelPath, false);
            while (msgList.size() != 0) {
                // get msg and process it
                byte[] msg = Utils.getHeadMessage(msgList, zk, slaveChannelPath);
                processMessage(msg);

                msgList = zk.getChildren(slaveChannelPath, false);
            }

            zk.getChildren(slaveChannelPath, new SlaveMessageWatcher(this));

            // set slave status
            zk.create(Utils.getSlaveStatusPath(localAddr), new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException e) {
            logger.error(e.getMessage());
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            throw new ZooKeeperException(e);
        }

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public ZooKeeper getZooKeeper() {
        return zk;
    }

    public void addTargetPid(String pid, String projectName, String pkgName, String launchScript) {
        ProcessMeta processMeta = new ProcessMeta(projectName, pkgName, launchScript);

        byte[] data = JsonUtils.getObjectData(processMeta);

        String pkgRuntimePath = Utils.constructString(Utils.getProjectRuntimeRootPath(projectName),
                Global.PATH_SEPARATOR, pkgName, Global.PATH_SEPARATOR, localAddr);

        String pidPath = Utils.constructString(slaveTargetPath, Global.PATH_SEPARATOR, pid);

        try {
            if (!Utils.checkNode(zk, pkgRuntimePath)) {
                // add package runtime path
                zk.create(pkgRuntimePath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            zk.create(pidPath, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            logger.error("add pid error: " + e.getMessage());
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            logger.error("add pid error: " + e.getMessage());
            throw new ZooKeeperException(e);
        }
    }

    public void deleteTargetPid(String pid, String projectName, String pkgName) {
        String pkgRuntimePath = Utils.constructString(Utils.getProjectRuntimeRootPath(projectName),
                Global.PATH_SEPARATOR, pkgName, Global.PATH_SEPARATOR, localAddr);

        String pidPath = Utils.constructString(slaveTargetPath, Global.PATH_SEPARATOR, pid);

        try {
            if (Utils.checkNode(zk, pkgRuntimePath)) {
                // delete package runtime path
                zk.delete(pkgRuntimePath, -1);
            }

            zk.delete(pidPath, -1);
        } catch (InterruptedException e) {
            logger.error("delete pid error: " + e.getMessage());
            throw new ZooKeeperException(e);
        } catch (KeeperException e) {
            logger.error("delete pid error: " + e.getMessage());
            throw new ZooKeeperException(e);
        }
    }

    public void processMessage(byte[] message) {
        MessageMeta meta = JsonUtils.getObject(message, MessageMeta.class);

        processMasterCmd(meta.getAction(), meta.getProject(), meta.getData());
    }

    public void processMasterCmd(String action, String projectName, byte[] data) {
        PackageMeta pkgMeta = JsonUtils.getObject(data, PackageMeta.class);
        logger.info("recv master cmd: " + projectName + " -> " + action + "@" + pkgMeta.getPkgName());

        try {
            if (Global.CMD_DEPLOY.equals(action)) {
                deployPackage(projectName, pkgMeta);
            } else if (Global.CMD_LAUNCH.equals(action)) {
                launchProcess(projectName, pkgMeta);
            } else if (Global.CMD_STOP.equals(action)) {
                stopProcess(projectName, pkgMeta);
            } else if (Global.CMD_RESTART.equals(action)) {
                restartProcess(projectName, pkgMeta);
            } else {
                throw new IllegalArgumentException("Illegal master cmd");
            }
        } catch (KeeperException e) {
            logger.error("process master cmd error: " + e.getMessage());
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            logger.error("process master cmd error: " + e.getMessage());
            throw new ZooKeeperException(e);
        } catch (ProcessLaunchedException e) {
            logger.error("process master cmd error: " + e.getMessage());
            throw new AutomateException(e);
        } catch (ProcessNotLaunchedException e) {
            logger.error("process master cmd error: " + e.getMessage());
            throw new AutomateException(e);
        }
    }

    private void restartProcess(String projectName, PackageMeta pkgMeta) throws InterruptedException, KeeperException {
        String projectBasePath = Utils.getProjectRootPath(projectName);

        String deployBasePath = Utils.getDeployFullPath(pkgMeta);
        String deployDirPath = Utils.constructString(deployBasePath, File.separator, pkgMeta.getPkgName(), "-", pkgMeta
                .getPkgVersion());

        logger.info("restart process: " + pkgMeta.getPkgName() + " -> " + projectBasePath);

        String launchScript = Utils.constructString(deployDirPath, File.separator, pkgMeta.getActivateCmd());

        processHolder.stopProcess(projectName, pkgMeta.getPkgName(), launchScript);

        Thread.sleep(Global.DEFAULT_WAIT_TIME * 1000);

        processHolder.addProcess(projectName, pkgMeta.getPkgName(), launchScript);

        logger.info("restart [DONE]");
    }

    private void stopProcess(String projectName, PackageMeta pkgMeta) throws InterruptedException, KeeperException,
            ProcessNotLaunchedException {
        String projectBasePath = Utils.getProjectRootPath(projectName);

        String deployBasePath = Utils.getDeployFullPath(pkgMeta);
        String deployDirPath = Utils.constructString(deployBasePath, File.separator, pkgMeta.getPkgName(), "-", pkgMeta
                .getPkgVersion());

        String pkgBasePath = Utils.constructString(projectBasePath, Global.PROJECT_RUNTIME, Global.PATH_SEPARATOR,
                pkgMeta.getPkgName(), Global.PATH_SEPARATOR, localAddr);

        if (!Utils.checkNode(zk, pkgBasePath)) {
            throw new ProcessNotLaunchedException("target process unlaunched: " + pkgMeta.getPkgName());
        }

        logger.info("stop process: " + pkgMeta.getPkgName() + " -> " + projectBasePath);

        String launchScript = Utils.constructString(deployDirPath, File.separator, pkgMeta.getActivateCmd());

        processHolder.stopProcess(projectName, pkgMeta.getPkgName(), launchScript);

        logger.info("stop [DONE]");
    }

    private void deployPackage(String projectName, PackageMeta pkgMeta) throws KeeperException, InterruptedException {
        String projectBasePath = Utils.getProjectRootPath(projectName);

        logger.info("deploy package: " + pkgMeta.getPkgName() + " -> " + projectBasePath);

        String srcPkgPath = Utils.getPackageFullPath(pkgMeta);
        String deployBasePath = Utils.getDeployFullPath(pkgMeta);
        String deployFilePath = Utils.constructString(deployBasePath, File.separator, pkgMeta.getPkgFullName());

        File checkPath = new File(deployBasePath);

        byte[] b = zk.getData(projectBasePath, null, null);
        MasterMeta masterMeta = JsonUtils.getObject(b, MasterMeta.class);

        String projectMasterAddr = masterMeta.getMasterAddr();

        // check deploy path
        if (!checkPath.exists()) {
            if (!checkPath.mkdirs()) {
                throw new ExecuteOsCmdException("create deploy path failed: " + checkPath.getAbsolutePath());
            }
        }

        logger.debug("dir path: " + checkPath.getAbsolutePath());

        // sync file from master
        String cmd = Utils.constructString("rsync -ae ssh ", projectMasterAddr, ":", srcPkgPath, " ", deployBasePath);

        logger.debug("sync: " + cmd);
        Utils.executeOsCmd(cmd);

        // check file
        checkPath = new File(deployFilePath);
        if (!checkPath.exists()) {
            throw new ExecuteOsCmdException("file sync failed: " + deployFilePath);
        }

        logger.info("\t file synced: " + deployFilePath);

        // extracts files
        int extIndex = deployFilePath.indexOf("tar.gz");
        if (extIndex > 0) {
            String deployDirPath = Utils.constructString(deployBasePath, File.separator, pkgMeta.getPkgName(), "-",
                    pkgMeta.getPkgVersion());
            cmd = Utils.constructString("tar -xzf ", deployFilePath, " -C ", deployBasePath);

            Utils.executeOsCmd(cmd);

            checkPath = new File(deployDirPath);
            if (!checkPath.exists()) {
                throw new ExecuteOsCmdException("extracts file failed: " + pkgMeta.getPkgFullName());
            }

            logger.info("\t file extracted: " + deployDirPath);
        }

        String pkgBasePath = Utils.constructString(projectBasePath, Global.PROJECT_DEPLOY, Global.PATH_SEPARATOR,
                pkgMeta.getPkgName(), Global.PATH_SEPARATOR, localAddr);
        zk.create(pkgBasePath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        logger.info("deploy [DONE]");
    }

    private void launchProcess(String projectName, PackageMeta pkgMeta) throws KeeperException, InterruptedException,
            ProcessLaunchedException {
        String projectBasePath = Utils.getProjectRootPath(projectName);

        String deployBasePath = Utils.getDeployFullPath(pkgMeta);
        String deployDirPath = Utils.constructString(deployBasePath, File.separator, pkgMeta.getPkgName(), "-", pkgMeta
                .getPkgVersion());

        String pkgBasePath = Utils.constructString(projectBasePath, Global.PROJECT_RUNTIME, Global.PATH_SEPARATOR,
                pkgMeta.getPkgName(), Global.PATH_SEPARATOR, localAddr);

        if (Utils.checkNode(zk, pkgBasePath)) {
            throw new ProcessLaunchedException("target process launched: " + pkgMeta.getPkgName());
        }

        logger.info("launch process: " + pkgMeta.getPkgName() + " -> " + projectBasePath);

        String launchScript = Utils.constructString(deployDirPath, File.separator, pkgMeta.getActivateCmd());

        File checkFile = new File(launchScript);
        if (!checkFile.exists()) {
            throw new ExecuteOsCmdException("launch script file not found: " + launchScript);
        }

        if (!checkFile.canExecute()) {
            checkFile.setExecutable(true);
        }

        processHolder.addProcess(projectName, pkgMeta.getPkgName(), launchScript);

        logger.info("launch [DONE]");
    }

    public static void main(String[] args) {
        String zkAddress = Global.DEFAULT_ZK_ADDRESS;

        if (args.length > 1) {
            System.err.println("USAGE: AutoSlave zkAddress");
            System.exit(2);
        } else {
            if (args.length > 0) {
                zkAddress = args[0];
            }
        }

        new AutoSlave(zkAddress).startSlave();
    }
}
