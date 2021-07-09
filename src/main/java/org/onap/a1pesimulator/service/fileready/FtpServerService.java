package org.onap.a1pesimulator.service.fileready;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.onap.a1pesimulator.data.fileready.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@PropertySource("classpath:application.properties")
public class FtpServerService {

    private static final Logger log = LoggerFactory.getLogger(FtpServerService.class);

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
                .flatMap(this::tryToUploadFileToFtp)
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
        fileData.setArchivedPmBulkFile(fileData.getPmBulkFile());
        return Mono.just(fileData);
    }

    /**
     * Upload file to FTP
     *
     * @param fileData archived file in Mono
     * @return archived file for fileReadyEvent
     */
    private Mono<FileData> tryToUploadFileToFtp(FileData fileData) {
        return Mono.just(fileData);
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
            log.error("Could not delete file: {}", file.getName(), e);
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
     */
    private Mono<FileData> resumeError(Throwable throwable, FileData fileData) {
        log.error("Error occurs while uploading file to FTP server", throwable);
        deletePMBulkFile(fileData.getPmBulkFile());
        deletePMBulkFile(fileData.getArchivedPmBulkFile());
        return Mono.empty();
    }
}
