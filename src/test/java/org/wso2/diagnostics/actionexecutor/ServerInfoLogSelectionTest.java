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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;

public class ServerInfoLogSelectionTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ConfigMapHolder config = ConfigMapHolder.getInstance();
    private final Map<String, Object> settings = new HashMap<>();
    private Map<String, Object> originalConfig;
    private String originalHome;
    private Path dumpDirectory;

    @Before
    public void setUp() throws IOException {
        originalConfig = config.getConfigMap();
        originalHome = System.getProperty(Constants.APP_HOME);
        System.setProperty(Constants.APP_HOME, temporaryFolder.getRoot().getAbsolutePath());
        settings.put(Constants.LOGS_DIRECTORY, "logs");
        config.setConfigMap(settings);

        Path logs = temporaryFolder.newFolder("logs").toPath();
        dumpDirectory = temporaryFolder.newFolder("dump").toPath();
        Files.writeString(logs.resolve("wso2carbon.log"), "active");
        Files.writeString(logs.resolve("wso2error.log"), "active");
        Files.writeString(logs.resolve("audit.log"), "active");
        Files.writeString(logs.resolve("wso2carbon-2026-11-20.log"), "rotated");
        Files.writeString(logs.resolve("notes.txt"), "other");
        Path history = Files.createDirectory(logs.resolve("history"));
        Files.writeString(history.resolve("wso2error.log"), "older copy");
        Files.writeString(history.resolve("heap.HPROF"), "heap");
    }

    @After
    public void restoreConfiguration() {
        config.setConfigMap(originalConfig);
        if (originalHome == null) {
            System.clearProperty(Constants.APP_HOME);
        } else {
            System.setProperty(Constants.APP_HOME, originalHome);
        }
    }

    @Test
    public void includesOnlyActiveLogsByDefault() throws IOException {
        assertEquals(Set.of("logs/wso2carbon.log", "logs/wso2error.log", "logs/audit.log"), collectLogs());
    }

    @Test
    public void includesActiveApiManagerLogsAndExcludesRotatedCopiesByDefault() throws IOException {
        Path logs = temporaryFolder.getRoot().toPath().resolve("logs");
        for (String name : Set.of("api", "wso2-apigw-errors", "wso2-apigw-service")) {
            Files.writeString(logs.resolve(name + ".log"), "active");
            Files.writeString(logs.resolve(name + "-2026-11-20.log"), "rotated");
        }
        assertEquals(Set.of("logs/wso2carbon.log", "logs/wso2error.log", "logs/audit.log",
                "logs/api.log", "logs/wso2-apigw-errors.log", "logs/wso2-apigw-service.log"), collectLogs());
    }

    @Test
    public void includesHeapDumpsIndependentlyOfRotatedLogs() throws IOException {
        settings.put(Constants.SERVER_INFO_EXCLUDE_HEAP_DUMPS, false);
        assertEquals(Set.of("logs/wso2carbon.log", "logs/wso2error.log", "logs/audit.log",
                "logs/history/heap.HPROF"), collectLogs());
    }

    @Test
    public void includesRotatedLogsWhileKeepingHeapDumpsExcluded() throws IOException {
        settings.put(Constants.SERVER_INFO_INCLUDE_ROTATED_LOGS, true);
        assertEquals(Set.of("logs/wso2carbon.log", "logs/wso2error.log", "logs/audit.log",
                "logs/wso2carbon-2026-11-20.log", "logs/history/wso2error.log", "logs/notes.txt"), collectLogs());
    }

    @Test
    public void includesAllLogsAndHeapDumpsWhenBothOverridesAreSet() throws IOException {
        settings.put(Constants.SERVER_INFO_INCLUDE_ROTATED_LOGS, true);
        settings.put(Constants.SERVER_INFO_EXCLUDE_HEAP_DUMPS, false);
        assertEquals(Set.of("logs/wso2carbon.log", "logs/wso2error.log", "logs/audit.log",
                "logs/wso2carbon-2026-11-20.log", "logs/history/wso2error.log", "logs/history/heap.HPROF",
                "logs/notes.txt"), collectLogs());
    }

    private Set<String> collectLogs() throws IOException {
        new ServerInfo().execute(dumpDirectory.toString());
        try (ZipFile archive = new ZipFile(dumpDirectory.resolve("logs.zip").toFile())) {
            return archive.stream().map(ZipEntry::getName).collect(Collectors.toSet());
        }
    }
}
