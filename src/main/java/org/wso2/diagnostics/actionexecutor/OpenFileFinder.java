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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Class used to execute lsof command.
 */
public class OpenFileFinder implements ActionExecutor {

    private static Logger log = LogManager.getLogger(OpenFileFinder.class);

    /**
     * This string is used to represent process id.
     */
    private String processId;

    /**
     * Creates MemoryDumper with process id.
     */
    public OpenFileFinder() {

        this.processId = ServerProcess.getProcessId();

    }

    /**
     * Execute lsof command and write the output to a file.
     *
     * @param filepath file path of dump folder
     */
    @Override
    public void execute(String filepath) {

        if (new File(filepath).exists()) { // check whether file exists before dumping.
            String frame = filepath + "/lsof-output.txt ";
            String command = "lsof -p " + this.processId;

            try {
                Process process = Runtime.getRuntime().exec(command);
                Scanner scanner = new Scanner(process.getInputStream(), "IBM850");
                scanner.useDelimiter("\\A");
                try {
                    FileWriter writer = new FileWriter(frame);
                    writer.write(scanner.next());
                    writer.close();
                } catch (IOException e) {
                    log.error("Unable to do write in file for OpenFileFinder executor");
                }
                scanner.close();
                log.info("OpenFileFinder executed successfully.");

            } catch (IOException e) {
                log.error("Unable to do lsof command");
            }
        }

    }

}
