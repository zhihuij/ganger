package com.netease.automate.meta;

/**
 * Meta of master info.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class MasterMeta {
    private String masterAddr;
    private String packageRoot;

    /**
     * Default constructor.
     */
    public MasterMeta() {
        // do nothing
    }

    public MasterMeta(String masterAddr, String packageRoot) {
        this.masterAddr = masterAddr;
        this.packageRoot = packageRoot;
    }

    /**
     * @return the masterAddr
     */
    public String getMasterAddr() {
        return masterAddr;
    }

    /**
     * @param masterAddr
     *            the masterAddr to set
     */
    public void setMasterAddr(String masterAddr) {
        this.masterAddr = masterAddr;
    }

    /**
     * @return the packageRoot
     */
    public String getPackageRoot() {
        return packageRoot;
    }

    /**
     * @param packageRoot
     *            the packageRoot to set
     */
    public void setPackageRoot(String packageRoot) {
        this.packageRoot = packageRoot;
    }

}
