package com.netease.automate.test.master;

import java.util.List;

import com.netease.automate.meta.ProjectMeta;

import junit.framework.TestCase;

public class PackageDependenceTest extends TestCase {
    public void testDependence() {
        ProjectMeta projectMeta = new ProjectMeta("target/xmpp.properties", "10.100.82.217");

        List<String> packageList = projectMeta.getTopoOrderPackageList();
        for(String p: packageList) {
            System.out.print(p + " -> ");
        }

        System.out.println("");

        List<String> subPakcage = projectMeta.getChildrenPackage("root");
        System.out.println("sub of " + "root: ");
        for (String pkg : subPakcage) {
            System.out.println(pkg);
        }

        subPakcage = projectMeta.getChildrenPackage("master");
        System.out.println("sub of " + "master: ");
        for (String pkg : subPakcage) {
            System.out.println(pkg);
        }

        subPakcage = projectMeta.getChildrenPackage("openfire");
        System.out.println("sub of " + "openfire: ");
        for (String pkg : subPakcage) {
            System.out.println(pkg);
        }

        subPakcage = projectMeta.getChildrenPackage("proxy");
        System.out.println("sub of " + "proxy: ");
        for (String pkg : subPakcage) {
            System.out.println(pkg);
        }

        subPakcage = projectMeta.getChildrenPackage("robot");
        System.out.println("sub of " + "robot: ");
        for (String pkg : subPakcage) {
            System.out.println(pkg);
        }
    }
}
