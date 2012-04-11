package com.netease.automate.meta;

/**
 * Meta of message info.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class MessageMeta {
    private String action;
    private String project;
    private byte[] data;

    /**
     * Default constructor.
     */
    public MessageMeta() {
        // do nothing
    }

    public MessageMeta(String action, String project, byte[] data) {
        this.action = action;
        this.project = project;
        this.data = data;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * @param project
     *            the project to set
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }
}
