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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.utils.ConfigMapHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.wso2.diagnostics.utils.Constants.FTP_DIR;
import static org.wso2.diagnostics.utils.Constants.FTP_PASSWORD;
import static org.wso2.diagnostics.utils.Constants.FTP_PORT;
import static org.wso2.diagnostics.utils.Constants.FTP_SERVER;
import static org.wso2.diagnostics.utils.Constants.FTP_USER;

/**
 * Utility class to upload the zip file to FTP server.
 */
public class FTPUploader {

    private static final Logger log = LogManager.getLogger(FTPUploader.class);

    /**
     * This method is used to upload the zip file to FTP server.
     *
     * @param zipFilePath zip file path
     * @param zipFileName zip file name
     */
    public void uploadFile(String zipFilePath, String zipFileName) {
        String ftpServer = "";
        int ftpPort = 0;
        String ftpDir = "";
        try {
            Map<String, Object> configMap = ConfigMapHolder.getInstance().getConfigMap();
            ftpServer = (String) configMap.get(FTP_SERVER);
            String ftpUser = (String) configMap.get(FTP_USER);
            String ftpPassword = (String) configMap.get(FTP_PASSWORD);
            ftpPort = Integer.parseInt((String) configMap.get(FTP_PORT));
            ftpDir = (String) configMap.get(FTP_DIR);

            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(ftpServer, ftpPort);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setRemoteVerificationEnabled(false);

            File zipFile = new File(zipFilePath + zipFileName);
            if (!ftpDir.endsWith("/")) {
                ftpDir = ftpDir + File.separator;
            }
            String remoteFile = ftpDir + zipFileName;
            InputStream inputStream = new FileInputStream(zipFile);

            boolean done = ftpClient.storeFile(remoteFile, inputStream);
            inputStream.close();
            if (done) {
                log.info("Zip file " + zipFileName + " is uploaded successfully to the FTP server at " + ftpServer);
            }
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            log.error("Error occurred while uploading the zip file to the FTP server at " + ftpServer +
                    ", port " + ftpPort + ", directory " + ftpDir, e);
        }
    }
}
