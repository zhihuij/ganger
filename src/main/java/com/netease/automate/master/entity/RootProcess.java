package com.netease.automate.master.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.action.Action;
import com.netease.automate.meta.ActionStatus;
import com.netease.automate.meta.Global;
import com.netease.automate.meta.ProjectMeta;
import com.netease.automate.utils.Tree;
import com.netease.automate.utils.Utils;
import com.netease.event.EventType;

/**
 * Root process in the process tree.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class RootProcess extends AbstractProcess {
    private Tree<AbstractProcess> processTree = new Tree<AbstractProcess>();
    private Map<String, AbstractProcess> processNameMap = new HashMap<String, AbstractProcess>();

    public void addProcess(AbstractProcess parentProcess, AbstractProcess subProcess, String processName) {
        processTree.addNode(parentProcess, subProcess);

        processNameMap.put(processName, subProcess);
    }

    public List<AbstractProcess> getSubProcess(AbstractProcess process) {
        return processTree.getChildrenData(process);
    }

    private ProjectMeta projectMeta;

    public RootProcess(ProjectMeta projectMeta) {
        this.projectMeta = projectMeta;
        processName = Utils.constructString(projectMeta.getProjectName(), Global.PATH_SEPARATOR,
                Global.ROOT_PACKAGE_NAME);
    }

    public void init() {
        init(this, null);
    }

    /**
     * Clear action status.
     * 
     * @param packageName
     */
    public void clearAction(String packageName) {
        int i = 0;
        List<String> topoOrderPackageList = projectMeta.getTopoOrderPackageList();
        for (; i < topoOrderPackageList.size(); i++) {
            if (packageName.equals(topoOrderPackageList.get(i))) {
                break;
            }
        }

        for (; i < topoOrderPackageList.size(); i++) {
            String processName = topoOrderPackageList.get(i);
            AbstractProcess process = processNameMap.get(processName);

            process.setActionStatus(ActionStatus.NOT_STARTED);
            List<AbstractProcess> subProcesses = getSubProcess(process);
            for (AbstractProcess sub : subProcesses) {
                sub.setActionStatus(ActionStatus.NOT_STARTED);
            }
        }
    }

    /**
     * Get the target process by package name.
     * 
     * @param packageName
     *            the package name
     * @return the process
     */
    public AbstractProcess getProcessByName(String packageName) {
        return processNameMap.get(packageName);
    }

    /**
     * Before doing the action, mark the status.
     * 
     * @param action
     *            the action object
     * @param packages
     *            the target packages
     */
    public void doSupervisedAction(Action action, List<String> packages) {
        if (packages.size() == 0) {
            clearAction(Global.ROOT_PACKAGE_NAME);
            doAction(action);
        } else {
            for (String pkg : packages) {
                clearAction(pkg);

                AbstractProcess pkgSup = getProcessByName(pkg);
                if (pkgSup == null) {
                    throw new IllegalArgumentException("package not found: " + pkg);
                }
                pkgSup.doAction(action);
            }
        }
    }

    @Override
    public void init(RootProcess rootProcess, AbstractProcess parent) {
        // this object is root, ignore root and parent
        super.init(this, this);

        processTree.setRoot(this);
        processNameMap.put(Global.ROOT_PACKAGE_NAME, this);

        List<String> packageList = projectMeta.getTopoOrderPackageList();
        for (String pkg : packageList) {
            if (!pkg.equals(Global.ROOT_PACKAGE_NAME)) {
                // make it is not root package
                PackageSupervisor pkgSup = new PackageSupervisor(projectMeta, pkg);
                processNameMap.put(pkg, pkgSup);

                pkgSup.init(this, null); // package supervisor should ignore parent, beacause it may
                // have multiple parent.
            }
        }
    }

    @Override
    protected void doSelfAction(Action action) throws AutomateException {
        // do nothing
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void processEvent(EventType event, AbstractProcess targetProcess) {
        // do nothing
    }
}
