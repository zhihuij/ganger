package com.netease.automate.master.entity;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;
import com.netease.automate.master.event.EventTypes;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProcessStatus;
import com.netease.automate.meta.ProjectMeta;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;
import com.netease.event.EventType;

/**
 * Target runtime process.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class RuntimeProcess extends AbstractProcess {
    private AbstractProcess parentProcess;
    private ProjectMeta projectMeta;
    private PackageMeta packageMeta;
    private String target;

    public RuntimeProcess(ProjectMeta projectMeta, PackageMeta packageMeta, String target) {
        this.projectMeta = projectMeta;
        this.packageMeta = packageMeta;
        this.target = target;

        processName = Utils.constructString(projectMeta.getProjectName(), Global.PATH_SEPARATOR, this.packageMeta
                .getPkgName(), Global.PATH_SEPARATOR, target);
    }

    @Override
    public void init(RootProcess rootProcess, AbstractProcess parent) {
        super.init(rootProcess, parent);

        parentProcess = parent;

        rootProcess.addProcess(parentProcess, this, Utils.constructString(packageMeta.getPkgName(),
                Global.PATH_SEPARATOR, target));
    }

    @Override
    protected void doSelfAction(Action action) throws AutomateException {
        action.doAction(this, projectMeta.getProjectName(), packageMeta, target);
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void processEvent(EventType event, AbstractProcess targetProcess) {
        if (EventTypes.PROCESS_STOPPED.equals(event)) {
            setStatus(ProcessStatus.STOPPED);
            LogUtils.logErrorLine(targetProcess.getProcessName() + " stopped");
            
            parentProcess.processEvent(EventTypes.CHILD_PROCESS_STOPPED, targetProcess);
        } else if (EventTypes.PROCESS_STARTED.equals(event)) {
            setStatus(ProcessStatus.STARTED);
            
            LogUtils.logErrorLine(targetProcess.getProcessName() + " started");
            parentProcess.processEvent(EventTypes.CHILD_PROCESS_STARTED, targetProcess);
        } else if (EventTypes.PROCESS_RESTARTING.equals(event)) {
            setStatus(ProcessStatus.RESTARTING);
            
            LogUtils.logErrorLine(targetProcess.getProcessName() + " restarting");
            parentProcess.processEvent(EventTypes.CHILD_PROCESS_RESTARTED, targetProcess);
        }else if (EventTypes.PROCESS_RESTARTED.equals(event)) {
            setStatus(ProcessStatus.RESTARTED);
            
            LogUtils.logErrorLine(targetProcess.getProcessName() + " restarted");
            parentProcess.processEvent(EventTypes.CHILD_PROCESS_RESTARTED, targetProcess);
        }
    }
}
