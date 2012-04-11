package com.netease.automate.meta;

/**
 * Meta for target info.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class SlaveTarget {
    /**
     * Address of target.
     */
    private String addr;
    /**
     * Status of slave: online, offline.
     */
    private SlaveStatus status;

    /**
     * Default constructor.
     */
    public SlaveTarget() {
        // do nothing
    }

    public SlaveTarget(String addr) {
        this.addr = addr;
    }

    /**
     * @return the addr
     */
    public String getAddr() {
        return addr;
    }

    /**
     * @param addr
     *            the addr to set
     */
    public void setAddr(String addr) {
        this.addr = addr;
    }

    /**
     * @return the status
     */
    public SlaveStatus getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(SlaveStatus status) {
        this.status = status;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SlaveTarget)) {
            return false;
        }

        SlaveTarget t = (SlaveTarget) o;
        return t.getAddr().equals(this.addr);
    }
}
