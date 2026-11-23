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


package org.wso2.diagnostics.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticDiskSpaceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ConfigMapHolder config = ConfigMapHolder.getInstance();
    private final Map<String, Object> settings = new HashMap<>();
    private Map<String, Object> originalConfig;
    private String originalHome;

    @Before
    public void configureOutputDirectory() {
        originalConfig = config.getConfigMap();
        originalHome = System.getProperty(Constants.APP_HOME);
        System.setProperty(Constants.APP_HOME, temporaryFolder.getRoot().getAbsolutePath());
        settings.put(Constants.ZIP_EXECUTOR_OUTPUT_DIRECTORY, "archives");
        config.setConfigMap(settings);
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
    public void skipsDiskProbeWhenMinimumIsOmittedOrZero() {
        assertTrue(FileUtils.hasEnoughDiskSpaceForDump());
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, 0);
        assertTrue(FileUtils.hasEnoughDiskSpaceForDump());
        assertFalse(FileUtils.getDiagnosticOutputDirectory().exists());
    }

    @Test
    public void allowsDumpWhenOutputVolumeHasEnoughSpace() {
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, "1 B");
        assertTrue(FileUtils.hasEnoughDiskSpaceForDump());
        assertTrue(FileUtils.getDiagnosticOutputDirectory().isDirectory());
    }

    @Test
    public void skipsDumpWhenMinimumExceedsUsableSpace() {
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, Long.MAX_VALUE);
        assertFalse(FileUtils.hasEnoughDiskSpaceForDump());
    }

    @Test
    public void skipsDumpForInvalidOrOverflowingMinimum() {
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, "-1GB");
        assertFalse(FileUtils.hasEnoughDiskSpaceForDump());
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, Long.MAX_VALUE + "TB");
        assertFalse(FileUtils.hasEnoughDiskSpaceForDump());
    }

    @Test
    public void skipsDumpWhenOutputVolumeCannotBeChecked() throws Exception {
        File output = temporaryFolder.newFile("archives");
        settings.put(Constants.ZIP_EXECUTOR_MIN_FREE_DISK_SPACE, "1B");
        assertFalse(FileUtils.hasEnoughDiskSpaceForDump());
        assertTrue(output.isFile());
    }
}
