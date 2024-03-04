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

package org.wso2.diagnostics.postexecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostExecutorTask extends TimerTask {

    private static final Logger log = LogManager.getLogger(PostExecutorTask.class);

    private final String logLine;
    private final String folderPath;

    public PostExecutorTask(String logLine, String folderPath) {

        this.logLine = logLine;
        this.folderPath = folderPath;
    }

    @Override
    public void run() {

        this.writeLogLine(logLine);
        this.executeZipFileExecutor();
        this.deleteFolder();
    }


    /**
     * This method is used to call LogLine Writer to write the log line.
     *
     * @param logLine error log line
     */
    private void writeLogLine(String logLine) {

        try {
            FileWriter writer = new FileWriter(folderPath + "/" + "log.txt");
            writer.write(logLine);
            writer.close();
        } catch (IOException e) {
            log.error("Error occurred while writing the log line to the file", e);
        }
    }

    /**
     * This method is used to call ZipFileExecutor to file the dump folder.
     */
    private void executeZipFileExecutor() {

        ZipFileExecutor zipFileExecutor = new ZipFileExecutor();
        zipFileExecutor.execute(folderPath);
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
}
