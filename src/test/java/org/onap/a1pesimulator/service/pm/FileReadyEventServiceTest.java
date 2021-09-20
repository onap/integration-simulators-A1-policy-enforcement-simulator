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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.InvalidPathException;
import java.time.ZonedDateTime;

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
        FileData testData = getTestFileData();
        Mono<FileData> fileMono = Mono.just(testData);
        FileData expectedFileData = fileReadyEventService.createFileReadyEvent(testData);
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
            return FileData.builder().pmBulkFile(createTempFile(PM_BULK_FILE)).startEventDate(ZonedDateTime.now())
                    .endEventDate(ZonedDateTime.now().plusMinutes(5)).archivedPmBulkFile(createTempFile(ARCHIVED_PM_BULK_FILE)).build();
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }
        return null;
    }
}