package com.netease.automate.test.utils;

import java.util.ArrayList;
import java.util.List;

import com.netease.automate.meta.PackageMeta;
import com.netease.automate.utils.JsonUtils;

import junit.framework.TestCase;

public class JsonUtilsTest extends TestCase {
    public void testMapping() {
        List<String> targetList = new ArrayList<String>();

        targetList.add("10.100.82.217");
        targetList.add("10.100.82.216");
        targetList.add("10.100.82.215");
        targetList.add("10.100.82.214");

        PackageMeta p1 = new PackageMeta("master", "1.0.0", "master-1.0.0.tar.gz", "",
                "/home/space/automate/deploy", "bin/mxmaster.sh");
        p1.setTargetList(targetList);

        System.out.println("target list: " + targetList);
        byte[] result = JsonUtils.getObjectData(p1);

        System.out.println(new String(result));

        PackageMeta p2 = JsonUtils.getObject(result, PackageMeta.class);

        List<String> targetList2 = p2.getTargetList();
        System.out.println("target list2: " + targetList2);

        for (int i = 0; i < targetList2.size(); i++) {
            assertEquals(targetList2.get(i), targetList.get(i));
        }

        assertEquals(p1.getPkgName(), p2.getPkgName());
        assertEquals(p1.getPkgFullName(), p2.getPkgFullName());
        assertEquals(p1.getPkgVersion(), p2.getPkgVersion());

        byte[] result2 = JsonUtils.getObjectData(p2);

        System.out.println(new String(result));

        assertEquals(new String(result), new String(result2));
    }
}
