package com.netease.automate.test.master;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.netease.automate.meta.Global;
import com.netease.automate.meta.MasterMeta;
import com.netease.automate.meta.PackageMeta;
import com.netease.automate.meta.ProjectMeta;
import com.netease.automate.utils.JsonUtils;
import com.netease.automate.utils.Utils;

import junit.framework.TestCase;

public class ConfigMetaTest extends TestCase {
    private ZooKeeper zk;

    protected void setUp() throws Exception {
        zk = new ZooKeeper("10.100.82.217:2181", 3000, null);
    }

    protected void tearDown() throws Exception {
    }

    private ProjectMeta constrcutProjectMeta(String projectName) throws KeeperException, InterruptedException {
        ProjectMeta projectMeta = null;

        String packageRoot = null;
        String masterAddr = null;

        String projectPath = Utils.getProjectRootPath(projectName);
        byte[] b = zk.getData(projectPath, false, null);

        MasterMeta maserMeta = JsonUtils.getObject(b, MasterMeta.class);

        packageRoot = maserMeta.getPackageRoot();
        masterAddr = maserMeta.getMasterAddr();

        String projectMetaPath = Utils.getProjectMetaRootPath(projectName);

        List<String> packageNameList = zk.getChildren(projectMetaPath, false);

        projectMeta = new ProjectMeta(projectName, masterAddr, packageRoot, readPackageInfo(projectMetaPath,
                packageNameList));

        return projectMeta;
    }

    private List<PackageMeta> readPackageInfo(String packageRootPath, List<String> packageNameList)
            throws KeeperException, InterruptedException {
        List<PackageMeta> packageMetaList = new ArrayList<PackageMeta>();

        for (String pkgName : packageNameList) {
            String packagePath = Utils.constructString(packageRootPath, Global.PATH_SEPARATOR, pkgName);
            byte[] data = zk.getData(packagePath, false, null);
            PackageMeta packageMeta = JsonUtils.getObject(data, PackageMeta.class);

            packageMetaList.add(packageMeta);
        }

        return packageMetaList;
    }

    private ProjectMeta parseProjectConfig(String configFile, String localAddr) throws KeeperException,
            InterruptedException {
        ProjectMeta projectMeta = new ProjectMeta(configFile, localAddr);
        checkBasicNode(projectMeta);

        String projectPath = Utils.getProjectRootPath(projectMeta.getProjectName());
        MasterMeta masterMeta = new MasterMeta(projectMeta.getMasterAddr(), projectMeta.getPackageRoot());
        byte[] data = JsonUtils.getObjectData(masterMeta);
        zk.setData(projectPath, data, -1);

        writePackageInfo(projectMeta);

        return projectMeta;
    }

    private void checkBasicNode(ProjectMeta projectMeta) throws KeeperException, InterruptedException {
        // check project node
        Utils.checkAndCreateNode(zk, Global.PROJECTS_ROOT);
        Utils.checkAndCreateNode(zk, Utils.getProjectRootPath(projectMeta.getProjectName()));

        Utils.checkAndCreateNode(zk, Utils.getProjectMetaRootPath(projectMeta.getProjectName()));
    }

    private void writePackageInfo(ProjectMeta projectMeta) throws KeeperException, InterruptedException {
        String projectMetaPath = Utils.getProjectMetaRootPath(projectMeta.getProjectName());

        List<String> packageNameList = projectMeta.getTopoOrderPackageList();

        for (String pkgName : packageNameList) {
            PackageMeta packageMeta = projectMeta.getPackageMeta(pkgName);

            String packagePath = Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR, pkgName);
            byte[] packageInfo = JsonUtils.getObjectData(packageMeta);
            if (!Utils.checkNode(zk, packagePath)) {
                zk.create(packagePath, packageInfo, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zk.setData(packagePath, packageInfo, -1);
            }
        }
    }

    public void testWriteConfig() throws Exception {
        ProjectMeta projectMeta = parseProjectConfig("xmpp.properties", "10.100.82.217");

        String projectMetaPath = Utils.getProjectMetaRootPath(projectMeta.getProjectName());

        List<String> packageNameList = projectMeta.getTopoOrderPackageList();

        for (String pkgName : packageNameList) {
            PackageMeta packageMeta = projectMeta.getPackageMeta(pkgName);

            String packagePath = Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR, pkgName);
            byte[] packageInfo = JsonUtils.getObjectData(packageMeta);
            if (!Utils.checkNode(zk, packagePath)) {
                zk.create(packagePath, packageInfo, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zk.setData(packagePath, packageInfo, -1);
            }
        }

        for (String pkgName : packageNameList) {
            assertEquals(true, Utils.checkNode(zk, Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR,
                    pkgName)));
        }

        ProjectMeta projectMeta1 = constrcutProjectMeta(projectMeta.getProjectName());

        assertEquals(projectMeta.getProjectName(), projectMeta1.getProjectName());
        assertEquals(projectMeta.getMasterAddr(), projectMeta1.getMasterAddr());
        assertEquals(projectMeta.getPackageRoot(), projectMeta1.getPackageRoot());

        List<String> pkgList = projectMeta.getTopoOrderPackageList();
        List<String> pkgList1 = projectMeta1.getTopoOrderPackageList();
        assertEquals(pkgList.size(), pkgList1.size());

        for (String pkg : pkgList) {
            String str = new String(JsonUtils.getObjectData(projectMeta.getPackageMeta(pkg)));
            String str1 = new String(JsonUtils.getObjectData(projectMeta1.getPackageMeta(pkg)));
            
            System.out.println(str);

            assertEquals(str, str1);
        }

        for (String pkgName : packageNameList) {
            zk.delete(Utils.constructString(projectMetaPath, Global.PATH_SEPARATOR, pkgName), -1);
        }
    }
}
