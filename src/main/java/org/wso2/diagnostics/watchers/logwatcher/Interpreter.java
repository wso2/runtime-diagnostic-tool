/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 *   WSO2 LLC. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.diagnostics.watchers.logwatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.actionexecutor.ActionExecutor;
import org.wso2.diagnostics.postexecutor.PostExecutorTask;
import org.wso2.diagnostics.utils.Constants;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to interpret the log line.
 */
public class Interpreter {

    private final Logger log = LogManager.getLogger(Interpreter.class);
    private String folderPath; // Folder path of the TimeStamp Folder
    private final Hashtable<String, Integer> logPatternLastMatchedTime;
    private final Timer timer;

    private final Map<String, ActionExecutor> actionExecutorMap;
    private final Map<String, String[]> regexMap;
    private final Map<String, Integer> regexPatternReloadTime;

    public Interpreter(Map<String, ActionExecutor> actionExecutorMap, Map<String, String[]> regexMap, Map<String,
            Integer> regexPatternReloadTime) {

        createLogFolder();
        this.actionExecutorMap = actionExecutorMap;
        this.regexMap = regexMap;
        logPatternLastMatchedTime = new Hashtable<>();
        this.regexPatternReloadTime = regexPatternReloadTime;
        timer = new Timer();
    }

    public void interpret(String errorLine, String completeLog) {
        this.diagnoseError(errorLine, completeLog);
    }

    /**
     * Method used to diagnose the error.
     *
     * @param errorLine error line
     * @param completeLog complete log
     */
    private void diagnoseError(String errorLine, String completeLog) {
        this.createFolder();
        String regexPattern = findRegexPattern(errorLine);
        String[] executorsList = regexMap.get(regexPattern);
        if (executorsList != null && this.doAnalysis(executorsList, errorLine, regexPattern)) {
            try {
                timer.schedule(new PostExecutorTask(completeLog, folderPath), new Date(new Date().getTime() + 5000));
            } catch (Exception e) {
                log.error("Error while scheduling the post executor task", e);
            }
        } else {
            this.deleteFolder();
        }
    }

    private String findRegexPattern(String errorLine) {
        for (Map.Entry<String, String[]> entry : regexMap.entrySet()) {
            String key = entry.getKey();
            // check whether the error line matches with the regex
            if (errorLine.matches(key)) {
                return key;
            }
        }
        return null;
    }

    /**
     * This method is used to do analysis.
     * First get diagnosis list and invoke certain action executor
     *
     * @param executorList list of executors
     * @param logLine log line
     * @param regexPattern regex pattern of the log line
     */
    private boolean doAnalysis(String[] executorList, String logLine, String regexPattern) {
        long reloadTime = regexPatternReloadTime.get(regexPattern);
        if (checkRegexPatternReloadTime(logLine, regexPattern, reloadTime)) {
            log.info("Executing the action executors for the log line matching the regex pattern " + regexPattern);
            AtomicBoolean analysed = new AtomicBoolean(false);
            // execute in parallel in different threads
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String exec : executorList) {
                String executor = exec.trim();
                ActionExecutor actionExecutor = actionExecutorMap.get(executor);
                if (actionExecutor == null) {
                    log.error("Action executor " + executor + " is not available.");
                    continue;
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    actionExecutor.execute(this.folderPath);
                    analysed.set(true);
                }, executorService);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();
            return true;
        }
        return false;
    }

    /**
     * this method used to create the log folder.
     */
    private void createLogFolder() {

        folderPath = (System.getProperty(Constants.APP_HOME) + "/temp/"); // get log file path
        File logFolder = new File(folderPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }
    }

    private void deleteFolder() {

        File dumpFolder = new File(this.folderPath);
        if (dumpFolder.exists()) {
            String[] entries = dumpFolder.list();
            for (String entry : entries) {
                File currentFile = new File(dumpFolder.getPath(), entry);
                currentFile.delete();
            }
            dumpFolder.delete();
        }
    }

    /**
     * Create folder for dump.
     */
    public void createFolder() {

        folderPath = (System.getProperty(Constants.APP_HOME) + "/temp/"); // get log file path
        File logFolder = new File(folderPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }
        // folder name set as timestamp
        String folderName = new Timestamp(System.currentTimeMillis()).toString().replace(" ", "_");
        File dumpFolder = new File(folderPath + folderName);
        if (!dumpFolder.exists()) {
            try {
                if (dumpFolder.mkdir()) {
                    folderPath = folderPath + folderName; // create folder if not exists.
                }
            } catch (Exception e) {
                log.error("Error while creating folder for dump", e);
            }
        }
    }

    /**
     * Method used to calculate current error time form log line.
     *
     * @param timeStr timestamp from the log line.
     * @return calculated time in Integer.
     */
    private int calculateTime(String timeStr) {

        String[] timeArray = timeStr.split(":");
        int hour = Integer.parseInt(timeArray[0]);
        int minute = Integer.parseInt(timeArray[1]);
        int second = Integer.parseInt(timeArray[0].substring(0, 2));
        return (hour * 3600) + (minute * 60) + second;
    }

    private boolean checkRegexPatternReloadTime(String testLine, String regexPattern, long reloadTime) {

        String timeRegex = "\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d";
        //Grep the first line of the error line.
        String[] errorLine = testLine.split("\n");
        Pattern pattern = Pattern.compile(timeRegex);
        Matcher matcher = pattern.matcher(errorLine[0]);
        if (matcher.find()) {
            long errorTime = calculateTime(matcher.group(0));
            if (logPatternLastMatchedTime.containsKey(regexPattern)) {
                if ((errorTime - logPatternLastMatchedTime.get(regexPattern)) > reloadTime) {
                    logPatternLastMatchedTime.replace(regexPattern, (int) errorTime);
                } else {
                    return false;
                }
            } else {
                logPatternLastMatchedTime.put(regexPattern, (int) errorTime);
                return true;
            }
        }
        return true;
    }
}
