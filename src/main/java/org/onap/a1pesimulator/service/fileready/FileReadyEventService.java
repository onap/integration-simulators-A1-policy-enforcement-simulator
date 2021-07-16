package org.onap.a1pesimulator.service.fileready;

import static org.onap.a1pesimulator.service.fileready.FtpServerService.deletePMBulkFile;
import static org.onap.a1pesimulator.util.Constants.FILE_READY_CHANGE_IDENTIFIER;
import static org.onap.a1pesimulator.util.Constants.FILE_READY_CHANGE_TYPE;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.data.fileready.FileReadyEvent;
import org.onap.a1pesimulator.data.fileready.NotificationFields;
import org.onap.a1pesimulator.data.fileready.NotificationFields.ArrayOfNamedHashMap;
import org.onap.a1pesimulator.data.ves.CommonEventHeader;
import org.onap.a1pesimulator.util.RanVesUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Service for PM Bulk File creation and its handling
 */

@Service
public class FileReadyEventService {

    private final FtpServerService ftpServerService;

    @Value("${file.ready.version}")
    private String version;

    @Value("${file.ready.vesEventListenerVersion}")
    private String vesEventListenerVersion;

    @Value("${file.ready.domain}")
    private String domain;

    @Value("${file.ready.eventName}")
    private String eventName;

    @Value("${file.ready.fileFormatType}")
    private String fileFormatType;

    @Value("${file.ready.fileFormatVersion}")
    private String fileFormatVersion;

    @Value("${file.ready.notificationFieldsVersion}")
    private String notificationFieldsVersion;

    @Value("${file.ready.priority}")
    private String priority;

    @Value("${file.ready.reportingEntityName}")
    private String reportingEntityName;

    public FileReadyEventService(FtpServerService ftpServerService) {
        this.ftpServerService = ftpServerService;
    }

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
        FileReadyEvent event = new FileReadyEvent();
        event.setCommonEventHeader(getCommonHeader());
        RanVesUtils.updateHeader(event);
        event.setNotificationFields(getNotificationFields(fileData.getArchivedPmBulkFile().getName()));
        fileData.setFileReadyEvent(event);
        return fileData;
    }

    /**
     * Creates NotificationFields section in FileReadyEvent
     *
     * @param fileName name of archived PM Bulk File
     * @return NotificationFields object
     */
    private NotificationFields getNotificationFields(String fileName) {
        NotificationFields notificationFields = NotificationFields.builder()
                .changeIdentifier(FILE_READY_CHANGE_IDENTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE)
                .notificationFieldsVersion(notificationFieldsVersion).build();

        ArrayOfNamedHashMap arrayOfNamedHashMap = new ArrayOfNamedHashMap();
        Map<String, String> hashMapItems = new HashMap<>();
        hashMapItems.put("location", ftpServerService.getFtpPath() + fileName);
        hashMapItems.put("compression", "gzip");
        hashMapItems.put("fileFormatType", fileFormatType);
        hashMapItems.put("fileFormatVersion", fileFormatVersion);

        arrayOfNamedHashMap.setName(fileName);
        arrayOfNamedHashMap.setHashMap(hashMapItems);
        notificationFields.setArrayOfNamedHashMap(Collections.singletonList(arrayOfNamedHashMap));
        return notificationFields;
    }

    /**
     * Creates CommonEventHeader
     *
     * @return created CommonEventHeader
     */
    private CommonEventHeader getCommonHeader() {
        return CommonEventHeader.builder()
                .version(version)
                .vesEventListenerVersion(vesEventListenerVersion)
                .domain(domain)
                .eventName(eventName)
                .eventId(UUID.randomUUID().toString())
                .priority(priority)
                .reportingEntityName(reportingEntityName)
                .sequence(0)
                .timeZoneOffset(ZonedDateTime.now().getOffset().toString())
                .build();
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
