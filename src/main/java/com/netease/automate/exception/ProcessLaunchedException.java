package com.netease.automate.exception;

/**
 * Exception thrown when a process has been launched.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessLaunchedException extends Exception {
    private static final long serialVersionUID = 1723476836348967412L;

    public ProcessLaunchedException(String message) {
        super(message);
    }
}
