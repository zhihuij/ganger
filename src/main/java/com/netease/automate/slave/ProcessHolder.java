package com.netease.automate.slave;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.netease.automate.exception.AutomateException;
import com.netease.automate.meta.Global;
import com.netease.automate.utils.Utils;

/**
 * Process holder for slave managed processes.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class ProcessHolder {
    private static Logger logger = Logger.getLogger(ProcessHolder.class);

    private ExecutorService executorService = null;
    private String launcher;
    private AutoSlave slave;
    private Map<String, ProcessCheckRunnable> processMap = new ConcurrentHashMap<String, ProcessCheckRunnable>();

    class ProcessStartupCallable implements Callable<String> {
        private String processName;
        private String cmd;

        private String pid = null;

        public ProcessStartupCallable(String processName, String launchScript) {
            this.processName = processName;
            String scriptDir = launchScript.substring(0, launchScript.lastIndexOf("/"));
            String processCmd = launchScript.substring(launchScript.lastIndexOf("/") + 1);

            this.cmd = Utils.constructString("cd ", scriptDir, " && ", launcher, " ./", processCmd, " ./", processName,
                    ".log");
        }

        @Override
        public String call() throws Exception {
            logger.info("process " + processName + " starting...");
            pid = Utils.executeOsCmdWithResult(cmd);
            logger.info("process " + processName + " started, PID = " + pid);

            return pid;
        }

    }

    class ProcessCheckRunnable implements Runnable {
        private String projectName;
        private String processName;
        private String launchScript;

        private volatile boolean stopped;
        private String pid = null;

        public ProcessCheckRunnable(String projectName, String processName, String launchScript, String pid) {
            this.projectName = projectName;
            this.processName = processName;
            this.launchScript = launchScript;
            this.pid = pid;
        }

        @Override
        public void run() {
            processMap.put(launchScript, this);

            // check process status, reboot if process shut down(exceptional)
            String checkCmd = getCheckCmd(pid);
            while (true) {
                if (stopped) {
                    break;
                }

                String count = Utils.executeOsCmdWithResult(checkCmd);
                if (!stopped && Integer.valueOf(count) == 0) {
                    logger.error("process " + processName + " quit abnormally");

                    stopped = true;
                    // just stop the process, wait master command to relaunch
                    instance.stopProcess(projectName, processName, launchScript);
                    break;
                }

                try {
                    Thread.sleep(Global.DEFAULT_CHECK_INTERNAL * 1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }

        public String stop() {
            if (!stopped) {
                String shellCmd = Utils.constructString("kill -9 ", pid);
                Utils.executeOsCmd(shellCmd);

                stopped = true;
                Thread.currentThread().interrupt();
            }

            logger.info("process " + processName + " stopped");

            return pid;
        }
    }

    private static ProcessHolder instance;

    public static ProcessHolder getInstance(String launcher, AutoSlave slave) {
        if (instance == null) {
            instance = new ProcessHolder(launcher, slave);
        }

        return instance;
    }

    private ProcessHolder(String launcher, AutoSlave slave) {
        this.launcher = launcher;
        this.slave = slave;

        executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 100, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    private String getCheckCmd(String pid) {
        return Utils.constructString("ps -p ", pid, " | grep ", pid, " | wc -l");
    }

    public void addProcess(String projectName, String processName, String launchScript) {
        Future<String> future = executorService.submit(new ProcessStartupCallable(processName, launchScript));
        String pid = null;
        try {
            pid = future.get();
        } catch (InterruptedException e) {
            throw new AutomateException(e);
        } catch (ExecutionException e) {
            throw new AutomateException(e);
        }

        try {
            Thread.sleep(Global.DEFAULT_WAIT_TIME * 1000);
        } catch (InterruptedException e) {
            // do nothing
        }

        executorService.execute(new ProcessCheckRunnable(projectName, processName, launchScript, pid));

        slave.addTargetPid(pid, projectName, processName, launchScript);
    }

    public void addLiveProcess(String projectName, String processName, String launchScript, String pid) {
        executorService.execute(new ProcessCheckRunnable(projectName, processName, launchScript, pid));
    }

    public void stopProcess(String projectName, String pkgName, String launchScript) {
        ProcessCheckRunnable processWrapper = processMap.get(launchScript);
        if (processWrapper != null) {
            String pid = processWrapper.stop();
            try {
                Thread.sleep(Global.DEFAULT_WAIT_TIME * 1000);
            } catch (InterruptedException e) {
                // do nothing
            }

            slave.deleteTargetPid(pid, projectName, pkgName);
        }

    }
}
