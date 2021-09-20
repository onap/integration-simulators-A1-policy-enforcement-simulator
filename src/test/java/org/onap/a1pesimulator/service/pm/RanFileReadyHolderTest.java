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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.data.fileready.FileReadyEvent;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.onap.a1pesimulator.service.report.RanVesSender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;

class RanFileReadyHolderTest extends CommonFileReady {

    private RanFileReadyHolder ranFileReadyHolder;

    @Mock
    PMBulkFileService pmBulkFileService;

    @Mock
    RanVesSender ranVesSender;

    @Mock
    FtpServerService ftpServerService;

    @Mock
    FileReadyEventService fileReadyEventService;

    @BeforeEach
    void setUp() {
        super.setUp();
        ranFileReadyHolder = spy(new RanFileReadyHolder(ranVesSender, ftpServerService, pmBulkFileService, fileReadyEventService));
    }

    @Test
    void createPMBulkFileAndSendFileReadyMessage() {
        ListAppender<ILoggingEvent> appender = createCommonLogAndMock();

        ranFileReadyHolder.createPMBulkFileAndSendFileReadyMessage();
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("PM Bulk file was generated, uploaded to FTP and File ready event was send to VES Collector");
    }

    @Test
    void createPMBulkFileAndSendFileReadyMessageForOneCell() {
        ListAppender<ILoggingEvent> appender = createCommonLogAndMock();

        ranFileReadyHolder.createPMBulkFileAndSendFileReadyMessageForCellId(TEST_CELL_ID);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("PM Bulk file was generated, uploaded to FTP and File ready event was send to VES Collector");
    }

    @Test
    void errorCreatePMBulkFileAndSendFileReadyMessage() {
        ListAppender<ILoggingEvent> appender = createCommonLogAndMock();
        doReturn(Mono.error(new Exception("error"))).when(fileReadyEventService).createFileReadyEventAndDeleteTmpFile(any());

        ranFileReadyHolder.createPMBulkFileAndSendFileReadyMessage();
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).contains("File ready event was unsuccessful: error");
    }

    @Test
    void saveEventToMemory() {
        ranFileReadyHolder = spy(new RanFileReadyHolder(ranVesSender, ftpServerService, pmBulkFileService, fileReadyEventService));
        try {
            ranFileReadyHolder.saveEventToMemory(loadEventFromFile(), TEST_CELL_ID, UUID.randomUUID().toString(), 30);
            ranFileReadyHolder.saveEventToMemory(loadEventFromFile(), TEST_CELL_ID, UUID.randomUUID().toString(), 30);
        } catch (VesBrokerException e) {
            e.printStackTrace();
        }
        assertThat(ranFileReadyHolder.getCollectedEventsByCell().get(TEST_CELL_ID)).hasSize(2);
    }

    @Test
    void errorSaveEventToMemory() throws VesBrokerException {
        doThrow(new VesBrokerException("error")).when(ranFileReadyHolder).saveEventToMemory(any(), any(), any(), any());

        Throwable exception = assertThrows(VesBrokerException.class,
                () -> ranFileReadyHolder.saveEventToMemory(loadEventFromFile(), TEST_CELL_ID, UUID.randomUUID().toString(), 30));
        assertThat(exception.getMessage()).contains("error");
        assertThat(ranFileReadyHolder.getCollectedEventsByCell()).isEmpty();
    }

    @Test
    void getCollectedEventsByCell() {
        Map<String, List<EventMemoryHolder>> collectedEvents = ranFileReadyHolder.getCollectedEventsByCell();
        assertNotNull(collectedEvents);
    }

    @Test
    void getCollectedEvents() {
        List<EventMemoryHolder> collectedEvents = ranFileReadyHolder.getCollectedEventsForCellId(TEST_CELL_ID);
        assertNotNull(collectedEvents);
        try {
            ranFileReadyHolder.saveEventToMemory(loadEventFromFile(), TEST_CELL_ID, UUID.randomUUID().toString(), 30);
            collectedEvents = ranFileReadyHolder.getCollectedEventsForCellId(TEST_CELL_ID);
            assertNotNull(collectedEvents);
        } catch (VesBrokerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates common Log and Mocks
     *
     * @return ListAppender<ILoggingEvent>
     */
    private ListAppender<ILoggingEvent> createCommonLogAndMock() {
        ListAppender<ILoggingEvent> appender = createCommonLog(RanFileReadyHolder.class);

        List<EventMemoryHolder> collectedEvents = getTestEvents();
        Map<String, List<EventMemoryHolder>> collectedEventsByCell = getTestEventsByCells(collectedEvents);
        FileData testFileData = FileData.builder().pmBulkFile(createTempFile(PM_BULK_FILE)).build();

        doReturn(collectedEventsByCell).when(ranFileReadyHolder).getCollectedEventsByCell();
        doReturn(collectedEvents).when(ranFileReadyHolder).getCollectedEventsForCellId(any());
        doReturn(Mono.just(testFileData)).when(pmBulkFileService).generatePMBulkFileXml(collectedEvents);
        testFileData.setArchivedPmBulkFile(createTempFile(ARCHIVED_PM_BULK_FILE));
        doReturn(Mono.just(testFileData)).when(ftpServerService).uploadFileToFtp(any());
        testFileData.setFileReadyEvent(new FileReadyEvent());
        doReturn(Mono.just(testFileData)).when(fileReadyEventService).createFileReadyEventAndDeleteTmpFile(any());
        doReturn(Mono.just(HttpStatus.SC_ACCEPTED)).when(ranVesSender).send(any());
        return appender;
    }
}