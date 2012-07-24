package com.netease.automate.master.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;
import com.netease.automate.master.action.ActionOrder;
import com.netease.automate.meta.ActionStatus;
import com.netease.automate.meta.ProcessStatus;
import com.netease.automate.utils.LogUtils;
import com.netease.event.EventType;

/**
 * Abstract process.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public abstract class AbstractProcess implements Entity {
    protected String processName;
    protected RootProcess rootProcess;
    protected ProcessStatus processStatus = ProcessStatus.STOPPED;
    protected ActionStatus actionStatus = ActionStatus.NOT_STARTED;

    public void init(RootProcess rootProcess, AbstractProcess parent) {
        this.rootProcess = rootProcess;
    }

    public String getProcessName() {
        return processName;
    }

    public void doAction(Action action) throws AutomateException {
        if (this instanceof RuntimeProcess) {
            // runtime process, just do the action
            doSelfAction(action);

            actionStatus = ActionStatus.DONE;
        } else {
            // package supervisor
            if (actionStatus == ActionStatus.DONE) {
                LogUtils.logInfoLine("action has been done for: " + processName);
                // action has been done
                return;
            }

            List<AbstractProcess> subProcesses = rootProcess.getSubProcess(this);

            List<PackageSupervisor> pkgSupList = new ArrayList<PackageSupervisor>();
            List<RuntimeProcess> runtimeProcessList = new ArrayList<RuntimeProcess>();

            for (AbstractProcess process : subProcesses) {
                if (process instanceof PackageSupervisor) {
                    // get the sub-package supervisor of this package supervisor
                    pkgSupList.add((PackageSupervisor) process);
                } else {
                    // get the sub-runtime process of this package supervisor
                    runtimeProcessList.add((RuntimeProcess) process);
                }
            }

            ActionOrder order = action.getActionOrder();
            if (order == ActionOrder.PRE_ORDER) {
                if (this instanceof PackageSupervisor) {
                    // check dependence
                    List<String> packageDeps = ((PackageSupervisor) this).getPackageDeps();
                    for (String pkg : packageDeps) {
                        if (rootProcess.getProcessByName(pkg).getActionStatus() == ActionStatus.NOT_STARTED) {
                            // not all the dependent packages have done the action, wait until
                            // that is true

                            LogUtils.logInfoLine("dependent package has not done: " + processName + " -> " + pkg);
                            return;
                        }
                    }
                }

                doSelfAction(action);
                // do the action for runtime process
                for (RuntimeProcess p : runtimeProcessList) {
                    p.doAction(action);
                }

                // set the status flag
                actionStatus = ActionStatus.DONE;
            }

            // do the action for the packages who dependent on this package
            if (order == ActionOrder.POST_ORDER) {
                Collections.reverse(pkgSupList);
            }

            for (PackageSupervisor p : pkgSupList) {
                p.doAction(action);
            }

            if (order == ActionOrder.POST_ORDER) {
                // check dependence
                for (PackageSupervisor p : pkgSupList) {
                    if (p.getActionStatus() == ActionStatus.NOT_STARTED) {
                        // not all the packages that dependent on this package have been done
                        // the action, wait until that is true

                        LogUtils.logInfoLine("package has not done: " + processName + " <- " + p.getProcessName());
                        return;
                    }
                }

                doSelfAction(action);

                Collections.reverse(runtimeProcessList);
                for (RuntimeProcess p : runtimeProcessList) {
                    p.doAction(action);
                }

                actionStatus = ActionStatus.DONE;
            }
        }
    }

    public ProcessStatus getStatus() {
        return processStatus;
    }

    public void setStatus(ProcessStatus status) {
        this.processStatus = status;
    }

    public ActionStatus getActionStatus() {
        return this.actionStatus;
    }

    public void setActionStatus(ActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }

    /**
     * Process event.
     * 
     * @param event
     *            the event object
     * @param targetProcess
     *            the target process
     */
    public abstract void processEvent(EventType event, AbstractProcess targetProcess);

    /**
     * Get the target address.
     * 
     * @return the target address
     */
    public abstract String getTarget();

    /**
     * Do the action
     * 
     * @param action
     *            the action object
     * @throws AutomateException
     *             if there are error
     */
    protected abstract void doSelfAction(Action action) throws AutomateException;
}
