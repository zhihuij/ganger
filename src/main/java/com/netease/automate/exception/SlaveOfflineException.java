package com.netease.automate.exception;

/**
 * Slave offline exception.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class SlaveOfflineException extends RuntimeException {
    private static final long serialVersionUID = -2095935215707488548L;

    public SlaveOfflineException(String message) {
        super(message);
    }
}
