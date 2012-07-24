package com.netease.automate.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.meta.Global;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.MultiCompletor;
import jline.SimpleCompletor;

/**
 * User console for master node.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class MasterConsole {
    /**
     * Console prompter.
     */
    private static final String CONSOLE_PROMPT = "ganger > ";

    /**
     * Command list.
     */
    private static final List<String> commandList = new ArrayList<String>();
    /**
     * Command map, command name as the key, description as the value.
     */
    private static final Map<String, String> commandMap = new HashMap<String, String>();

    static {
        // init command list
        commandList.add(Global.CMD_LOAD);
        commandList.add(Global.CMD_DEPLOY);
        commandList.add(Global.CMD_LAUNCH);
        commandList.add(Global.CMD_UPDATE);
        commandList.add(Global.CMD_RESTART);
        commandList.add(Global.CMD_STOP);
        commandList.add(Global.CMD_STATUS);
        commandList.add(Global.CMD_HELP);

        // init command map
        commandMap.put(Global.CMD_LOAD, "configFile");
        commandMap.put(Global.CMD_DEPLOY, "project [package]");
        commandMap.put(Global.CMD_LAUNCH, "project [package]");
        commandMap.put(Global.CMD_UPDATE, "project [package]");
        commandMap.put(Global.CMD_RESTART, "project [package]");
        commandMap.put(Global.CMD_STOP, "project [package]");
        commandMap.put(Global.CMD_STATUS, "project [package]");
        commandMap.put(Global.CMD_HELP, "");
    }

    /**
     * Command options.
     * 
     * @author jiaozhihui@corp.netease.com
     */
    class MasterCommandOptions {
        /**
         * Action name of the command.
         */
        private String action;
        /**
         * Project name.
         */
        private String project;
        /**
         * List of package name.
         */
        private List<String> packages;

        public String getAction() {
            return action;
        }

        public String getProject() {
            return project;
        }

        public List<String> getPackages() {
            return packages;
        }

        /**
         * Parse the command string.
         * 
         * @param cmdString
         *            command string
         * @return true for successfully parse, false otherwise
         */
        public boolean parseCommand(String cmdString) {
            String[] args = cmdString.split(" ");
            if (args.length < 2) {
                return false;
            }

            action = args[0];
            project = args[1];

            packages = Arrays.asList(args).subList(2, args.length);

            return true;
        }
    }

    private AutoMaster master = null;
    private ConsoleReader consoleReader = null;

    private SimpleCompletor projectNameCompletor = new SimpleCompletor("");
    private SimpleCompletor packageNameCompletor = new SimpleCompletor("");

    public MasterConsole(AutoMaster master) {
        this.master = master;
        try {
            consoleReader = new ConsoleReader();
        } catch (IOException e) {
            throw new AutomateException("start console fail");
        }
    }

    private void usage() {
        for (String cmd : commandList) {
            System.err.println("\t" + cmd + " " + commandMap.get(cmd));
        }
    }

    /**
     * Start the console.
     */
    public void start() {
        Completor commandCompletor = new SimpleCompletor(new String[] { Global.CMD_LOAD, Global.CMD_DEPLOY,
                Global.CMD_LAUNCH, Global.CMD_UPDATE, Global.CMD_RESTART, Global.CMD_STOP, Global.CMD_STATUS,
                Global.CMD_HELP });

        FileNameCompletor fileNameCompletor = new FileNameCompletor();
        Completor projectCompletor = new MultiCompletor(new Completor[] { fileNameCompletor, projectNameCompletor });

        List<Completor> completors = new LinkedList<Completor>();
        completors.add(commandCompletor);
        completors.add(projectCompletor);
        completors.add(packageNameCompletor);

        consoleReader.addCompletor(new ArgumentCompletor(completors));

        MasterCommandOptions commandOptions = new MasterCommandOptions();
        String line = null;

        try {
            System.out.println("\nWelcome to Ganger");
            while ((line = consoleReader.readLine(CONSOLE_PROMPT)) != null) {
                String trimLine = line.trim();
                if (trimLine.length() != 0) {
                    try {
                        if (!commandOptions.parseCommand(trimLine)
                                || !commandMap.containsKey(commandOptions.getAction())
                                || commandOptions.getAction().equals(Global.CMD_HELP)) {
                            usage();
                            continue;
                        }

                        // do the action according the command
                        master.doAction(commandOptions.getAction(), commandOptions.getProject(), commandOptions
                                .getPackages());
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new AutomateException("exception while read command");
        }
    }

    /**
     * Add project name to the command candidate list.
     * 
     * @param projectName
     *            project name
     */
    public void addProject(String projectName) {
        projectNameCompletor.addCandidateString(projectName);
    }

    /**
     * Add package names to the command candidate list.
     * 
     * @param packageList
     *            list of package names
     */
    public void addPackage(List<String> packageList) {
        for (String pkg : packageList) {
            packageNameCompletor.addCandidateString(pkg);
        }
    }
}
