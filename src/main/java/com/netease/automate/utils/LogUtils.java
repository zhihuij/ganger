package com.netease.automate.utils;

/**
 * Simple log utility for log message.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class LogUtils {
    private static final String INFO_PREFIX = "[INFO] ";
    private static final String ERROR_PREFIX = "[ERROR] ";

    public static void logInfo(String msg) {
        System.out.print(Utils.constructString(INFO_PREFIX, msg));
    }

    public static void logEnd(String msg) {
        System.out.println(msg);
    }

    public static void logInfoLine(String msg) {
        System.out.println(Utils.constructString(INFO_PREFIX, msg));
    }

    public static void logErrorLine(String msg) {
        System.out.println(Utils.constructString(ERROR_PREFIX, msg));
    }
}
