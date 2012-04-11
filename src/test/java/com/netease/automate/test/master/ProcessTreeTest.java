package com.netease.automate.test.master;

import java.util.ArrayList;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.master.AutoMaster;
import com.netease.automate.master.action.Action;
import com.netease.automate.master.action.ActionOrder;
import com.netease.automate.master.entity.AbstractProcess;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProjectMeta;

import junit.framework.TestCase;

public class ProcessTreeTest extends TestCase {

    class EchoAction implements Action {
        @Override
        public void doAction(AbstractProcess process, String projectName, PackageMeta packageMeta, String target)
                throws AutomateException {
            System.out.println(packageMeta.getPkgName() + " -> " + target);
        }

        @Override
        public ActionOrder getActionOrder() {
            return ActionOrder.POST_ORDER;
        }
    }

    public void testProcessTree() {
        AutoMaster master = new AutoMaster("10.100.82.217:2181");
        master.startMaster(false);
        master.load("xmpp.properties", new ArrayList<String>());
        
        ProjectMeta projectMeta = master.test("xmpp", new EchoAction());
        
        System.out.println();
        for(String pkg: projectMeta.getTopoOrderPackageList()) {
            System.out.print(pkg + " -> ");
        }
    }
}
