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
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.wso2.diagnostics.utils.Constants.FTP_UPLOAD_ENABLED;
import static org.wso2.diagnostics.utils.Constants.SFTP_UPLOAD_ENABLED;

/**
 * Class used to zip the folder and upload the zip file to FTP/SFTP server if configured.
 */
public class ZipFileExecutor {

    private final Logger log = LogManager.getLogger(ZipFileExecutor.class);
    private final String logDirPath;
    private final int maxFiles;

    public ZipFileExecutor() {
        String logDirRelativePath = ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.ZIP_EXECUTOR_OUTPUT_DIRECTORY).toString();
        logDirPath = System.getProperty(Constants.APP_HOME) + File.separator + logDirRelativePath + File.separator;
        maxFiles = Integer.parseInt(ConfigMapHolder.getInstance().getConfigMap().
                get(Constants.ZIP_EXECUTOR_MAX_COUNT).toString());
        File logFolder = new File(logDirPath);
        if (!(logFolder.exists())) {
            logFolder.mkdir();
        }
    }

    /**
     * Zip the folder and upload the zip file to FTP/SFTP server if configured.
     */
    public void execute(String path) {
        log.info("Zipping the folder at " + path);
        File folder = new File(path);
        try {
            String zipFileName = ServerProcess.getNodeId() + "_" + folder.getName() + ".zip";
            zipFolder(path, logDirPath + zipFileName);
            log.info("Diagnosis Dumped in :" + logDirPath + zipFileName);
            FileUtils.rotateFiles(new File(logDirPath), maxFiles);
            executeUploader(logDirPath, zipFileName);
        } catch (Exception e) {
            log.error("Unable to zip the file at " + path, e);
        }
    }

    private void zipFolder(String srcFolder, String destZipFile) {

        ZipOutputStream zip;
        FileOutputStream fileWriter;
        try {
            fileWriter = new FileOutputStream(destZipFile); // set file output stream
            zip = new ZipOutputStream(fileWriter);

            addFolderToZip("", srcFolder, zip);

            zip.flush();
            zip.close();
        } catch (Exception e) {
            log.error("Error occurred while zipping the folder", e);
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
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);

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
