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

package org.wso2.diagnostics;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.actionexecutor.ActionExecutor;
import org.wso2.diagnostics.actionexecutor.ActionExecutorFactory;
import org.wso2.diagnostics.actionexecutor.ServerInfo;
import org.wso2.diagnostics.actionexecutor.ServerProcess;
import org.wso2.diagnostics.watchers.logwatcher.Interpreter;
import org.wso2.diagnostics.watchers.trafficanalyzer.TrafficAnalyzerInitializer;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;
import org.wso2.diagnostics.utils.TomlParser;
import org.wso2.diagnostics.watchers.CPUWatcher;
import org.wso2.diagnostics.watchers.logwatcher.LogWatcher;
import org.wso2.diagnostics.watchers.MemoryWatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.wso2.diagnostics.utils.Constants.APP_HOME;
import static org.wso2.diagnostics.utils.Constants.CONFIG_FILE_PATH;
import static org.wso2.diagnostics.utils.Constants.CPU_WATCHER_ENABLED;
import static org.wso2.diagnostics.utils.Constants.CPU_WATCHER_RETRY_COUNT;
import static org.wso2.diagnostics.utils.Constants.CPU_WATCHER_INTERVAL;
import static org.wso2.diagnostics.utils.Constants.CPU_WATCHER_THRESHOLD;
import static org.wso2.diagnostics.utils.Constants.LOG_WATCHER_ENABLED;
import static org.wso2.diagnostics.utils.Constants.LOG_WATCHER_INTERVAL;
import static org.wso2.diagnostics.utils.Constants.MEMORY_WATCHER_ENABLED;
import static org.wso2.diagnostics.utils.Constants.MEMORY_WATCHER_INTERVAL;
import static org.wso2.diagnostics.utils.Constants.MEMORY_WATCHER_RETRY_COUNT;
import static org.wso2.diagnostics.utils.Constants.MEMORY_WATCHER_THRESHOLD;

/**
 * Diagnostic tool launcher.
 */
public class DiagnosticsApp {

    private static final Logger log = LogManager.getLogger(DiagnosticsApp.class);

