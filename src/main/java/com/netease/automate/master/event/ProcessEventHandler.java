package com.netease.automate.master.event;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.master.entity.RootProcess;
import com.netease.event.EventContext;
import com.netease.event.EventHandler;
import com.netease.event.exception.EventException;

/**
 * Handler of process event.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessEventHandler implements EventHandler {
    private static final String PROCESS_MATCHER = "/projects/(.+)/runtime/(.+)";
    
    private Set<String> eventPauseSet;
    private Map<String, RootProcess> rootProcessMap = null;
    private Pattern pattern = null;

    public ProcessEventHandler(Set<String> eventPauseSet, Map<String, RootProcess> rootProcessMap) {
        this.eventPauseSet = eventPauseSet;
        this.rootProcessMap = rootProcessMap;
        this.pattern = Pattern.compile(PROCESS_MATCHER);
    }

    @Override
    public void handleEvent(EventContext eventCtx) throws EventException {
        ProcessEventContext ctx = (ProcessEventContext) eventCtx;
        String eventPath = ctx.getEventPath();

        Matcher matcher = pattern.matcher(eventPath);
        if (matcher.matches()) {
            String projectName = matcher.group(1);
            String processName = matcher.group(2);

            if (eventPauseSet.contains(projectName)) {
                return;
            }

            RootProcess rootProcess = rootProcessMap.get(projectName);
            if (rootProcess != null) {
                // call the process to handle the event
                AbstractProcess process = rootProcess.getProcessByName(processName);
                process.processEvent(eventCtx.getEventType(), process);
            }
        }
    }
}
