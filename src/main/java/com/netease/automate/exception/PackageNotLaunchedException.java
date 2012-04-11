package com.netease.automate.exception;

/**
 * <p>
 * The difference between process and package is that, process represent a OS process that resident
 * in *ONE* target machine, and package represent all the process that resident in *ALL* target
 * machines that this package should deploy to.
 * </p>
 * 
 * PackageLaunchedException cann't happen, because you cann't make sure that whether all the
 * processes belongs to the package have been launched when you match a runtime package path. </br>
 * In this situation, ProcessLaunchedException will throw later if target process has been launched.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class PackageNotLaunchedException extends Exception {
    private static final long serialVersionUID = 6688276496199413377L;

    public PackageNotLaunchedException(String message) {
        super(message);
    }
}
