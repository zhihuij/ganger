package com.netease.automate.exception;

/**
 * Exception thrown when execute OS command.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ExecuteOsCmdException extends RuntimeException {
    private static final long serialVersionUID = -8213352707365744141L;

    public ExecuteOsCmdException(String message) {
        super(message);
    }

    public ExecuteOsCmdException(Throwable throwable) {
        super(throwable);
    }

    public ExecuteOsCmdException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
