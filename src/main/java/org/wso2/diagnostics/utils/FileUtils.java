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
     * @throws Exception throw a exception
     */
    public static void zipFolder(String srcFolder, String destZipFile) {

        ZipOutputStream zip;
        FileOutputStream fileWriter;
        try {
            fileWriter = new FileOutputStream(destZipFile); // set file output stream
            zip = new ZipOutputStream(fileWriter);

            addFolderToZip("", srcFolder, zip);

            zip.flush();
            zip.close();
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

    /**
     * Method used to zip file.
     *
     * @param path    destination path of the file
     * @param srcFile source path of the file which need to zip
     * @param zip     ZipOutputStream
     * @throws Exception
     */
    private static void addFileToZip(String path, String srcFile, ZipOutputStream zip)
            throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip); // if current file is directory do zip folder.
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    /**
     * Mehtod used to zip folder.
     *
     * @param path      destination  path of the folder.
     * @param srcFolder source path of the folder.
     * @param zip       ZipOutputStream
     * @throws Exception throw exception to parent method
     */
    private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
            throws Exception {

        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
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
        Arrays.sort(files, Comparator.comparing(File::getName));
        int fileCount = files.length;
        if (fileCount >= maxFiles) {
            for (int i = 0; i < fileCount - maxFiles + 1; i++) {
                files[i].delete();
            }
        }
    }
}
