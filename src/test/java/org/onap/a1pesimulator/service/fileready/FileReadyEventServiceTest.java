package org.onap.a1pesimulator.service.fileready;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.InvalidPathException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.fileready.FileData;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FileReadyEventServiceTest extends CommonFileReady {

    @Mock
    private FtpServerService ftpServerService;

    @InjectMocks
    private FileReadyEventService fileReadyEventService;

    @BeforeEach
    void setUp() {
        super.setUp();
        when(ftpServerService.getFtpPath()).thenReturn("");
    }

    @Test
    void createFileReadyEventAndDeleteTmpFile() {
        Mono<FileData> fileMono = Mono.just(getTestFileData());
        FileData expectedFileData = fileReadyEventService.createFileReadyEvent(getTestFileData());
        StepVerifier.create(fileReadyEventService.createFileReadyEventAndDeleteTmpFile(fileMono))
                .expectNext(expectedFileData)
                .verifyComplete();
        Mono<FileData> resultFileData = fileReadyEventService.createFileReadyEventAndDeleteTmpFile(fileMono);
        assertFileDataResults(resultFileData.block());
        verify(ftpServerService, times(3)).getFtpPath();
    }

    @Test
    void createFileReadyEvent() {
        FileData resultFileData = fileReadyEventService.createFileReadyEvent(getTestFileData());
        assertFileDataResults(resultFileData);
        verify(ftpServerService, times(1)).getFtpPath();
    }

    /**
     * Common asserst for all tests here
     */
    private void assertFileDataResults(FileData fileData) {
        assertNotNull(fileData);
        assertNotNull(fileData.getFileReadyEvent());
        assertEquals(ARCHIVED_PM_BULK_FILE, fileData.getFileReadyEvent().getNotificationFields().getArrayOfNamedHashMap().get(0).getHashMap().get("location"));
    }

    /**
     * Creates FileData object for test cases
     *
     * @return test FileData object
     */
    private FileData getTestFileData() {
        try {
            return FileData.builder().pmBulkFile(createTempFile(PM_BULK_FILE)).archivedPmBulkFile(createTempFile(ARCHIVED_PM_BULK_FILE)).build();
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }
        return null;
    }
}