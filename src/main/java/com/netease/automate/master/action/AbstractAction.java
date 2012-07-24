package com.netease.automate.master.action;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.exception.ProcessLaunchedException;
import com.netease.automate.exception.ProcessNotLaunchedException;
import com.netease.automate.exception.ZooKeeperException;
import com.netease.automate.master.SlaveActionWatcher;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.master.entity.PackageSupervisor;
import com.netease.automate.master.task.Task;
import com.netease.automate.master.task.TaskFactory;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;

/**
 * Abstract action.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public abstract class AbstractAction implements Action {
    protected static Logger logger = Logger.getLogger(AbstractAction.class);

    protected ZooKeeper zk;

    public AbstractAction(ZooKeeper zk) {
        this.zk = zk;
    }

    public void doAction(AbstractProcess process, String projectName, PackageMeta packageMeta, String target)
            throws AutomateException {
        try {
            String actionRootPath = Utils.constructString(getActionRootPath(projectName), Global.PATH_SEPARATOR,
                    packageMeta.getPkgName());
            checkNode(actionRootPath);

            if (process instanceof PackageSupervisor || target == null) {
                // do nothing, before return, package node will create
                return;
            }

            String pkgTargetPath = Utils.constructString(actionRootPath, Global.PATH_SEPARATOR, target);

            try {
                checkTarget(pkgTargetPath);
            } catch (ProcessLaunchedException e) {
                // TODO process launched exception
                LogUtils.logErrorLine(Utils.constructString(projectName, Global.PATH_SEPARATOR, packageMeta
                        .getPkgName(), "\t -> ", target, " launched"));
                return;
            } catch (ProcessNotLaunchedException e) {
                // TODO process unlaunched exception
                LogUtils.logErrorLine(Utils.constructString(projectName, Global.PATH_SEPARATOR, packageMeta
                        .getPkgName(), "\t -> ", target, " unlaunched"));
                return;
            }

            // before do action
            beforeDoAction(process, pkgTargetPath);

            Task task = TaskFactory.createTask(zk, projectName, getAction(), packageMeta, target, pkgTargetPath);

            LogUtils.logInfo(Utils.constructString(getAction(), ": \t", projectName, Global.PATH_SEPARATOR, packageMeta
                    .getPkgName(), "\t -> ", target, " ..."));

            Semaphore sep = new Semaphore(1);
            Watcher watcher = new SlaveActionWatcher(zk, task, sep);
            zk.getChildren(actionRootPath, watcher); // add watcher before do the task

            try {
                sep.acquire();
            } catch (InterruptedException e) {
                // do nothing
            }

            task.doTask();

            try {
                sep.acquire();
            } catch (InterruptedException e) {
                // do nothing
            }

            afterActionDone(process, pkgTargetPath);

            LogUtils.logEnd("\t[DONE]");
        } catch (KeeperException e) {
            logger.error(e.getMessage());
            throw new ZooKeeperException(e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            throw new ZooKeeperException(e);
        }
    }

    /**
     * Check the target package path.
     * 
     * @param pkgPath
     *            package path
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    protected abstract void checkNode(String pkgPath) throws KeeperException, InterruptedException;

    /**
     * Check target slave status.
     * 
     * @param targetPath
     *            target path
     * @throws ProcessLaunchedException
     *             if the target process has been launched
     * @throws ProcessNotLaunchedException
     *             if the target process has not been launched
     */
    protected abstract void checkTarget(String targetPath) throws ProcessLaunchedException, ProcessNotLaunchedException;

    /**
     * Get the action name.
     * 
     * @return the action name
     */
    protected abstract String getAction();

    /**
     * Get the root path of action
     * 
     * @param projectName
     *            the project name
     * @return the root path
     */
    protected abstract String getActionRootPath(String projectName);

    /**
     * Before action behavior.
     * 
     * @param process
     *            the target process
     * @param pkgTargetPath
     *            package target path
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    protected void beforeDoAction(AbstractProcess process, String pkgTargetPath) throws KeeperException,
            InterruptedException {
        // do nothing
    }

    /**
     * After action behavior.
     * 
     * @param process
     *            the target process
     * @param pkgTargetPath
     *            package target path
     * @throws KeeperException
     *             throws when write project meta to zookeeper
     * @throws InterruptedException
     *             if the zookeeper server transaction is interrupted
     */
    protected void afterActionDone(AbstractProcess process, String pkgTargetPath) throws KeeperException,
            InterruptedException {
        // do nothing
    }
}
