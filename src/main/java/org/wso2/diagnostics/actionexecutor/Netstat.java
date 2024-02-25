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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Class used to execute netstat command.
 */
public class Netstat implements ActionExecutor {

    private static Logger log = LogManager.getLogger(Netstat.class);
    private String command;

    public Netstat() {
        // read command from configmapholder
        Map configuration = ConfigMapHolder.getInstance().getConfigMap();
        ArrayList actionExecutorConfigs = (ArrayList) configuration.get(
                Constants.TOML_NAME_ACTION_EXECUTOR_CONFIGURATION);
        for (Object actionExecutorConfig : actionExecutorConfigs) {
            Map actionExecutorConfigMap = (Map) actionExecutorConfig;
            if (actionExecutorConfigMap.get("executor").equals("Netstat")) {
                command = (String) actionExecutorConfigMap.get("command");
            }
        }
    }

    /**
     * Execute netstat command and write the output to a file.
     *
     * @param filepath file path of dump folder
     */
    @Override
    public void execute(String filepath) {

        if (new File(filepath).exists()) { // check whether file exists before dumping.
            String filename = "/netstat-output.txt ";
            String frame = filepath + filename;
            try {
                if (command != null) {
                    Process process = Runtime.getRuntime().exec(command);
                    Scanner scanner = new Scanner(process.getInputStream(), "IBM850");
                    scanner.useDelimiter("\\A");
                    try {
                        FileWriter writer = new FileWriter(frame);
                        writer.write(scanner.next());
                        writer.close();
                    } catch (IOException e) {
                        log.error("Unable to do write in file in netstat");
                    }
                    scanner.close();
                    log.info("Netstat executed successfully");
                } else {
                    log.error("Unable to detect the OS");
                }

            } catch (IOException e) {
                log.error("Unable to do netstat");
            }
        }
    }
}
