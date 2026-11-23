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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogWatcherTest {

    private static final String ERROR = "[2026-11-23 17:45:12,345] ERROR {example.Logger} - failure";
    private static final String FATAL = "[2026-11-23 17:45:13,345] FATAL {example.Logger} - fatal";
    private static final String WARN = "[2026-11-23 17:45:14,345]  WARN {example.Logger} - warning";
    private static final String INFO = "[2026-11-23 17:45:15,345]  INFO {example.Logger} - ERROR_CODE FATAL WARN";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final List<String> firstLines = new ArrayList<>();
    private final List<String> entries = new ArrayList<>();

    @Test
    public void defaultsToErrorAndFatalAndIgnoresSeverityInMessageText() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, null);
        Files.writeString(file, WARN + "\n" + ERROR + "\n" + INFO + "\n" + FATAL + "\n");
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            watcher.readLines(reader);
            watcher.finishEntry();
        }
        assertEquals(List.of(ERROR, FATAL), firstLines);
        assertEquals(List.of(ERROR + "\n", FATAL + "\n"), entries);
    }

    @Test
    public void usesConfiguredTriggerLevels() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, " warn , fatal ");
        Files.writeString(file, WARN + "\n" + ERROR + "\n" + FATAL + "\n");
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            watcher.readLines(reader);
            watcher.finishEntry();
        }
        assertEquals(List.of(WARN, FATAL), firstLines);
    }

    @Test
    public void retainsHeaderAndStackTraceAcrossPolls() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, null);
        Files.writeString(file, ERROR + "\njava.lang.IllegalStateException: ERROR in message\n");
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            watcher.readLines(reader);
            assertTrue(firstLines.isEmpty());
            Files.writeString(file, "\tat example.Main.run(Main.java:12)\n" + INFO + "\n",
                    StandardOpenOption.APPEND);
            watcher.readLines(reader);
        }
        assertEquals(List.of(ERROR), firstLines);
        assertEquals(List.of(ERROR + "\njava.lang.IllegalStateException: ERROR in message\n"
                + "\tat example.Main.run(Main.java:12)\n"), entries);
    }

    @Test
    public void waitsForAnIncompleteLineToBeWritten() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, null);
        Files.writeString(file, ERROR + "\n\tat example.");
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            watcher.readLines(reader);
            Files.writeString(file, "Main.run(Main.java:12)\r\n" + INFO + "\n", StandardOpenOption.APPEND);
            watcher.readLines(reader);
        }
        assertEquals(List.of(ERROR), firstLines);
        assertEquals(List.of(ERROR + "\n\tat example.Main.run(Main.java:12)\n"), entries);
    }

    private LogWatcher watcher(Path file, String levels) {
        return new LogWatcher(file.toString(), (firstLine, entry) -> {
            firstLines.add(firstLine);
            entries.add(entry);
        }, 0.1, levels);
    }

    @Test
    public void recognizesApiManagerHeadersWithTenantAndApplication() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, null);
        String prefixedError = "TID: [-1234] [api-gateway] " + ERROR;
        String prefixedFatal = "TID: [-1234] [] " + FATAL;
        Files.writeString(file, prefixedError + "\nTID: [-1234] [] " + INFO + "\n" + prefixedFatal + "\n");
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            watcher.readLines(reader);
            watcher.finishEntry();
        }
        assertEquals(List.of(prefixedError, prefixedFatal), firstLines);
        assertEquals(List.of(prefixedError + "\n", prefixedFatal + "\n"), entries);
    }

    @Test
    public void detectsTruncationAboveRewoundReadOffset() throws IOException {
        Path file = temporaryFolder.newFile().toPath();
        LogWatcher watcher = watcher(file, null);
        Files.writeString(file, ERROR + "\n" + "partial".repeat(40));
        String replacementEntry = FATAL + " in replacement log";
        try (RandomAccessFile original = new RandomAccessFile(file.toFile(), "r")) {
            long originalLength = Files.size(file);
            watcher.reopenIfTruncated(original, originalLength);
            watcher.readLines(original);
            long rewoundOffset = original.getFilePointer();
            Files.writeString(file, replacementEntry + "\n");
            long replacementLength = Files.size(file);
            assertTrue(rewoundOffset < replacementLength && replacementLength < originalLength);
            try (RandomAccessFile replacement = watcher.reopenIfTruncated(original, replacementLength)) {
                assertEquals(0, replacement.getFilePointer());
                watcher.readLines(replacement);
                watcher.finishEntry();
            }
        }
        assertEquals(List.of(ERROR, replacementEntry), firstLines);
    }
}
