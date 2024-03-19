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

package org.wso2.diagnostics.watchers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.actionexecutor.ActionExecutor;
import org.wso2.diagnostics.actionexecutor.ActionExecutorFactory;
import org.wso2.diagnostics.postexecutor.ZipFileExecutor;
import org.wso2.diagnostics.utils.CommonUtils;
import org.wso2.diagnostics.utils.FileUtils;
import org.wso2.diagnostics.utils.JMXDataRetriever;

/**
 * It will check the memory usage and if it is consistently above the threshold,
 * it will execute the memory watcher actions.
 */
public class MemoryWatcher extends Thread {

    private static final Logger log = LogManager.getLogger(MemoryWatcher.class);

    private final int retryCount;
    private final double threshold;
    private int count = 0;
    private final String pid;
    private long lastCountUpdatedTime;

    public MemoryWatcher(String pid, double threshold, int retryCount) {
        this.pid = pid;
        this.threshold = threshold;
        this.retryCount = retryCount;
        this.lastCountUpdatedTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Memory watcher thread executing for pid: " + pid + ", threshold: " + threshold +
                    ", retry count: " + retryCount + ", count: " + count + ", last count updated time: " +
                    lastCountUpdatedTime + ", current time: " + System.currentTimeMillis());
        }
        double memoryUsage = JMXDataRetriever.getMemoryUsage(pid);
        log.debug("Memory usage: " + memoryUsage + "%");
        if (memoryUsage > threshold) {
            count++;
            lastCountUpdatedTime = System.currentTimeMillis();
            log.info("Heap usage is above threshold. Heap usage: " + memoryUsage + "%, Retry count: " + count);
        }
        if (count > retryCount) {
            log.debug("Memory usage is consistently above threshold. Executing memory watcher actions.");
            String tempFolderPath = FileUtils.createTimeStampFolder();
            String[] actionExecutors = CommonUtils.getActionExecutors("memory_watcher");
            if (actionExecutors != null) {
                for (String actionExecutor : actionExecutors) {
                    if (log.isDebugEnabled()) {
                        log.debug("Executing action executor: " + actionExecutor);
                    }
                    ActionExecutor executor = ActionExecutorFactory.getActionExecutor(actionExecutor);
                    if (executor != null) {
                        executor.execute(tempFolderPath);
                        if (log.isDebugEnabled()) {
                            log.debug("Action executor " + actionExecutor + " executed successfully.");
                        }
                    } else {
                        log.error("Action executor " + actionExecutor + " is not available.");
                    }
                }
            }
            ZipFileExecutor zipFileExecutor = new ZipFileExecutor();
            zipFileExecutor.execute(tempFolderPath);
            if (log.isDebugEnabled()) {
                log.debug("Zipping the folder " + tempFolderPath + " is successful.");
            }
            FileUtils.deleteFolder(tempFolderPath);
            if (log.isDebugEnabled()) {
                log.debug("Deleted the folder " + tempFolderPath + " successfully.");
            }
            count = 0;
            lastCountUpdatedTime = System.currentTimeMillis();
        }
        // reset the count after 1 hour
        if (System.currentTimeMillis() - lastCountUpdatedTime > 3600000) {
            count = 0;
        }
    }
}
