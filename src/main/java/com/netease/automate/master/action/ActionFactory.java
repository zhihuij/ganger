package com.netease.automate.master.action;

import org.apache.zookeeper.ZooKeeper;

import com.netease.automate.meta.Global;
import com.netease.automate.meta.ProjectMeta;

/**
 * Action factory, have to bind to zookeeper instance.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ActionFactory {
    private static ActionFactory instance;

    public static ActionFactory bind(ZooKeeper zk) {
        if (instance != null) {
            throw new IllegalStateException("action factory has been binded");
        }

        instance = new ActionFactory(zk);
        return instance;
    }

    public static ActionFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("action factory not bind");
        }

        return instance;
    }

    private ZooKeeper zk;

    private ActionFactory(ZooKeeper zk) {
        this.zk = zk;
    }

    /**
     * Create the action object according the action name.
     * 
     * @param action
     *            action name
     * @param projectMeta
     *            the project meta this action binded to
     * @return the action object
     */
    public Action createAction(String action, ProjectMeta projectMeta) {
        if (Global.CMD_DEPLOY.equals(action)) {
            return new DeployAction(zk);
        } else if (Global.CMD_LAUNCH.equals(action)) {
            return new LaunchAction(zk);
        } else if (Global.CMD_RESTART.equals(action)) {
            return new RestartAction(zk);
        } else if (Global.CMD_STOP.equals(action)) {
            return new StopAction(zk);
        } else if (Global.CMD_STATUS.equals(action)) {
            return new StatusAction();
        } else {
            throw new IllegalArgumentException("Illegal action");
        }
    }
}
