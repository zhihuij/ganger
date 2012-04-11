package com.netease.automate.master.event;

import com.netease.event.EventContext;
import com.netease.event.EventType;

/**
 * Context of process event.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessEventContext implements EventContext {
    private EventType eventType;
    private String eventPath;

    public ProcessEventContext(EventType eventType, String eventPath) {
        this.eventType = eventType;
        this.eventPath = eventPath;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public String getEventPath() {
        return eventPath;
    }
}