    public static void main(String[] args) {

        Map<String, Object> configMap;
        try {
            // delay the start to allow read the carbon pid
            Thread.sleep(5000);
            Map<String, ActionExecutor> actionExecutorMap = new HashMap<>();
            Map<String, String[]> regexMap = new LinkedHashMap<>();
            Map<String, Integer> regexPatternReloadTime = new HashMap<>();
            configMap = readConfiguration(System.getProperty(APP_HOME) + CONFIG_FILE_PATH,
                    actionExecutorMap, regexMap, regexPatternReloadTime);
            printServerInfo();

            String appHome = System.getProperty(Constants.APP_HOME);
            if (!appHome.endsWith(File.separator)) {
                appHome = appHome + File.separator;
            }

            // create log watcher thread
            boolean logWatcherEnabled = Boolean.parseBoolean(configMap.get(LOG_WATCHER_ENABLED).toString());
            if (logWatcherEnabled) {
                double logWatcherInterval = Double.parseDouble(configMap.get(LOG_WATCHER_INTERVAL).toString());
                LogWatcher carbonLogTailor = new LogWatcher(appHome +
                        configMap.get(Constants.LOG_FILE_CONFIGURATION_FILE_PATH),
                        new Interpreter(actionExecutorMap, regexMap, regexPatternReloadTime), logWatcherInterval);
                log.info("Listening to : " + configMap.get(Constants.LOG_FILE_CONFIGURATION_FILE_PATH));
                carbonLogTailor.start();
            }

            // create cpu watcher thread
            boolean cpuWatcherEnabled = Boolean.parseBoolean(configMap.get(CPU_WATCHER_ENABLED).toString());
            if (cpuWatcherEnabled) {
                int cpuWatcherInterval = Integer.parseInt(configMap.get(CPU_WATCHER_INTERVAL).toString());
                int cpuWatcherRetryCount = Integer.parseInt(configMap.get(CPU_WATCHER_RETRY_COUNT).toString());
                int cpuWatcherThreshold = Integer.parseInt(configMap.get(CPU_WATCHER_THRESHOLD).toString());

                log.info("Initiating CPUWatcher with interval: " + cpuWatcherInterval +
                        " retry count: " + cpuWatcherRetryCount + " threshold: " + cpuWatcherThreshold);
                ScheduledExecutorService cpuUsageExecutorService = Executors.newSingleThreadScheduledExecutor();
                CPUWatcher cpuWatcher = new CPUWatcher(
                        ServerProcess.getProcessId(), cpuWatcherThreshold, cpuWatcherRetryCount);
                cpuUsageExecutorService.scheduleAtFixedRate(cpuWatcher, 60, cpuWatcherInterval, SECONDS);
            }

            // create memory watcher thread
            boolean memoryWatcherEnabled = Boolean.parseBoolean(configMap.get(MEMORY_WATCHER_ENABLED).toString());
            if (memoryWatcherEnabled) {
                int memoryWatcherInterval = Integer.parseInt(configMap.get(MEMORY_WATCHER_INTERVAL).toString());
                int memoryWatcherRetryCount = Integer.parseInt(configMap.get(MEMORY_WATCHER_RETRY_COUNT).toString());
                int memoryWatcherThreshold = Integer.parseInt(configMap.get(MEMORY_WATCHER_THRESHOLD).toString());

                log.info("Initiating MemoryWatcher with interval: " + memoryWatcherInterval +
                        " retry count: " + memoryWatcherRetryCount + " threshold: " + memoryWatcherThreshold);
                ScheduledExecutorService memoryUsageExecutorService = Executors.newSingleThreadScheduledExecutor();
                MemoryWatcher memoryWatcher = new MemoryWatcher(
                        ServerProcess.getProcessId(), memoryWatcherThreshold, memoryWatcherRetryCount);
                memoryUsageExecutorService.scheduleAtFixedRate(memoryWatcher, 60, memoryWatcherInterval, SECONDS);
            }

            // start traffic analyzer
            TrafficAnalyzerInitializer.init();

        } catch (IOException | InterruptedException e) {
            log.error("Error on starting Diagnostics tool", e);
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> readConfiguration(
            String configFilePath, Map<String, ActionExecutor> actionExecutorMap, Map<String, String[]> regexTree,
            Map<String, Integer> regexPatternReloadTime) throws IOException {

        Map<String, Object> configMap = TomlParser.parse(configFilePath);
        ConfigMapHolder.getInstance().setConfigMap(configMap);
        ServerProcess.setProcessId((String) configMap.get(Constants.PROCESS_ID_PATH));

        ArrayList actionExecutorConfigs = (ArrayList) configMap.get(
                Constants.TOML_NAME_ACTION_EXECUTOR_CONFIGURATION);
        for (Object actionExecutorConfig : actionExecutorConfigs) {
            String executorName = (String) ((HashMap) actionExecutorConfig).get(Constants.TOML_NAME_EXECUTOR);
            ActionExecutor actionExecutor = ActionExecutorFactory.getActionExecutor(executorName);
            if (actionExecutor == null) {
                log.error("Action executor " + executorName + " is not available.");
                continue;
            }
            actionExecutorMap.put(executorName, actionExecutor);
        }

        ArrayList regexConfigs = (ArrayList) configMap.get("log_pattern");
        for (Object regexConfig : regexConfigs) {
            String regexName = (String) ((HashMap) regexConfig).get("regex");
            String executorList = (String) ((HashMap) regexConfig).get("executors");
            String reloadTime = (String) ((HashMap) regexConfig).get(
                    Constants.TOML_NAME_RELOAD_TIME);
            if (StringUtils.isNotEmpty(executorList)) {
                String[] executors = executorList.split(",");
                regexTree.put(regexName, executors);
                regexPatternReloadTime.put(regexName, Integer.parseInt(reloadTime));
            }
        }
        ServerProcess.writePID(System.getProperty(Constants.APP_HOME));
        return configMap;
    }

    private static void printServerInfo() {

        log.info("Starting WSO2 Diagnostics Tool");
        String serverInfo = new ServerInfo().getServerInfo();
        // print each string line in a new log line
        Arrays.stream(serverInfo.split("\\r?\\n")).forEach(log::info);
    }

}
