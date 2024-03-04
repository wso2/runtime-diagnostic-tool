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

package org.wso2.diagnostics.actionexecutor;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class used to represent the java process of wso2 server.
 *
 */
public class ServerProcess {

    private static final Logger log = LogManager.getLogger(ServerProcess.class);

    private static String nodeID;
    static String processId;

    static String processFilePath;

    /**
     * Getter method for processId.
     *
     * @return String processId
     */
    public static String getProcessId() {
        if ((processId == null)) {
            // read the process id from the wso2carbon.pid file
            setProcessId(processFilePath);
        }
        return processId;
    }

    /**
     * Setter method for processId.
     *
     * @param path wso2carbon.pid file path
     */
    public static void setProcessId(String path) {
        processFilePath = path;
        String appHome = System.getProperty(Constants.APP_HOME);
        if (!appHome.endsWith(File.separator)) {
            appHome = appHome + File.separator;
        }
        try (RandomAccessFile file = new RandomAccessFile(appHome + path, "r")) {
            // read the process id from the wso2carbon.pid file
            processId = file.readLine();
            log.info("Server Process ID: " + processId);
        } catch (IOException e) {
            log.error("wso2carbon.pid file not found.");
        }
    }

    public static String getNodeId() {
        if (nodeID == null) {
            String nodeId = System.getProperty("node.id");
            if (StringUtils.isEmpty(nodeId)) {
                Object id = ConfigMapHolder.getInstance().getConfigMap().get(Constants.NODE_ID);
                if (null != id) {
                    nodeId = id.toString();
                } else {
                    nodeId = processId;
                }
            }
            nodeID = nodeId;
        }
        return nodeID;
    }

    /**
     * Write the process ID of this process to the file.
     *
     * @param runtimePath wso2.runtime.path sys property value.
     */
    public static void writePID(String runtimePath) {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int indexOfAt = jvmName.indexOf('@');
        if (indexOfAt < 1) {
            log.warn("Cannot extract current process ID from JVM name '" + jvmName + "'.");
            return;
        }
        String pid = jvmName.substring(0, indexOfAt);

        Path runtimePidFile = Paths.get(runtimePath, "diagnostics.pid");
        try {
            Files.write(runtimePidFile, pid.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Cannot write process ID '" + pid + "' to '" + runtimePidFile.toString() + "' file.", e);
        }
    }

}
