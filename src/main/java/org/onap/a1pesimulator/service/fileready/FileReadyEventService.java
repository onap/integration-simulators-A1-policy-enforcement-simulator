package org.onap.a1pesimulator.service.fileready;

import static org.onap.a1pesimulator.service.fileready.FtpServerService.deletePMBulkFile;

import java.io.File;

import org.onap.a1pesimulator.data.fileready.FileData;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Service for PM Bulk File creation and its handling
 */

@Service
public class FileReadyEventService {

    /**
     * It will create FileReadyEvent.json which will go to VES Collector
     *
     * @return created FileReadyEvent
     */
    protected Mono<FileData> createFileReadyEventAndDeleteTmpFile(Mono<FileData> fileMono) {
        return fileMono
                .map(this::createFileReadyEvent)
                .doOnNext(file -> deleteTempArchivedBulkFile(file.getArchivedPmBulkFile()));
    }

    /**
     * Creates File Ready Event
     *
     * @param fileData information about PM Bulk Files created in previous steps
     * @return added newly created FileReadyEvent to FileData
     */
    protected FileData createFileReadyEvent(FileData fileData) {
        return fileData;
    }

    /**
     * Deletes temporary archived PM Bulk File
     *
     * @param fileMono temporary archived PM Bulk File
     */
    private void deleteTempArchivedBulkFile(File fileMono) {
        deletePMBulkFile(fileMono);
    }
}
