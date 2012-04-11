package com.netease.automate.exception;

/**
 * Base exception.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class AutomateException extends RuntimeException {
    private static final long serialVersionUID = 1653383908649064039L;

    public AutomateException(String message) {
        super(message);
    }

    public AutomateException(Throwable throwable) {
        super(throwable);
    }

    public AutomateException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
