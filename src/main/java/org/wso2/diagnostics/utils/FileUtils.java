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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    private static final Logger log = LogManager.getLogger(FileUtils.class);

    public static void writeToFile(String filePath, String content) {
        // create the file if it does not exist
        try {
            Files.createFile(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error while creating file", e);
        }
        // write the content to the file
        try {
            Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Error while writing to file", e);
        }
    }

    public static void copyFile(String source, String destination) {
        try {
            Files.copy(Paths.get(source), Paths.get(destination));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method used to zip folder.
     *
     * @param srcFolder   file which needed to zip.
     * @param destZipFile destination path of zip folder.
     */
    public static void zipFolder(String srcFolder, String destZipFile) {
        zipFolder(srcFolder, destZipFile, file -> true);
    }

    /**
     * Zip the selected files, descending only into directories accepted by the filter.
     */
    public static void zipFolder(String srcFolder, String destZipFile, Predicate<File> includeEntry) {
        File source = new File(srcFolder);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destZipFile))) {
            addFolderToZip(source.getName(), source, zip, includeEntry);
        } catch (Exception e) {
            log.error("Error while zipping the folder", e);
        }
    }

    public static String createTimeStampFolder() {

        String folderPath = (System.getProperty(Constants.APP_HOME) + "/temp/");

        File logFolder = new File(folderPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }

        // folder name set as timestamp
        String folderName = new Timestamp(System.currentTimeMillis()).toString().replace(" ", "_");
        File dumpFolder = new File(folderPath + folderName);
        if (!dumpFolder.exists()) {
            try {
                if (dumpFolder.mkdir()) {
                    folderPath = folderPath + folderName; // create folder if not exists.
                }

            } catch (SecurityException se) {
                log.error("Error while creating folder for dump", se);
            }
        }
        return folderPath;
    }

    private static void addFolderToZip(String path, File folder, ZipOutputStream zip, Predicate<File> includeEntry)
            throws IOException {
        File[] children = folder.listFiles();
        if (children == null) {
            throw new IOException("Unable to list files in " + folder);
        }
        for (File child : children) {
            if (!includeEntry.test(child)) {
                continue;
            }
            String entryPath = path + "/" + child.getName();
            if (child.isDirectory()) {
                addFolderToZip(entryPath, child, zip, includeEntry);
            } else if (child.isFile()) {
                try (FileInputStream input = new FileInputStream(child)) {
                    zip.putNextEntry(new ZipEntry(entryPath));
                    input.transferTo(zip);
                    zip.closeEntry();
                }
            }
        }
    }

    public static void deleteFolder(String folderPath) {

        File dumpFolder = new File(folderPath);
        if (dumpFolder.exists()) {
            String[] entries = dumpFolder.list();
            if (entries == null) {
                return;
            }
            for (String entry : entries) {
                File currentFile = new File(dumpFolder.getPath(), entry);
                currentFile.delete();
            }
            dumpFolder.delete();
        }

    }

    public static void rotateFiles(File directory, int maxFiles) {

        File[] files = directory.listFiles();
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int fileCount = files.length;
        if (fileCount >= maxFiles) {
            for (int i = 0; i < fileCount - maxFiles; i++) {
                files[i].delete();
            }
        }
    }
}
