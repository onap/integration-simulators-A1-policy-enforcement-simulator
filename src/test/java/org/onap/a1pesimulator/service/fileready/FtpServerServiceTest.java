package org.onap.a1pesimulator.service.fileready;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.a1pesimulator.service.fileready.FtpServerService.deletePMBulkFile;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.test.StepVerifier;


class FtpServerServiceTest extends CommonFileReady {

    @InjectMocks
    FtpServerService ftpServerService;

    /**
     * Test to save archived PM Bulk File into specify directory
     */
    @Test
    void saveFileToFtp() {
        ReflectionTestUtils.setField(ftpServerService, "xmlPmLocation", TEMP_DIR);
        File archivedPmBulkFile = createTempFile(ARCHIVED_PM_BULK_FILE);
        FileData testFileData = getTestFileData();
        FileData expectedFileData = FileData.builder().archivedPmBulkFile(archivedPmBulkFile).pmBulkFile(testFileData.getPmBulkFile()).build();
        expectedFileData.setArchivedPmBulkFile(archivedPmBulkFile);

        StepVerifier.create(ftpServerService.uploadFileToFtp(testFileData))
                .expectNext(expectedFileData)
                .verifyComplete();
    }

    /**
     * Test error while trying to upload archived PM Bulk File
     */
    @Test()
    void errorWhileUploadFileToFtp() {
        ReflectionTestUtils.setField(ftpServerService, "ftpServerUpload", true);
        ReflectionTestUtils.setField(ftpServerService, "ftpServerPort", "22");
        File archivedPmBulkFile = createTempFile(ARCHIVED_PM_BULK_FILE);
        FileData testFileData = getTestFileData();
        FileData expectedFileData = FileData.builder().archivedPmBulkFile(archivedPmBulkFile).pmBulkFile(testFileData.getPmBulkFile()).build();
        expectedFileData.setArchivedPmBulkFile(archivedPmBulkFile);

        StepVerifier.create(ftpServerService.uploadFileToFtp(testFileData))
                .verifyComplete();
    }

    /**
     * Test error while trying to delete not existing file
     */
    @Test()
    void errorWhileDeletingFile() {
        Logger testLog = (Logger) LoggerFactory.getLogger(FtpServerService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        testLog.addAppender(appender);

        deletePMBulkFile(new File("test.txt"));
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly("Could not delete file: test.txt");
    }

    /**
     * Test if path to FTP is created correctly
     */
    @Test
    void getFtpPath() {
        List<String> ftpPathVars = new ArrayList<>();
        ftpPathVars.add("ftpServerProtocol");
        ftpPathVars.add("ftpServerUsername");
        ftpPathVars.add("ftpServerPassword");
        ftpPathVars.add("ftpServerUrl");
        ftpPathVars.add("ftpServerPort");
        ftpPathVars.add("ftpServerFilepath");

        ftpPathVars.forEach(var -> {
            ReflectionTestUtils.setField(ftpServerService, var, var);
        });
        String ftpPath = ftpServerService.getFtpPath();

        assertTrue(ftpPathVars.stream().allMatch(ftpPath::contains));
    }

    /**
     * Creates FileData object for test cases
     *
     * @return test FileData object
     */
    private FileData getTestFileData() {
        return FileData.builder().pmBulkFile(createTempFile(PM_BULK_FILE)).build();
    }

}