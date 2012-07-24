package com.netease.automate.master;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.exception.ZooKeeperException;
import com.netease.automate.master.action.Action;
import com.netease.automate.master.action.ActionFactory;
import com.netease.automate.master.entity.RootProcess;
import com.netease.automate.master.event.EventTypes;
import com.netease.automate.master.event.EventWatcher;
import com.netease.automate.master.event.ProcessEventHandler;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.MasterMeta;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProjectMeta;
import com.netease.automate.meta.SlaveStatus;
import com.netease.automate.meta.SlaveTarget;
import com.netease.automate.utils.JsonUtils;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;
import com.netease.event.Reactor;
import com.netease.event.ReactorStrategy;

/**
 * Master of automate tools.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class AutoMaster {
    private static Logger logger = Logger.getLogger(AutoMaster.class);

    /**
     * Zookeeper instance.
     */
    private ZooKeeper zk = null;
    /**
     * Address of the zookeeper.
     */
    private String zkAddress;
    /**
     * Address of current master node.
     */
    private String localAddr;

    /**
     * Reactor for the event.
     */
    private Reactor reactor = null;
    /**
     * Event watcher for target process event.
     */
    private EventWatcher eventWatcher = null;
    /**
     * Handler of process event.
     */
    private ProcessEventHandler eventHandler = null;

    private ActionFactory actionFactory;

    /**
     * All known project metas.
     */
    private Map<String, ProjectMeta> projectMetaMap = new HashMap<String, ProjectMeta>();
    /**
     * Root process map for all the project, project name as the key.
     */
    private Map<String, RootProcess> rootProcessMap = new HashMap<String, RootProcess>();

    /**
     * Stop process the event while project is in pause state.
     */
    private Set<String> eventPauseSet = Collections.synchronizedSet(new HashSet<String>());

    /**
     * User console.
     */
    private MasterConsole console = null;

    /**
     * Status watcher for the slave.
     */
    private SlaveStatusWatcher slaveStatusWatcher = null;
    private Map<String, SlaveTarget> slaveTargetMap = new ConcurrentHashMap<String, SlaveTarget>();

    public AutoMaster(String... args) {
        if (args.length != 1) {
            System.err.println("USAGE: AutoMaster zkAddress");
            System.exit(2);
        } else {
            zkAddress = args[0];
        }
    }

    public void startMaster(boolean consoleFlag) {
        try {
            reactor = Reactor.getInstance(ReactorStrategy.MULTI_THREAD);
            eventWatcher = new EventWatcher(reactor);
            zk = new ZooKeeper(zkAddress, 3000, eventWatcher);
            eventWatcher.bind(zk);

            logger.debug("zooking bind: " + zkAddress);
        } catch (IOException e) {
            logger.error("Create zk error: " + e.getMessage());
            zk = null;
            return;
        }

        try {
            localAddr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e1) {
            // do nothing
        }

        actionFactory = ActionFactory.bind(zk);

        eventHandler = new ProcessEventHandler(eventPauseSet, rootProcessMap);
        reactor.registerEvent(eventHandler, EventTypes.PROCESS_STARTED, EventTypes.PROCESS_STOPPED,
                EventTypes.PROCESS_RESTARTED);

        slaveStatusWatcher = new SlaveStatusWatcher(slaveTargetMap, zk);
        try {
            String slavesPath = Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE);
            if (Utils.checkNode(zk, slavesPath)) {
                // load slave status from zookeeper
                List<String> slaves = zk.getChildren(slavesPath, slaveStatusWatcher);
                for (String addr : slaves) {
                    SlaveTarget slaveTarget = slaveTargetMap.get(addr);
                    String slaveStatusPath = Utils.getSlaveStatusPath(addr);
                    if (slaveTarget == null && Utils.checkNode(zk, slaveStatusPath)) {
                        slaveTarget = new SlaveTarget(addr);
                        slaveTarget.setStatus(SlaveStatus.ONLINE);

                        slaveTargetMap.put(addr, slaveTarget);

                        zk.getData(slaveStatusPath, slaveStatusWatcher, null);

                        LogUtils.logInfoLine(Utils.constructString("slave -> ", addr, " online"));
                    }
                }
            }
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }

        // start user console
        console = new MasterConsole(this);
        if (consoleFlag) {
            console.start();
        }
    }

    /**
     * Do the specific action for target project.
     * 
     * @param actionName
     *            name of the action
     * @param projectName
     *            name of the project
     * @param packages
     *            list of package names which the action will be processed
     */
    public void doAction(String actionName, String projectName, List<String> packages) {
        if (actionName.equals(Global.CMD_LOAD)) {
            load(projectName, packages);
        } else {
            ProjectMeta projectMeta = projectMetaMap.get(projectName);
            RootProcess rootProcess = rootProcessMap.get(projectName);

            if (projectMeta == null || rootProcess == null) {
                try {
                    // construct project meta from zookeeper meta.
                    projectMeta = constrcutProjectMeta(projectName);

                    rootProcess = initProject(projectMeta);
                } catch (KeeperException e) {
                    // do nothing
                } catch (InterruptedException e) {
                    // do nothing
                }

                if (projectMeta == null || rootProcess == null) {
                    throw new IllegalArgumentException("project -> " + projectName + " not found");
                }
            }

            if (actionName.equals(Global.CMD_DEPLOY)) {
                deploy(projectMeta, rootProcess, packages);
            } else if (actionName.equals(Global.CMD_LAUNCH)) {
                launch(projectMeta, rootProcess, packages);
            } else if (actionName.equals(Global.CMD_UPDATE)) {
                update(projectMeta, rootProcess, packages);
            } else if (actionName.equals(Global.CMD_RESTART)) {
                restart(projectMeta, rootProcess, packages);
            } else if (actionName.equals(Global.CMD_STOP)) {
                stop(projectMeta, rootProcess, packages);
            } else if (actionName.equals(Global.CMD_STATUS)) {
                status(projectMeta, rootProcess, packages);
            } else {
            }
        }
    }

    /**
     * Interface for test.
     * 
     * @param projectName
     *            name of the project
     * @param action
     *            action object, which can be construct to complete some test work
     * @return the meta info of the target project
     */
    public ProjectMeta test(String projectName, Action action) {
        RootProcess rootProcess = rootProcessMap.get(projectName);
        ProjectMeta projectMeta = projectMetaMap.get(projectName);

        rootProcess.doSupervisedAction(action, new ArrayList<String>());

        return projectMeta;
    }

    private RootProcess initProject(ProjectMeta projectMeta) {
        projectMetaMap.put(projectMeta.getProjectName(), projectMeta);

        RootProcess rootProcess = new RootProcess(projectMeta);
        rootProcess.init();

        rootProcessMap.put(projectMeta.getProjectName(), rootProcess);

        console.addProject(projectMeta.getProjectName());
        console.addPackage(projectMeta.getTopoOrderPackageList());

        return rootProcess;
    }

    /**
     * Load and construct project meta from project config file.
     * 
     * @param projectConfigFile
     *            file name of the proejct config
     * @param packages
     *            can't contain anything
     */
    public void load(String projectConfigFile, List<String> packages) {
        if (packages.size() != 0) {
            throw new IllegalArgumentException("illegal argument");
        }

        if (!projectConfigFile.endsWith("properties")) {
            throw new IllegalArgumentException("config should be a java properties file");
        }

        try {
            ProjectMeta projectMeta = parseProjectConfig(projectConfigFile, localAddr);

            initProject(projectMeta);

            for (String addr : projectMeta.getTargetList()) {
                SlaveTarget slaveTarget = slaveTargetMap.get(addr);
                if (slaveTarget == null) {
                    slaveTarget = new SlaveTarget(addr);

                    boolean status = Utils.checkNode(zk, Utils.getSlaveStatusPath(addr));
                    if (status) {
                        slaveTarget.setStatus(SlaveStatus.ONLINE);
                    } else {
                        slaveTarget.setStatus(SlaveStatus.OFFLINE);
                    }

                    slaveTargetMap.put(addr, slaveTarget);
                }
            }

            LogUtils.logInfoLine(Utils.constructString("project -> ", projectMeta.getProjectName(), " loaded"));
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }
    }

    /**
     * Do a single specific action.
     * 
     * @param actionName
     *            name of the action
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target packages
     */
    public void doSingleAction(String actionName, ProjectMeta projectMeta, RootProcess rootProcess,
            List<String> packages) {
        // construct action from factory
        Action actionObj = actionFactory.createAction(actionName, projectMeta);

        rootProcess.doSupervisedAction(actionObj, packages);
    }

    /**
     * Deploy packages of the project.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void deploy(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        doSingleAction(Global.CMD_DEPLOY, projectMeta, rootProcess, packages);
    }

    /**
     * Launch target project or target processes.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void launch(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_LAUNCH, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    /**
     * Update packages: stop current running processes of the packages, deploy the new packages, and
     * launch the processes of the packages.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void update(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_STOP, projectMeta, rootProcess, packages);
        doSingleAction(Global.CMD_DEPLOY, projectMeta, rootProcess, packages);
        doSingleAction(Global.CMD_LAUNCH, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    /**
     * Restart the target processes of the packages.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void restart(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_RESTART, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    /**
     * Stop the target processes of the packages.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void stop(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_STOP, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    /**
     * Get the status of the project.
     * 
     * @param projectMeta
     *            project meta
     * @param rootProcess
     *            root process of the project
     * @param packages
     *            target package names
     */
    public void status(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        // slave status
        LogUtils.logInfoLine(Utils.constructString("slave status of ", projectMeta.getProjectName(), ": "));
        for (String slave : projectMeta.getTargetList()) {
            SlaveTarget target = slaveTargetMap.get(slave);

            if (target == null || target.getStatus() == SlaveStatus.OFFLINE) {
                LogUtils.logInfoLine(Utils.constructString(slave, "\t offline"));
            } else {
                LogUtils.logInfoLine(Utils.constructString(slave, "\t online"));
            }
        }

        LogUtils.logInfoLine("");
        LogUtils.logInfoLine(Utils.constructString("process status of ", projectMeta.getProjectName(), ": "));
        // packages status
        doSingleAction(Global.CMD_STATUS, projectMeta, rootProcess, packages);
    }

    /**
     * Parse project config from config file
     * 
     * @param configFile
     *            filen name of the config file
     * @param localAddr
     *            address of the current master node
     * @return the project meta
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    private ProjectMeta parseProjectConfig(String configFile, String localAddr) throws KeeperException,
            InterruptedException {
        ProjectMeta projectMeta = new ProjectMeta(configFile, localAddr);
        checkBasicNode(projectMeta);

        String projectPath = Utils.getProjectRootPath(projectMeta.getProjectName());
        MasterMeta masterMeta = new MasterMeta(projectMeta.getMasterAddr(), projectMeta.getPackageRoot());
        byte[] data = JsonUtils.getObjectData(masterMeta);
        zk.setData(projectPath, data, -1);

        writePackageInfo(projectMeta);

        return projectMeta;
    }

    /**
     * Write packages info of project to zookeeper
     * 
     * @param projectMeta
     *            project meta
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    private void writePackageInfo(ProjectMeta projectMeta) throws KeeperException, InterruptedException {
        String projectMetaPath = Utils.getProjectMetaRootPath(projectMeta.getProjectName());

        List<String> packageNameList = projectMeta.getTopoOrderPackageList();

        for (String pkgName : packageNameList) {
            PackageMeta packageMeta = projectMeta.getPackageMeta(pkgName);

            String packagePath = Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR, pkgName);
            byte[] packageInfo = JsonUtils.getObjectData(packageMeta);
            // write package info
            if (!Utils.checkNode(zk, packagePath)) {
                zk.create(packagePath, packageInfo, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zk.setData(packagePath, packageInfo, -1);
            }
        }
    }

    /**
     * Read project meta from zookeeper
     * 
     * @param projectName
     *            name of the project
     * @return project meta
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    private ProjectMeta constrcutProjectMeta(String projectName) throws KeeperException, InterruptedException {
        ProjectMeta projectMeta = null;

        String packageRoot = null;
        String masterAddr = null;

        String projectPath = Utils.getProjectRootPath(projectName);
        byte[] b = zk.getData(projectPath, false, null);

        // read the master meta
        MasterMeta maserMeta = JsonUtils.getObject(b, MasterMeta.class);

        packageRoot = maserMeta.getPackageRoot();
        masterAddr = maserMeta.getMasterAddr();

        String projectMetaPath = Utils.getProjectMetaRootPath(projectName);

        List<String> packageNameList = zk.getChildren(projectMetaPath, false);

        // read package list
        projectMeta = new ProjectMeta(projectName, masterAddr, packageRoot, readPackageInfo(projectMetaPath,
                packageNameList));

        return projectMeta;
    }

    /**
     * Read package info from zookeeper.
     * 
     * @param packageRootPath
     *            the root path of the packages
     * @param packageNameList
     *            the list of package names
     * @return the list of package metas
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    private List<PackageMeta> readPackageInfo(String packageRootPath, List<String> packageNameList)
            throws KeeperException, InterruptedException {
        List<PackageMeta> packageMetaList = new ArrayList<PackageMeta>();

        for (String pkgName : packageNameList) {
            String packagePath = Utils.constructString(packageRootPath, Global.PATH_SEPARATOR, pkgName);
            // read package data and construct the meta object
            byte[] data = zk.getData(packagePath, false, null);
            PackageMeta packageMeta = JsonUtils.getObject(data, PackageMeta.class);

            packageMetaList.add(packageMeta);
        }

        return packageMetaList;
    }

    /**
     * The the basic data path of the project in zookeeper.
     * 
     * @param projectMeta
     *            project meta
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    private void checkBasicNode(ProjectMeta projectMeta) throws KeeperException, InterruptedException {
        // check arc node
        Utils.checkAndCreateNode(zk, Global.ARC_ROOT);
        Utils.checkAndCreateNode(zk, Utils.constructString(Global.ARC_ROOT, Global.ARC_MASTER));
        Utils.checkAndCreateNode(zk, Utils.constructString(Global.ARC_ROOT, Global.ARC_SLAVE));

        // check project node
        Utils.checkAndCreateNode(zk, Global.PROJECTS_ROOT);
        Utils.checkAndCreateNode(zk, Utils.getProjectRootPath(projectMeta.getProjectName()));

        Utils.checkAndCreateNode(zk, Utils.getProjectMetaRootPath(projectMeta.getProjectName()));
        Utils.checkAndCreateNode(zk, Utils.getProjectDeployRootPath(projectMeta.getProjectName()));
        Utils.checkAndCreateNode(zk, Utils.getProjectRuntimeRootPath(projectMeta.getProjectName()));
    }

    public static void main(String[] args) {
        new AutoMaster(args).startMaster(true);
    }
}
