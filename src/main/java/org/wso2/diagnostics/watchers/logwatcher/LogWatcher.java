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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogWatcher extends Thread {

    private static final Logger log = LogManager.getLogger(LogWatcher.class);
    private static final Set<String> DEFAULT_TRIGGER_LEVELS = Set.of("ERROR", "FATAL");
    private static final int MAX_ENTRY_LENGTH = 1_000_000;
    // API Manager can prefix the timestamp with tenant ID and application name.
    private static final Pattern LOG_HEADER = Pattern.compile(
            "^(?:TID:\\s*\\[[^\\]]*\\]\\s+\\[[^\\]]*\\]\\s+)?"
                    + "\\[\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}[.,]\\d{3}\\]\\s+([A-Z]+)\\s+\\{");

    private final File file;
    private final long delay;
    private final BiConsumer<String, String> entryConsumer;
    private final Set<String> triggerLevels;
    private final StringBuilder entry = new StringBuilder();
    private String firstLine;
    private long lastObservedLength;

    public LogWatcher(String filepath, Interpreter interpreter, double delay) {
        this(filepath, interpreter, delay, null);
    }

    public LogWatcher(String filepath, Interpreter interpreter, double delay, String triggerLevels) {
        this(filepath, interpreter::interpret, delay, triggerLevels);
    }

    LogWatcher(String filepath, BiConsumer<String, String> entryConsumer, double delay, String triggerLevels) {
        this.file = new File(filepath);
        this.delay = Math.round(delay * 1000);
        this.entryConsumer = entryConsumer;
        this.triggerLevels = triggerLevels == null ? DEFAULT_TRIGGER_LEVELS
                : Arrays.stream(triggerLevels.split(","))
                        .map(level -> level.trim().toUpperCase(Locale.ROOT))
                        .filter(level -> !level.isEmpty())
                        .collect(Collectors.toSet());
    }

    @Override
    public void run() {
        RandomAccessFile reader = null;
        try {
            while (!isInterrupted()) {
                try {
                    if (reader == null) {
                        reader = new RandomAccessFile(file, "r");
                        lastObservedLength = reader.length();
                        reader.seek(lastObservedLength);
                        log.info("Initiating LogWatcher for file: {}", file);
                    }
                    long fileLength = file.length();
                    reader = reopenIfTruncated(reader, fileLength);
                    if (fileLength > reader.getFilePointer()) {
                        readLines(reader);
                    } else {
                        finishEntry();
                    }
                } catch (FileNotFoundException e) {
                    log.debug("Log file {} is unavailable.", file);
                }
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Error while tailing the log file: {}", file, e);
        } finally {
            closeReader(reader);
        }
    }

    RandomAccessFile reopenIfTruncated(RandomAccessFile reader, long fileLength) throws IOException {
        if (fileLength < lastObservedLength) {
            finishEntry();
            RandomAccessFile replacement = new RandomAccessFile(file, "r");
            closeReader(reader);
            reader = replacement;
            log.info("Log file has been rotated. Reopening {}", file);
        }
        lastObservedLength = fileLength;
        return reader;
    }

    private void closeReader(RandomAccessFile reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Unable to close the log file: {}", file, e);
            }
        }
    }

    void readLines(RandomAccessFile reader) throws IOException {
        while (true) {
            long lineStart = reader.getFilePointer();
            String line = reader.readLine();
            if (line == null) {
                return;
            }
            // readLine converts one byte per character and excludes the line terminator.
            if (reader.getFilePointer() - lineStart == line.length()) {
                reader.seek(lineStart);
                return;
            }

            Matcher header = LOG_HEADER.matcher(line);
            if (header.find()) {
                finishEntry();
                if (triggerLevels.contains(header.group(1))) {
                    firstLine = line;
                }
            }
            if (firstLine != null) {
                entry.append(line).append('\n');
                if (entry.length() > MAX_ENTRY_LENGTH) {
                    finishEntry();
                }
            }
        }
    }

    void finishEntry() {
        if (firstLine != null) {
            entryConsumer.accept(firstLine, entry.toString());
            firstLine = null;
            entry.setLength(0);
        }
    }
}
