package com.netease.automate.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta for package info.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class PackageMeta {
    private String pkgName;
    private String pkgVersion;
    private String pkgFullName;
    private String pkgRoot;
    private String deployPath;
    private String activateCmd;

    private List<String> dependentPkgList = new ArrayList<String>();
    private List<String> targetList = new ArrayList<String>();

    /**
     * Default constructor.
     */
    public PackageMeta() {
        // do nothing
    }

    public PackageMeta(String pkgName, String pkgVersion, String pkgFullName, String pkgRoot, String deployPath,
            String activateCmd) {
        this.pkgName = pkgName;
        this.pkgVersion = pkgVersion;
        this.pkgFullName = pkgFullName;
        this.pkgRoot = pkgRoot;
        this.deployPath = deployPath;
        this.activateCmd = activateCmd;
    }

    /**
     * @return the packageName
     */
    public String getPkgName() {
        return pkgName;
    }

    /**
     * @return the packageVersion
     */
    public String getPkgVersion() {
        return pkgVersion;
    }

    /**
     * @return the packageFullName
     */
    public String getPkgFullName() {
        return pkgFullName;
    }

    /**
     * @return the pkgRoot
     */
    public String getPkgRoot() {
        return pkgRoot;
    }

    /**
     * @return the deployPath
     */
    public String getDeployPath() {
        return deployPath;
    }

    /**
     * @return the activateCmd
     */
    public String getActivateCmd() {
        return activateCmd;
    }

    /**
     * @param pkgName
     *            the pkgName to set
     */
    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    /**
     * @param pkgVersion
     *            the pkgVersion to set
     */
    public void setPkgVersion(String pkgVersion) {
        this.pkgVersion = pkgVersion;
    }

    /**
     * @param pkgFullName
     *            the pkgFullName to set
     */
    public void setPkgFullName(String pkgFullName) {
        this.pkgFullName = pkgFullName;
    }

    /**
     * @param pkgRoot
     *            the pkgRoot to set
     */
    public void setPkgRoot(String pkgRoot) {
        this.pkgRoot = pkgRoot;
    }

    /**
     * @param deployPath
     *            the deployPath to set
     */
    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    /**
     * @param activateCmd
     *            the activateCmd to set
     */
    public void setActivateCmd(String activateCmd) {
        this.activateCmd = activateCmd;
    }

    public void addDependentPkg(String pkgName) {
        dependentPkgList.add(pkgName);
    }

    /**
     * @return the dependentPkgList
     */
    public List<String> getDependentPkgList() {
        return dependentPkgList;
    }

    /**
     * @param dependentPkgList
     *            the dependentPkgList to set
     */
    public void setDependentPkgList(List<String> dependentPkgList) {
        this.dependentPkgList = dependentPkgList;
    }

    public void addTarget(String target) {
        targetList.add(target);
    }

    /**
     * @return the targetList
     */
    public List<String> getTargetList() {
        return targetList;
    }

    /**
     * @param targetList
     *            the targetList to set
     */
    public void setTargetList(List<String> targetList) {
        this.targetList = targetList;
    }
}
