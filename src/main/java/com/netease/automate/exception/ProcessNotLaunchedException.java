package com.netease.automate.exception;

/**
 * Exception thrown when a process hasn't been launched.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessNotLaunchedException extends Exception {
    private static final long serialVersionUID = -6479488919150358827L;

    public ProcessNotLaunchedException(String message) {
        super(message);
    }
}
