package com.netease.automate.test.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class PatternTest extends TestCase {
    public void testPattern() {
        String pattern = "/projects/(.+)/runtime(.+)";
        Pattern pathPattern = Pattern.compile(pattern);
        
        Matcher matcher = pathPattern.matcher("/projects/xmpp/runtime/root/master");
        if(matcher.matches()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
        }
    }
}
