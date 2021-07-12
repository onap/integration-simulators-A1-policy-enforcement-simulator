package org.onap.a1pesimulator.service.fileready;

import static java.util.Objects.nonNull;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;


import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.exception.NotUploadedToFtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import reactor.core.publisher.Mono;

@Service
public class FtpServerService {

    private static final Logger log = LoggerFactory.getLogger(FtpServerService.class);

    //true - file will be uploaded to FTP; false - file will be copied into xmlPmLocation
    @Value("${ftp.server.upload}")
    private boolean ftpServerUpload;

    // location where archived file will be copied
    @Value("${xml.pm.location}")
    private String xmlPmLocation;

    @Value("${ftp.server.url}")
    private String ftpServerUrl;

    @Value("${ftp.server.protocol}")
    private String ftpServerProtocol;

    @Value("${ftp.server.port}")
    private String ftpServerPort;

    @Value("${ftp.server.filepath}")
    private String ftpServerFilepath;

    @Value("${ftp.server.username}")
    private String ftpServerUsername;

    @Value("${ftp.server.password}")
    private String ftpServerPassword;

    public Mono<FileData> uploadFileToFtp(FileData fileData) {
        return Mono.just(fileData)
                .flatMap(this::tryToCompressFile)
                .flatMap(this::tryToUploadOrSaveFileToFtp)
                .onErrorResume(throwable -> resumeError(throwable, fileData))
                .doOnNext(file -> deletePMBulkFile(file.getPmBulkFile()));
    }

    /**
     * Trying to compress file into .gz
     *
     * @param fileData file to be archived
     * @return archived file
     */
    private Mono<FileData> tryToCompressFile(FileData fileData) {
        File archiveBulkFile = new File(TEMP_DIR, fileData.getPmBulkFile().getName() + ".gz");

        try (GZIPOutputStream zos = new GZIPOutputStream(
                new FileOutputStream(archiveBulkFile.getAbsolutePath())); FileInputStream inputStream = new FileInputStream(fileData.getPmBulkFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            fileData.setArchivedPmBulkFile(archiveBulkFile);
            log.trace("Compressing file {}", fileData.getPmBulkFile().getName());
            return Mono.just(fileData);
        } catch (IOException e) {
            log.error("Could not compress file", e);
            return Mono.empty();
        }
    }

    /**
     * Upload file to FTP or copy it to mounted location
     *
     * @param fileData data about file
     * @return fileData for fileReadyEvent
     */
    private Mono<FileData> tryToUploadOrSaveFileToFtp(FileData fileData) {
        if (ftpServerUpload) {
            return tryToUploadFileToFtp(fileData);
        } else {
            File fileOnFtp = new File(xmlPmLocation, fileData.getArchivedPmBulkFile().getName());
            try {
                FileCopyUtils.copy(fileData.getArchivedPmBulkFile(), fileOnFtp);
                return Mono.just(fileData);
            } catch (IOException e) {
                return Mono.error(new NotUploadedToFtpException("File was not copied to FTP location", e));
            }
        }
    }

    /**
     * Upload file to FTP
     *
     * @param fileData archived file in Mono
     * @return archived file for fileReadyEvent
     */
    private Mono<FileData> tryToUploadFileToFtp(FileData fileData) {
        if (nonNull(fileData.getArchivedPmBulkFile())) {
            SSHClient client = getSSHClient();
            if (nonNull(client)) {
                try (client; SFTPClient sftpClient = client.newSFTPClient()) {
                    File archiveBulkFile = fileData.getArchivedPmBulkFile();
                    sftpClient.put(archiveBulkFile.getAbsolutePath(), ftpServerFilepath + "/" + archiveBulkFile.getName());

                    log.info("Uploading file to FTP: {}", archiveBulkFile.getAbsoluteFile());
                    return Mono.just(fileData);
                } catch (IOException e) {
                    log.error("Exception while trying to upload a file", e);
                }
            } else {
                log.error("Could not connect to FTP server");
            }
        } else {
            log.error("There is no file to upload");
        }
        return Mono.error(new NotUploadedToFtpException("File was not uploaded to FTP"));
    }

    /**
     * Creates SSHClient instance
     *
     * @return SSHClient
     */
    private SSHClient getSSHClient() {
        SSHClient client = new SSHClient();
        try {
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(ftpServerUrl, Integer.parseInt(ftpServerPort));
            client.authPassword(ftpServerUsername, ftpServerPassword);
            return client;
        } catch (IOException e) {
            log.error("There was an error while connecting to FTP server", e);
            try {
                client.close();
            } catch (IOException ioException) {
                log.error("There was an error while closing the connection to FTP server", e);
            }
            return null;
        }
    }

    /**
     * Deletes created PM Bulk File xml from temp storage after successful upload to FTP
     *
     * @param file file which we gonna delete
     */
    public static void deletePMBulkFile(File file) {
        try {
            log.trace("Deleting file: {}", file.getAbsoluteFile());
            Files.delete(file.toPath());
        } catch (IOException e) {
            log.warn("Could not delete file: {}", file.getName(), e);
        }
    }

    /**
     * Get path to FTP server
     *
     * @return for example:  "sftp://foo:pass@106.120.119.170:2222/upload/"
     */
    public String getFtpPath() {
        return ftpServerProtocol + "://" + ftpServerUsername + ":" + ftpServerPassword + "@" + ftpServerUrl + ":" + ftpServerPort + "/" + ftpServerFilepath
                + "/";
    }

    /**
     * Try to clean up things after an exception
     *
     * @param throwable error thrown
     * @param fileData data about files which needs to be deleted
     * @return empty Mono object
     */
    private Mono<FileData> resumeError(Throwable throwable, FileData fileData) {
        log.error("Error occurs while uploading file to FTP server", throwable);
        deletePMBulkFile(fileData.getPmBulkFile());
        deletePMBulkFile(fileData.getArchivedPmBulkFile());
        return Mono.empty();
    }
}
