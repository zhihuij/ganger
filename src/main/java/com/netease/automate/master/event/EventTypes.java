package com.netease.automate.master.event;

import com.netease.event.EventType;
import com.netease.event.utils.EventUtils;

/**
 * Event type definitions.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class EventTypes {
    /**
     * Parent process has been started.
     */
    public static final EventType PARENT_PROCESS_STARTED = EventUtils.defineEvent("parent_process_started");
    /**
     * Parent process has been stopped.
     */
    public static final EventType PARENT_PROCESS_STOPPED = EventUtils.defineEvent("parent_process_stopped");
    /**
     * Parent process has been restarted.
     */
    public static final EventType PARENT_PROCESS_RESTARTED = EventUtils.defineEvent("parent_process_restarted");

    /**
     * Process has been started.
     */
    public static final EventType PROCESS_STARTED = EventUtils.defineEvent("process_started");
    /**
     * Process has been stopped.
     */
    public static final EventType PROCESS_STOPPED = EventUtils.defineEvent("process_stopped");
    /**
     * Process is restarting.
     */
    public static final EventType PROCESS_RESTARTING = EventUtils.defineEvent("process_restarting");
    
    /**
     * Process has been restarted.
     */
    public static final EventType PROCESS_RESTARTED = EventUtils.defineEvent("process_restarted");

    /**
     * Child process has been started.
     */
    public static final EventType CHILD_PROCESS_STARTED = EventUtils.defineEvent("child_process_started");
    /**
     * Child process has been stopped.
     */
    public static final EventType CHILD_PROCESS_STOPPED = EventUtils.defineEvent("child_process_stopped");
    /**
     * Child process has been restarted.
     */
    public static final EventType CHILD_PROCESS_RESTARTED = EventUtils.defineEvent("child_process_restarted");
}
