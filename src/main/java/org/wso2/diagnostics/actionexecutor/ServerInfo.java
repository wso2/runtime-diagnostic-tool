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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;
import org.wso2.diagnostics.utils.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ServerInfo implements ActionExecutor {

    private static final Logger log = LogManager.getLogger(ServerInfo.class);
    private Map<String, Object> configuration;

    public ServerInfo() {
        configuration = ConfigMapHolder.getInstance().getConfigMap();
    }

    /**
     * Method used to take server information.
     *
     * @param folderPath folder path of the dump folder
     */
    @Override
    public void execute(String folderPath) {

        if (new File(folderPath).exists()) {

            String filepath = folderPath + "/server-info.txt";
            try {
                FileWriter writer = new FileWriter(filepath);
                writer.write(getServerInfo());
                writer.close();
            } catch (IOException e) {
                log.error("Unable to do write server information to file.");
            }
        }

        copyDeploymentToml(folderPath);
        copyLogsFolder(folderPath);
        copyDiagnosticLog(folderPath);
        log.info("ServerInfo executed successfully.");
    }

    /**
     * Method used to get server information.
     *
     * @return server information
     */
    public String getServerInfo() {
        String serverInfo = "";
        serverInfo += "Server name: " + ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.SERVER_NAME) + "\n";
        serverInfo += "Version: " + ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.SERVER_VERSION)  + "\n";
        serverInfo += "Update Level: " + getServerUpdateInfo() + "\n";
        serverInfo += "Java Version: " + System.getProperty("java.version") + "\n";
        serverInfo += "Java Home: " + System.getProperty("java.home") + "\n";
        serverInfo += "OS Name: " + System.getProperty("os.name") + "\n";
        serverInfo += "OS Version: " + System.getProperty("os.version") + "\n";
        serverInfo += "OS Architecture: " + System.getProperty("os.arch") + "\n";
        serverInfo += "Diagnostic App Home: " + System.getProperty("app.home") + "\n";
        return serverInfo;
    }

    private void copyDeploymentToml(String folderpath) {
        String deploymentToml = System.getProperty(Constants.APP_HOME) + File.separator +
                configuration.get(Constants.DEPLOYMENT_TOML_PATH);
        File file = new File(deploymentToml);
        if (file.exists()) {
            FileUtils.copyFile(deploymentToml, folderpath + "/deployment.toml");
        }
    }

    private void copyLogsFolder(String folderpath) {
        String logsFolder = System.getProperty(Constants.APP_HOME) + File.separator +
                configuration.get(Constants.LOGS_DIRECTORY);
        File file = new File(logsFolder);
        if (file.exists()) {
            FileUtils.zipFolder(logsFolder, folderpath + "/logs.zip");
        }
    }

    private void copyDiagnosticLog(String folderpath) {
        String deploymentToml = System.getProperty(Constants.APP_HOME) + File.separator +
                configuration.get(Constants.DIAGNOSTIC_LOG_FILE_PATH);
        File file = new File(deploymentToml);
        if (file.exists()) {
            FileUtils.copyFile(deploymentToml, folderpath + "/diagnostics.log");
        }
    }

    /**
     * Parse config.json file and retrieve update level parameter
     */
    private String getServerUpdateInfo() {

        String configJson = System.getProperty(Constants.APP_HOME) + File.separator +
                configuration.get(Constants.UPDATES_CONFIG_PATH);
        if (log.isDebugEnabled()) {
            log.debug("Config JSON path: " + configJson);
        }
        File file = new File(configJson);
        if (file.exists()) {
            // parse config.json and get update level
            JsonObject configJsonObj = readJsonObject(configJson);
            if (configJsonObj != null) {
                // Check if the field exists
                if (!configJsonObj.has("update-level")) {
                    log.info("Field 'update-level' not found in the update JSON.");
                } else {
                    String updateLevel = configJsonObj.get("update-level").getAsString();
                    log.info("Update level: " + updateLevel);
                    return updateLevel;
                }
            }
        }
        return null;
    }

    private static JsonObject readJsonObject(String jsonFilePath) {

        try {
            // Parse the JSON file into a JsonObject
            JsonObject jsonObject = JsonParser.parseReader(new FileReader(jsonFilePath)).getAsJsonObject();
            // Get the value of the field as a String
            return jsonObject;
        } catch (IOException e) {
            log.info("Updates info file not found.");
            return null;
        }
    }
}
