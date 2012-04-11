package com.netease.automate.exception;

public class IllegalConfigException extends AutomateException {
    private static final long serialVersionUID = -5860124235994022878L;

    public IllegalConfigException(String message) {
        super(message);
    }

    public IllegalConfigException(Throwable throwable) {
        super(throwable);
    }

    public IllegalConfigException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
