package com.netease.automate.meta;

/**
 * Global definition.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class Global {
    public static final String DEFAULT_ZK_ADDRESS = "10.100.82.217:2181";

    /**
     * Separator for zookeeper path.
     */
    public static final String PATH_SEPARATOR = "/";

    public static final String PROJECTS_ROOT = "/projects";
    public static final String PROJECT_META = "/meta";
    public static final String PROJECT_DEPLOY = "/deploy";
    public static final String PROJECT_RUNTIME = "/runtime";

    public static final String ARC_ROOT = "/arc";
    public static final String ARC_MASTER = "/master";
    public static final String ARC_SLAVE = "/slave";

    public static final String SLAVE_TARGET = "/target";
    public static final String SLAVE_STATUS = "/status";
    public static final String SLAVE_MSG_CHANNEL = "/channel";

    /**
     * Command for master console.
     */
    public static final String CMD_LOAD = "load";
    public static final String CMD_DEPLOY = "deploy";
    public static final String CMD_LAUNCH = "launch";
    public static final String CMD_UPDATE = "update";
    public static final String CMD_STOP = "stop";
    public static final String CMD_RESTART = "restart";
    /**
     * Command for monitor.
     */
    public static final String CMD_STATUS = "status";
    public static final String CMD_HELP = "help";

    public static final String QUEUE_REFIX = "message_";

    public static final String ROOT_PACKAGE_NAME = "root";
    
    public static final String PROCESS_RESTARTING = "restarting";
    public static final String PROCESS_RESTARTED = "restarted";

    /**
     * Default wait time(s) for process initialization.
     */
    public static final int DEFAULT_WAIT_TIME = 2;

    /**
     * Default process check time.
     */
    public static final int DEFAULT_CHECK_INTERNAL = 10;
}
