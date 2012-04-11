package com.netease.automate.master.action;

/**
 * Action order definition.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public enum ActionOrder {
    /**
     * Do the action first for the process itself.
     */
    PRE_ORDER,
    /**
     * Do the action first for the child of the process.
     */
    POST_ORDER
}
