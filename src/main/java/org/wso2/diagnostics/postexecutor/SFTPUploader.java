package org.wso2.diagnostics.postexecutor;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.diagnostics.utils.ConfigMapHolder;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import static org.wso2.diagnostics.utils.Constants.SFTP_DIR;
import static org.wso2.diagnostics.utils.Constants.SFTP_KNOWN_HOSTS;
import static org.wso2.diagnostics.utils.Constants.SFTP_PASSWORD;
import static org.wso2.diagnostics.utils.Constants.SFTP_PORT;
import static org.wso2.diagnostics.utils.Constants.SFTP_SERVER;
import static org.wso2.diagnostics.utils.Constants.SFTP_STRICT_HOST_KEY_CHECKING;
import static org.wso2.diagnostics.utils.Constants.SFTP_USER;

/**
 * Utility class to upload the zip file to SFTP server.
 */
public class SFTPUploader {
    private static final Logger log = LogManager.getLogger(SFTPUploader.class);

    public void uploadFile(String zipFilePath, String zipFileName) {

        try {
            Map<String, Object> configMap = ConfigMapHolder.getInstance().getConfigMap();
            String sftpServer = configMap.get(SFTP_SERVER).toString();
            String sftpUser = configMap.get(SFTP_USER).toString();
            String sftpPassword = configMap.get(SFTP_PASSWORD).toString();
            int sftpPort = Integer.parseInt(configMap.get(SFTP_PORT).toString());
            String sftpDir = configMap.get(SFTP_DIR).toString();
            String knownHosts = configMap.get(SFTP_KNOWN_HOSTS).toString();
            String strictHostKeyChecking = configMap.get(SFTP_STRICT_HOST_KEY_CHECKING).toString();

            JSch jsch = new JSch();
            if (StringUtils.isNotEmpty(knownHosts)) {
                jsch.setKnownHosts(knownHosts);
            }
            if (StringUtils.isEmpty(sftpServer) || StringUtils.isEmpty(sftpUser) || StringUtils.isEmpty(sftpPassword) ||
                    StringUtils.isEmpty(sftpDir) || sftpPort == 0) {
                log.info("SFTP server details are not provided. Hence skipping the SFTP upload.");
                return;
            }
            Session session = jsch.getSession(sftpUser, sftpServer, sftpPort);
            session.setPassword(sftpPassword);
            if (StringUtils.equals(strictHostKeyChecking, "no")) {
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
            }
            session.connect();
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            File zipFile = new File(zipFilePath + zipFileName);
            if (!sftpDir.endsWith(File.separator)) {
                sftpDir = sftpDir + File.separator;
            }
            String remoteFile = sftpDir + zipFileName;
            sftpChannel.put(new FileInputStream(zipFile), remoteFile);

            sftpChannel.exit();
            session.disconnect();
            log.info("Successfully uploaded the zip file to SFTP server at " + sftpServer +
                    " to the directory " + sftpDir);
        } catch (Exception e) {
            log.error("Error occurred while uploading the zip file to SFTP server.", e);
        }
    }
}
