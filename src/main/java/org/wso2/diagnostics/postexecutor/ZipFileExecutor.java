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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.actionexecutor.ServerProcess;
import org.wso2.diagnostics.utils.CommonUtils;
import org.wso2.diagnostics.utils.ConfigMapHolder;
import org.wso2.diagnostics.utils.Constants;
import org.wso2.diagnostics.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.wso2.diagnostics.utils.Constants.FTP_UPLOAD_ENABLED;
import static org.wso2.diagnostics.utils.Constants.SFTP_UPLOAD_ENABLED;

/**
 * Class used to zip the folder and upload the zip file to FTP/SFTP server if configured.
 */
public class ZipFileExecutor {

    private static final Object ARCHIVE_LOCK = new Object();
    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+)\\s*(B|KB|MB|GB|TB)?");

    private final Logger log = LogManager.getLogger(ZipFileExecutor.class);
    private final String logDirPath;
    private final int maxFiles;
    private final long maxTotalSize;

    public ZipFileExecutor() {
        String logDirRelativePath = ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.ZIP_EXECUTOR_OUTPUT_DIRECTORY).toString();
        logDirPath = System.getProperty(Constants.APP_HOME) + File.separator + logDirRelativePath + File.separator;
        maxFiles = Integer.parseInt(ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.ZIP_EXECUTOR_MAX_COUNT).toString());
        maxTotalSize = parseMaxTotalSize(ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.ZIP_EXECUTOR_MAX_TOTAL_SIZE));
        File logFolder = new File(logDirPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }
    }

    static long parseMaxTotalSize(Object value) {

        if (value == null) {
            return Long.MAX_VALUE;
        }
        Matcher matcher = SIZE_PATTERN.matcher(value.toString().trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid " + Constants.ZIP_EXECUTOR_MAX_TOTAL_SIZE + ": " + value);
        }
        String unit = matcher.group(2);
        int power = unit == null ? 0 : "BKMGT".indexOf(unit.charAt(0));
        try {
            return Math.multiplyExact(Long.parseLong(matcher.group(1)), 1L << (10 * power));
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Invalid " + Constants.ZIP_EXECUTOR_MAX_TOTAL_SIZE + ": " + value, e);
        }
    }

    /**
     * This method is used to zip the folder and upload the zip file to FTP/SFTP server if configured.
     *
     * @param path path of the folder to be zipped
     */
    public void execute(String path) {
        log.info("Zipping the folder at " + path);
        File folder = new File(path);
        try {
            String zipFileName = ServerProcess.getNodeId() + "_" + folder.getName() + ".zip";
            Path archive = Path.of(logDirPath, zipFileName);
            Path temporaryArchive = Files.createTempFile(archive.getParent(), "diagnostics-", ".tmp");
            try {
                zipFolder(path, temporaryArchive);
                synchronized (ARCHIVE_LOCK) {
                    Files.move(temporaryArchive, archive, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Diagnosis Dumped in :" + archive);
                    FileUtils.rotateFiles(archive.getParent().toFile(), maxFiles, maxTotalSize);
                    if (Files.isRegularFile(archive)) {
                        executeUploader(logDirPath, zipFileName);
                    }
                }
            } finally {
                Files.deleteIfExists(temporaryArchive);
            }
        } catch (Exception e) {
            log.error("Unable to zip the file at " + path, e);
        }
    }

    private void zipFolder(String srcFolder, Path destZipFile) throws Exception {

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destZipFile))) {
            addFolderToZip("", srcFolder, zip);
        }
    }

    private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
            throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip); // if current file is directory do zip folder.
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                zip.closeEntry();
            }
        }
    }

    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
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

    private void executeUploader(String logDirpath, String zipName) {

        boolean ftpUploadEnabled = CommonUtils.getBooleanValue(ConfigMapHolder.getInstance().getConfigMap().
                get(FTP_UPLOAD_ENABLED), false);
        if (ftpUploadEnabled) {
            FTPUploader ftpUploader = new FTPUploader();
            ftpUploader.uploadFile(logDirpath, zipName);
        }

        boolean sftpUploadEnabled = CommonUtils.getBooleanValue(ConfigMapHolder.getInstance().getConfigMap().
                get(SFTP_UPLOAD_ENABLED), false);
        if (sftpUploadEnabled) {
            SFTPUploader sftpUploader = new SFTPUploader();
            sftpUploader.uploadFile(logDirpath, zipName);
        }
    }

}
