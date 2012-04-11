package com.netease.automate.meta;

/**
 * Meta for process info.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessMeta {
    private String projectName;
    private String processName;
    private String launchScript;

    /**
     * Default constructor.
     */
    public ProcessMeta() {
        // do nothing
    }

    public ProcessMeta(String projectName, String processName, String launchScript) {
        this.projectName = projectName;
        this.processName = processName;
        this.launchScript = launchScript;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName
     *            the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the processName
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * @param processName
     *            the processName to set
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * @return the launchScript
     */
    public String getLaunchScript() {
        return launchScript;
    }

    /**
     * @param launchScript
     *            the launchScript to set
     */
    public void setLaunchScript(String launchScript) {
        this.launchScript = launchScript;
    }

}
