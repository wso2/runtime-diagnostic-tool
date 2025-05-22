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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogWatcher extends Thread {

    private static final Logger log = LogManager.getLogger(LogWatcher.class);
    private static final long POST_ERROR_TIMEOUT_MILLIS = 5000; // 5 seconds default timeout
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("(INFO|ERROR|WARN|FATAL|DEBUG)");
    private static final int MAX_ERROR_CONTEXT_SIZE = 1000;

    /**
     * The processing state for log collection
     */
    private enum ProcessingState {
        NORMAL,             // Collecting pre-error context
        ERROR_COLLECTING,   // Collecting error and stack trace
        POST_ERROR_COLLECTING // Collecting post-error context
    }

    /**
     * The file which will be tailed.
     */
    private final File file;
    /**
     * The amount of time to wait for the file to be updated.
     */
    private final long delay;
    /**
     * The interpreter to notify of events when tailing.
     */
    private final Interpreter interpreter;
    /**
     * Maximum amount of logs stored before the Error Line
     */
    private final int maxPreErrorContextSize;
    /**
     * Maximum amount of logs stored after the Error Line
     */
    private final int maxPostErrorContextSize;

    /**
     * Creates a Tailer for the given file.
     *
     * @param filepath   the file to follow.
     * @param interpreter   the TailerListener to use.
     * @param delay      the delay between checks of the file for new content in seconds.
     * @param maxPreErrorContextSize    max log lines before the error
     * @param maxPostErrorContextSize   max log lines after the error
     */
    public LogWatcher(String filepath, Interpreter interpreter, double delay, int maxPreErrorContextSize, int maxPostErrorContextSize) {

        this.file = new File(filepath);
        this.delay = Math.round(delay * 1000);
        this.interpreter = interpreter;
        this.maxPreErrorContextSize = maxPreErrorContextSize;
        this.maxPostErrorContextSize = maxPostErrorContextSize;
    }
    public void run() {
        try {
            RandomAccessFile reader = null;
            long position = 0;
            LinkedList<String> contextQueue = new LinkedList<>();
            ProcessingState currentState = ProcessingState.NORMAL;
            String errorLine = "";
            long errorTimestamp = 0;
            int postErrorLogLevelCount = 0;

            while (reader == null) {
                try {
                    reader = new RandomAccessFile(file, "r");
                    log.info("Initiating LogWatcher for file: " + file.getPath());
                    position = file.length();
                    reader.seek(position);
                } catch (FileNotFoundException e) {
                    // ignoring this exception as the file may not be created yet
                    log.debug("Log file " + file.getPath() + " not found.");
                    Thread.sleep(delay);
                }
            }
            while (true) {
                long fileLength = file.length();

                if (fileLength < position) {
                    // File was rotated
                    log.info("Log file has been rotated. Reopening the file " + file.getPath());
                    // Reopen the reader after rotation
                    try {
                        // Ensure that the old file is closed
                        closeQuietly(reader);
                        reader = new RandomAccessFile(file, "r");
                        ProcessingResult result = readLines(reader, contextQueue, currentState,
                                errorLine, errorTimestamp, postErrorLogLevelCount);
                        currentState = result.state;
                        errorLine = result.errorLine;
                        errorTimestamp = result.errorTimestamp;
                        postErrorLogLevelCount = result.postErrorLogLevelCount;
                        position = 0;
                    } catch (FileNotFoundException e) {
                        log.error("Log file " + file.getPath() + " not found." , e);
                    }
                    continue;
                }
                // Check if the file has been updated
                if (fileLength > position) {
                    // Read new content and update state
                    ProcessingResult result = readLines(reader, contextQueue, currentState,
                            errorLine, errorTimestamp, postErrorLogLevelCount);
                    currentState = result.state;
                    errorLine = result.errorLine;
                    errorTimestamp = result.errorTimestamp;
                    postErrorLogLevelCount = result.postErrorLogLevelCount;
                    // Update the file length
                    position = fileLength;
                    // Move the file pointer to the end
                    reader.seek(fileLength);
                }
                // Check for timeout if we're in post-error collection state
                if (currentState == ProcessingState.POST_ERROR_COLLECTING &&
                        System.currentTimeMillis() - errorTimestamp > POST_ERROR_TIMEOUT_MILLIS) {

                    log.debug("Post-error timeout reached. Processing error context.");
                    interpreter.interpret(errorLine, contextQueue);

                    // Reset state
                    currentState = ProcessingState.NORMAL;
                    contextQueue.clear();
                    errorLine = "";
                    errorTimestamp = 0;
                    postErrorLogLevelCount = 0;
                }
                // Sleep for a short duration before checking for updates again
                Thread.sleep(delay);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while tailing the log file: " + file.getPath(), e);
        }
    }

    private void closeQuietly(Closeable closeable) {

        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            log.error("unable to close the file: " + file.getPath(), ioe);
        }
    }

    /**
     * Class to hold processing state results
     */
    private static class ProcessingResult {
        ProcessingState state;
        String errorLine;
        long errorTimestamp;
        int postErrorLogLevelCount;

        ProcessingResult(ProcessingState state, String errorLine, long errorTimestamp, int postErrorLogLevelCount) {
            this.state = state;
            this.errorLine = errorLine;
            this.errorTimestamp = errorTimestamp;
            this.postErrorLogLevelCount = postErrorLogLevelCount;
        }
    }

    private ProcessingResult readLines(RandomAccessFile reader, LinkedList<String> contextQueue,
                                       ProcessingState currentState, String errorLine,
                                       long errorTimestamp, int postErrorLogLevelCount) throws IOException {

        String line;

        while ((line = reader.readLine()) != null) {
            boolean isLogLevelLine = LOG_LEVEL_PATTERN.matcher(line).find();

            switch (currentState) {
                case NORMAL:
                    if (isLogLevelLine) {
                        // Manage pre-error context queue
                        if (contextQueue.size() >= maxPreErrorContextSize) {
                            contextQueue.removeFirst();
                        }
                        contextQueue.add(line);

                        // Check if it's an error line
                        if (line.contains("ERROR")) {
                            currentState = ProcessingState.ERROR_COLLECTING;
                            errorLine = line;
                            errorTimestamp = System.currentTimeMillis();
                        }
                    } else {
                        // Non-log level line in normal state, likely a stacktrace
                        contextQueue.add(line);
                    }
                    break;

                case ERROR_COLLECTING:
                    // Check if we've reached the maximum error context size
                    if (contextQueue.size() >= maxPreErrorContextSize + MAX_ERROR_CONTEXT_SIZE) {
                        log.warn("Maximum error context size reached (" + MAX_ERROR_CONTEXT_SIZE +
                                " lines). Switching to post-error collection to prevent memory issues.");
                        // Force transition to post-error collecting state
                        currentState = ProcessingState.POST_ERROR_COLLECTING;
                        postErrorLogLevelCount = 0;
                    } else {
                        contextQueue.add(line);
                    }

                    if (isLogLevelLine) {
                        // Found a new log level line - now start collecting post-error context
                        currentState = ProcessingState.POST_ERROR_COLLECTING;
                        postErrorLogLevelCount = 1; // Count this as first post-error log level line
                    }
                    break;


                case POST_ERROR_COLLECTING:
                    contextQueue.add(line);

                    if (isLogLevelLine) {
                        postErrorLogLevelCount++;

                        // Check if we've collected enough post-error log lines
                        if (postErrorLogLevelCount >= maxPostErrorContextSize) {
                            interpreter.interpret(errorLine, contextQueue);

                            // Reset state
                            currentState = ProcessingState.NORMAL;
                            contextQueue.clear();
                            errorLine = "";
                            errorTimestamp = 0;
                            postErrorLogLevelCount = 0;

                            // Add this new log level line as first in new context queue
                            contextQueue.add(line);

                            // Check if new line is an error (starting process again)
                            if (line.contains("ERROR")) {
                                currentState = ProcessingState.ERROR_COLLECTING;
                                errorLine = line;
                                errorTimestamp = System.currentTimeMillis();
                            }
                        }
                    }
                    break;
            }
        }

        return new ProcessingResult(currentState, errorLine, errorTimestamp, postErrorLogLevelCount);
    }
}