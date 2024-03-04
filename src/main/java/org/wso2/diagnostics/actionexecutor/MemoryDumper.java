/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org)
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
import org.wso2.diagnostics.utils.Constants;

import java.io.File;
import java.io.IOException;

/**
 * MemoryDumper class is used to do Heap dump for given process.
 * This class use Java Runtime environment and jmap command to do memory dump.
 */
public class MemoryDumper implements ActionExecutor {

    private static final Logger log = LogManager.getLogger(MemoryDumper.class);

    private final String serverProcess;

    public MemoryDumper() {

        this.serverProcess = ServerProcess.getProcessId();
    }

    /**
     * Method used to do memory dump with using Java Runtime Environment and jmap command.
     *
     * @param filepath file path of the dump folder
     */
    @Override
    public void execute(String filepath) {

        if (new File(filepath).exists()) { // check whether file exists before dumping.
            String filename = "/heap-dump.hprof ";
            String prefix = System.getenv("JAVA_HOME") + "/bin/jmap -dump:live,format=b,file=";
            String frame = prefix + filepath + filename + serverProcess;
            try {
                Runtime.getRuntime().exec(frame);
                // provide time to complete the memory dump process.
                Thread.sleep(1000);
                log.info("Memory dump execution is completed for " + serverProcess);
            } catch (IOException | InterruptedException e) {
                log.error("Unable to do Memory dump for " + serverProcess, e);
            }
        }
    }

    public void execute() {
        String folderPath = (System.getProperty(Constants.APP_HOME) +
                File.separator + "temp" + File.separator);
        File logFolder = new File(folderPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }
        this.execute(folderPath);
    }
}
