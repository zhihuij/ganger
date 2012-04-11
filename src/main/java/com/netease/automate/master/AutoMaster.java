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

    private ZooKeeper zk = null;
    private String zkAddress;
    private String localAddr;

    private Reactor reactor = null;
    private EventWatcher eventWatcher = null;
    private ProcessEventHandler eventHandler = null;

    private ActionFactory actionFactory;

    private Map<String, ProjectMeta> projectMetaMap = new HashMap<String, ProjectMeta>();
    private Map<String, RootProcess> rootProcessMap = new HashMap<String, RootProcess>();

    private Set<String> eventPauseSet = Collections.synchronizedSet(new HashSet<String>());

    private MasterConsole console = null;

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
                List<String> slaves = zk.getChildren(slavesPath, slaveStatusWatcher);
                for (String addr : slaves) {
                    SlaveTarget slaveTarget = slaveTargetMap.get(addr);
                    if (slaveTarget == null) {
                        slaveTarget = new SlaveTarget(addr);
                        slaveTarget.setStatus(SlaveStatus.ONLINE);

                        slaveTargetMap.put(addr, slaveTarget);

                        zk.getData(Utils.getSlaveStatusPath(addr), slaveStatusWatcher, null);

                        LogUtils.logInfoLine(Utils.constructString("slave -> ", addr, " online"));
                    }
                }
            }
        } catch (KeeperException e) {
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            throw new ZooKeeperException(e);
        }

        console = new MasterConsole(this);
        if (consoleFlag) {
            console.start();
        }
    }

    public void doAction(String actionName, String projectName, List<String> packages) {
        if (actionName.equals(Global.CMD_LOAD)) {
            load(projectName, packages);
        } else {
            ProjectMeta projectMeta = projectMetaMap.get(projectName);
            RootProcess rootProcess = rootProcessMap.get(projectName);

            if (projectMeta == null || rootProcess == null) {
                try {
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

    public void doSingleAction(String actionName, ProjectMeta projectMeta, RootProcess rootProcess,
            List<String> packages) {
        Action actionObj = actionFactory.createAction(actionName, projectMeta);

        rootProcess.doSupervisedAction(actionObj, packages);
    }

    public void deploy(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        doSingleAction(Global.CMD_DEPLOY, projectMeta, rootProcess, packages);
    }

    public void launch(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_LAUNCH, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    public void update(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_STOP, projectMeta, rootProcess, packages);
        doSingleAction(Global.CMD_DEPLOY, projectMeta, rootProcess, packages);
        doSingleAction(Global.CMD_LAUNCH, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    public void restart(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_RESTART, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

    public void stop(ProjectMeta projectMeta, RootProcess rootProcess, List<String> packages) {
        eventPauseSet.add(projectMeta.getProjectName());

        doSingleAction(Global.CMD_STOP, projectMeta, rootProcess, packages);

        eventPauseSet.remove(projectMeta.getProjectName());
    }

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
        doSingleAction(Global.CMD_STATUS, projectMeta, rootProcess, packages);
    }

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

    private void writePackageInfo(ProjectMeta projectMeta) throws KeeperException, InterruptedException {
        String projectMetaPath = Utils.getProjectMetaRootPath(projectMeta.getProjectName());

        List<String> packageNameList = projectMeta.getTopoOrderPackageList();

        for (String pkgName : packageNameList) {
            PackageMeta packageMeta = projectMeta.getPackageMeta(pkgName);

            String packagePath = Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR, pkgName);
            byte[] packageInfo = JsonUtils.getObjectData(packageMeta);
            if (!Utils.checkNode(zk, packagePath)) {
                zk.create(packagePath, packageInfo, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zk.setData(packagePath, packageInfo, -1);
            }
        }
    }

    private ProjectMeta constrcutProjectMeta(String projectName) throws KeeperException, InterruptedException {
        ProjectMeta projectMeta = null;

        String packageRoot = null;
        String masterAddr = null;

        String projectPath = Utils.getProjectRootPath(projectName);
        byte[] b = zk.getData(projectPath, false, null);

        MasterMeta maserMeta = JsonUtils.getObject(b, MasterMeta.class);

        packageRoot = maserMeta.getPackageRoot();
        masterAddr = maserMeta.getMasterAddr();

        String projectMetaPath = Utils.getProjectMetaRootPath(projectName);

        List<String> packageNameList = zk.getChildren(projectMetaPath, false);

        projectMeta = new ProjectMeta(projectName, masterAddr, packageRoot, readPackageInfo(projectMetaPath,
                packageNameList));

        return projectMeta;
    }

    private List<PackageMeta> readPackageInfo(String packageRootPath, List<String> packageNameList)
            throws KeeperException, InterruptedException {
        List<PackageMeta> packageMetaList = new ArrayList<PackageMeta>();

        for (String pkgName : packageNameList) {
            String packagePath = Utils.constructString(packageRootPath, Global.PATH_SEPARATOR, pkgName);
            byte[] data = zk.getData(packagePath, false, null);
            PackageMeta packageMeta = JsonUtils.getObject(data, PackageMeta.class);

            packageMetaList.add(packageMeta);
        }

        return packageMetaList;
    }

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
