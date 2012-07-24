package com.netease.automate.master.entity;

import java.util.List;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;
import com.netease.automate.master.action.ActionFactory;
import com.netease.automate.master.event.EventTypes;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProjectMeta;
import com.netease.automate.utils.LogUtils;
import com.netease.automate.utils.Utils;
import com.netease.event.EventType;

/**
 * Package supervisor process.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class PackageSupervisor extends AbstractProcess {
    private ProjectMeta projectMeta;
    private PackageMeta packageMeta;

    public PackageSupervisor(ProjectMeta projectMeta, String packageName) {
        this.projectMeta = projectMeta;
        this.packageMeta = projectMeta.getPackageMeta(packageName);
        processName = Utils.constructString(projectMeta.getProjectName(), Global.PATH_SEPARATOR, this.packageMeta
                .getPkgName());
    }
    
    public List<String> getPackageDeps() {
        return packageMeta.getDependentPkgList();
    }

    @Override
    public void init(RootProcess rootProcess, AbstractProcess parent) {
        super.init(rootProcess, parent);

        List<String> depList = packageMeta.getDependentPkgList();
        for (String dep : depList) {
            AbstractProcess depProcess = rootProcess.getProcessByName(dep);
            rootProcess.addProcess(depProcess, this, packageMeta.getPkgName());
        }

        List<String> targetList = packageMeta.getTargetList();
        for (String target : targetList) {
            RuntimeProcess subProcess = new RuntimeProcess(projectMeta, packageMeta, target);
            subProcess.init(rootProcess, this); // runtime process should use parameter parent as its
            // parent
        }
    }

    @Override
    protected void doSelfAction(Action action) throws AutomateException {
        action.doAction(this, projectMeta.getProjectName(), packageMeta, null);
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void processEvent(EventType event, AbstractProcess targetProcess) {
        // TODO child or parent process action specification, what to do when child process or
        // parent process event(started, stopped, restarted) happened
        if (EventTypes.CHILD_PROCESS_STOPPED.equals(event)) {
            // TODO supervisor maybe need to check whether all the target process has stopped

            // notify all the subpackages which dependent on this package
            List<String> subPackageList = projectMeta.getChildrenPackage(packageMeta.getPkgName());
            for (String pkg : subPackageList) {
                PackageSupervisor subPackageSup = (PackageSupervisor) rootProcess.getProcessByName(pkg);
                subPackageSup.processEvent(EventTypes.PARENT_PROCESS_STOPPED, targetProcess);
            }

            // notify done, now relaunch it
            targetProcess.doAction(ActionFactory.getInstance().createAction(Global.CMD_LAUNCH, projectMeta));
        } else if (EventTypes.CHILD_PROCESS_STARTED.equals(event)) {

        } else if (EventTypes.CHILD_PROCESS_RESTARTED.equals(event)) {

        } else if (EventTypes.PARENT_PROCESS_STOPPED.equals(event)) {
            LogUtils.logErrorLine(processName + "\t -> DP " + targetProcess.getProcessName() + " stopped");
        } else if (EventTypes.PARENT_PROCESS_STARTED.equals(event)) {

        } else if (EventTypes.PARENT_PROCESS_RESTARTED.equals(event)) {

        }
    }
}
