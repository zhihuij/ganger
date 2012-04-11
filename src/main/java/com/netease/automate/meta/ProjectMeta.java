package com.netease.automate.meta;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.exception.IllegalConfigException;
import com.netease.automate.utils.Utils;

/**
 * Project meta information.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProjectMeta {
    private String projectName;
    private String packageRoot;
    private String masterAddr;

    /**
     * Package map, package name as the key, and PackageMeta object as value.
     */
    private Map<String, PackageMeta> pkgMap = new HashMap<String, PackageMeta>();

    /**
     * Package dependence map, package name as key, pakcage list which dependent on the key package
     * as value. <br>
     */
    private Map<String, List<String>> packageDependenceMap = new HashMap<String, List<String>>();

    /**
     * The topological order list a the package dependence, the whole dependence should be a
     * directed graph of no cycles.
     */
    private List<String> topoOrderPackageList = new ArrayList<String>();

    /**
     * Target list of deploy target.
     */
    private List<String> targetList = new ArrayList<String>();

    public ProjectMeta(String configFile, String masterAddr) {
        parseConfigFile(masterAddr, configFile);
    }

    public ProjectMeta(String projectName, String masterAddr, String packageRoot, List<PackageMeta> packageMetaList) {
        initProjectMeta(projectName, masterAddr, packageRoot, packageMetaList);
    }

    private void initProjectMeta(String projectName, String masterAddr, String packageRoot,
            List<PackageMeta> packageMetaList) {
        this.projectName = projectName;
        this.masterAddr = masterAddr;
        this.packageRoot = packageRoot;

        PackageMeta root = new PackageMeta(Global.ROOT_PACKAGE_NAME, "1.0.0", "root-1.0.0.tar.gz", this.packageRoot,
                Global.ROOT_PACKAGE_NAME, Global.ROOT_PACKAGE_NAME);

        pkgMap.put(root.getPkgName(), root);

        for (PackageMeta pkgMeta : packageMetaList) {
            pkgMap.put(pkgMeta.getPkgName(), pkgMeta);
        }

        DirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        graph.addVertex(Global.ROOT_PACKAGE_NAME);
        // init package dependent graph
        for (PackageMeta pkgMeta : packageMetaList) {
            graph.addVertex(pkgMeta.getPkgName());
        }
        for (PackageMeta pkgMeta : packageMetaList) {
            String pkgName = pkgMeta.getPkgName();
            List<String> dependentList = pkgMeta.getDependentPkgList();

            for (String dep : dependentList) {
                graph.addEdge(pkgName, dep);
            }
        }

        // detect cycles
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<String, DefaultEdge>(graph);
        if (cycleDetector.detectCycles()) {
            Set<String> cyclePackageSet = cycleDetector.findCycles();
            StringBuilder sb = new StringBuilder();
            for (String p : cyclePackageSet) {
                sb.append(p).append(", ");
            }

            throw new AutomateException("dependence cycle detected: " + sb.toString());
        }

        // init topological order list
        TopologicalOrderIterator<String, DefaultEdge> iter = new TopologicalOrderIterator<String, DefaultEdge>(graph);
        while (iter.hasNext()) {
            topoOrderPackageList.add(iter.next());
        }

        Collections.reverse(topoOrderPackageList);

        Map<String, Integer> topoOrderIndexMap = new HashMap<String, Integer>();
        for (int i = 0; i < topoOrderPackageList.size(); i++) {
            topoOrderIndexMap.put(topoOrderPackageList.get(i), i);
        }

        for (PackageMeta pkgMeta : packageMetaList) {
            String pkgName = pkgMeta.getPkgName();
            List<String> dependentList = pkgMeta.getDependentPkgList();
            
            // init pacakge dependences
            List<String> subDeps = packageDependenceMap.get(pkgName);
            if (subDeps == null) {
                subDeps = new LinkedList<String>();
                packageDependenceMap.put(pkgName, subDeps);
            }

            for (String dep : dependentList) {
                subDeps = packageDependenceMap.get(dep);
                if (subDeps == null) {
                    subDeps = new LinkedList<String>();
                    packageDependenceMap.put(dep, subDeps);
                }

                int i = 0;
                for (; i < subDeps.size(); i++) {
                    if (topoOrderIndexMap.get(pkgName) < topoOrderIndexMap.get(subDeps.get(i))) {
                        break;
                    }
                }

                subDeps.add(i, pkgName);
            }
        }

        targetList = parseTargetList();
    }

    private List<String> parseTargetList() {
        Set<String> targetSet = new HashSet<String>();

        for (String pkg : topoOrderPackageList) {
            PackageMeta pkgMeta = pkgMap.get(pkg);

            for (String addr : pkgMeta.getTargetList()) {
                targetSet.add(addr);
            }
        }

        return Arrays.asList(targetSet.toArray(new String[0]));
    }

    private void parseConfigFile(String masterAddr, String configFile) {
        Properties prop = new Properties();

        try {
            File file = new File(configFile);
            prop.load(new FileReader(file));
        } catch (IOException e) {
            throw new IllegalConfigException(e);
        }

        String projectName = Utils.getPropValue(prop, Config.PROJECT_FIELD_NAME);
        String packageRoot = Utils.getPropValue(prop, Config.PROJECT_FIELD_PACKAGE_ROOT);

        String packages = Utils.getPropValue(prop, Config.PROJECT_FIELD_PACKAGES);
        String[] packageList = packages.split(Config.FIELD_SEPERATOR);

        List<PackageMeta> packageMetaList = new ArrayList<PackageMeta>();

        for (String pkg : packageList) {
            pkg = pkg.trim();

            String pkgVersion = Utils.getPropValue(prop, Config.getPackageVersionKey(pkg));
            String pkgFileName = Utils.getPropValue(prop, Config.getPackageFileNameKey(pkg));
            String pkgDeployPath = Utils.getPropValue(prop, Config.getPackageDeployPathKey(pkg));
            String pkgLaunchCmd = Utils.getPropValue(prop, Config.getPackageLaunchCmdKey(pkg));
            String pkgTargets = Utils.getPropValue(prop, Config.getPackageTargetsKey(pkg));
            String pkgDeps = prop.getProperty(Config.getPackageDepsKey(pkg), null);

            PackageMeta pkgMeta = new PackageMeta(pkg, pkgVersion, pkgFileName, packageRoot, pkgDeployPath,
                    pkgLaunchCmd);

            if (pkgDeps == null) {
                pkgMeta.addDependentPkg(Global.ROOT_PACKAGE_NAME);
            } else {
                String[] depPkgArray = pkgDeps.split(",");
                for (String pkgName : depPkgArray) {
                    pkgName = pkgName.trim();
                    pkgMeta.addDependentPkg(pkgName);
                }
            }

            String[] targetAddrList = pkgTargets.split(Config.FIELD_SEPERATOR);
            for (String addr : targetAddrList) {
                addr = addr.trim();
                pkgMeta.addTarget(addr);
            }

            packageMetaList.add(pkgMeta);
        }

        initProjectMeta(projectName, masterAddr, packageRoot, packageMetaList);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getPackageRoot() {
        return packageRoot;
    }

    public String getMasterAddr() {
        return masterAddr;
    }

    public PackageMeta getPackageMeta(String pkgName) {
        PackageMeta meta = pkgMap.get(pkgName);

        if (meta == null) {
            throw new IllegalArgumentException("pakage not found: " + pkgName);
        }

        return meta;
    }

    public List<String> getTopoOrderPackageList() {
        return Collections.unmodifiableList(topoOrderPackageList);
    }

    public List<String> getTargetList() {
        return Collections.unmodifiableList(targetList);
    }

    public List<String> getChildrenPackage(String packageName) {
        List<String> pkgList = packageDependenceMap.get(packageName);
        if (pkgList == null) {
            throw new IllegalArgumentException("package not found: " + packageName);
        }

        return Collections.unmodifiableList(pkgList);
    }
}
