/*
 * Copyright (C) 2021 Samsung Electronics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.onap.a1pesimulator.service.pm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.onap.a1pesimulator.service.pm.FtpServerService.deletePMBulkFile;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import reactor.test.StepVerifier;


class FtpServerServiceTest extends CommonFileReady {

    private FtpServerService ftpServerService;

    @Mock
    SSHClient sshClient;

    @Mock
    SFTPClient sftpClient;

    @InjectMocks
    VnfConfigReader vnfConfigReader;

    @BeforeEach
    void setUp() {
        super.setUp();
        ReflectionTestUtils.setField(vnfConfigReader, "vnfConfigFile", "src/test/resources/vnf.config");
        ftpServerService = spy(new FtpServerService(vnfConfigReader));
    }

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
     * Test successful FTP upload
     */
    @Test
    void uploadFileToFtp() {
        ReflectionTestUtils.setField(ftpServerService, "ftpServerUpload", true);
        doReturn(sshClient).when(ftpServerService).getSSHClient();
        try {
            doReturn(sftpClient).when(sshClient).newSFTPClient();
            doNothing().when(sftpClient).put(anyString(), anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        File archivedPmBulkFile = createTempFile(ARCHIVED_PM_BULK_FILE);
        FileData testFileData = getTestFileData();
        FileData expectedFileData = FileData.builder().archivedPmBulkFile(archivedPmBulkFile).pmBulkFile(testFileData.getPmBulkFile()).build();
        expectedFileData.setArchivedPmBulkFile(archivedPmBulkFile);

        StepVerifier.create(ftpServerService.uploadFileToFtp(testFileData))
                .expectNext(expectedFileData).verifyComplete();
    }

    /**
     * Test error while trying to upload archived PM Bulk File
     */
    @Test
    void errorWhileUploadingFileToFtp() {
        ReflectionTestUtils.setField(ftpServerService, "ftpServerUpload", true);
        FileData testFileData = getTestFileData();
        StepVerifier.create(ftpServerService.uploadFileToFtp(testFileData))
                .verifyComplete();
        verify(ftpServerService, times(1)).resumeError(any(), any());
    }

    /**
     * Test error while trying to delete not existing file
     */
    @Test
    void errorWhileDeletingFile() {
        ListAppender<ILoggingEvent> appender = createCommonLog(FtpServerService.class);

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
        ftpPathVars.add("ftpServerFilepath");

        ftpPathVars.forEach(var -> ReflectionTestUtils.setField(ftpServerService, var, var));
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