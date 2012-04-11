package com.netease.automate.meta;

import com.netease.automate.utils.Utils;

/**
 * Config constant definitions.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class Config {
    public static final String FIELD_SEPERATOR = ",";
    
    public static final String PROJECT_FIELD_NAME = "project.name";
    public static final String PROJECT_FIELD_PACKAGES = "project.packages";
    public static final String PROJECT_FIELD_PACKAGE_ROOT = "project.packageRoot";

    public static final String PACKAGE_FIELD_PREFIX = "package.";

    public static final String PACKAGE_FIELD_VERSION = ".version";
    public static final String PACKAGE_FIELD_FILE_NAME = ".fileName";
    public static final String PACKAGE_FIELD_DEPLOY_PATH = ".deployPath";
    public static final String PACKAGE_FIELD_LAUNCH_CMD = ".launchCmd";
    public static final String PACKAGE_FIELD_TARGETS = ".targets";
    public static final String PACKAGE_FIELD_DEPS = ".deps";

    public static String getPackageVersionKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_VERSION);
    }

    public static String getPackageFileNameKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_FILE_NAME);
    }

    public static String getPackageDeployPathKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_DEPLOY_PATH);
    }

    public static String getPackageLaunchCmdKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_LAUNCH_CMD);
    }

    public static String getPackageTargetsKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_TARGETS);
    }

    public static String getPackageDepsKey(String packageName) {
        return Utils.constructString(PACKAGE_FIELD_PREFIX, packageName, PACKAGE_FIELD_DEPS);
    }
}
