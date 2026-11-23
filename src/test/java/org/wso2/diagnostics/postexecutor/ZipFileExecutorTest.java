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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;
import org.wso2.diagnostics.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZipFileExecutorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesArchiveSizeLimits() {
        assertEquals(Long.MAX_VALUE, ZipFileExecutor.parseMaxTotalSize(null));
        assertEquals(1024L, ZipFileExecutor.parseMaxTotalSize(1024));
        assertEquals(2L * 1024 * 1024 * 1024, ZipFileExecutor.parseMaxTotalSize("2 GB"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverflowingArchiveSizeLimit() {
        ZipFileExecutor.parseMaxTotalSize("9223372036854775807TB");
    }

    @Test
    public void prunesOldestArchivesToMeetBothLimits() throws IOException {
        Path oldest = createArchive("oldest.zip", 3, 1000);
        Path middle = createArchive("middle.zip", 3, 2000);
        Path newest = createArchive("newest.zip", 3, 3000);
        Path other = createArchive("other.txt", 100, 0);

        FileUtils.rotateFiles(temporaryFolder.getRoot(), 3, 6);
        assertFalse(Files.exists(oldest));
        assertTrue(Files.exists(middle));
        assertTrue(Files.exists(newest));

        FileUtils.rotateFiles(temporaryFolder.getRoot(), 1, Long.MAX_VALUE);
        assertFalse(Files.exists(middle));
        assertTrue(Files.exists(newest));
        assertTrue(Files.exists(other));
    }

    @Test
    public void removesAnArchiveThatExceedsTheCapOnItsOwn() throws IOException {
        Path archive = createArchive("oversize.zip", 4, 1000);

        FileUtils.rotateFiles(temporaryFolder.getRoot(), 20, 3);

        assertFalse(Files.exists(archive));
    }

    @Test
    public void enforcesConfiguredLimitAfterCreatingAnArchive() throws IOException {
        Path input = temporaryFolder.newFolder("input").toPath();
        Files.writeString(input.resolve("log.txt"), "diagnostic log");
        ConfigMapHolder config = ConfigMapHolder.getInstance();
        Map<String, Object> originalConfig = config.getConfigMap();
        String originalHome = System.getProperty(Constants.APP_HOME);
        try {
            System.setProperty(Constants.APP_HOME, temporaryFolder.getRoot().getAbsolutePath());
            Path output = input.resolveSibling("archives");
            config.setConfigMap(Map.of(Constants.ZIP_EXECUTOR_OUTPUT_DIRECTORY, "archives",
                    Constants.ZIP_EXECUTOR_MAX_COUNT, "20"));
            new ZipFileExecutor().execute(input.toString());
            try (Stream<Path> archives = Files.list(output)) {
                assertEquals(1, archives.filter(path -> path.toString().endsWith(".zip")).count());
            }

            config.setConfigMap(Map.of(Constants.ZIP_EXECUTOR_OUTPUT_DIRECTORY, "archives",
                    Constants.ZIP_EXECUTOR_MAX_COUNT, "20", Constants.ZIP_EXECUTOR_MAX_TOTAL_SIZE, "1B"));
            new ZipFileExecutor().execute(input.toString());
            try (Stream<Path> archives = Files.list(output)) {
                assertEquals(0, archives.count());
            }
        } finally {
            config.setConfigMap(originalConfig);
            if (originalHome == null) {
                System.clearProperty(Constants.APP_HOME);
            } else {
                System.setProperty(Constants.APP_HOME, originalHome);
            }
        }
    }

    private Path createArchive(String name, int size, long modified) throws IOException {
        Path path = temporaryFolder.getRoot().toPath().resolve(name);
        Files.write(path, new byte[size]);
        Files.setLastModifiedTime(path, FileTime.fromMillis(modified));
        return path;
    }
}
