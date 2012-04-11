package com.netease.automate.exception;

/**
 * Wrapper for exception thrown by operation of zookeeper.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ZooKeeperException extends AutomateException {
    private static final long serialVersionUID = -1719722122773495464L;

    public ZooKeeperException(String message) {
        super(message);
    }

    public ZooKeeperException(Throwable throwable) {
        super(throwable);
    }

    public ZooKeeperException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
