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

package org.onap.a1pesimulator.service.fileready;

import static java.util.Objects.isNull;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.onap.a1pesimulator.service.ves.RanVesSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Mono;

/**
 * Entry point for PM Bulk File event
 */
@Service
public class RanFileReadyHolder {

    private static final Logger log = LoggerFactory.getLogger(RanFileReadyHolder.class);

    private List<EventMemoryHolder> collectedEvents;
    private final RanVesSender ranVesSender;
    private final FtpServerService ftpServerService;
    private final PMBulkFileService xmlFileService;
    private final FileReadyEventService fileReadyEventService;

    public RanFileReadyHolder(RanVesSender ranVesSender, FtpServerService ftpServerService, PMBulkFileService xmlFileService,
            FileReadyEventService fileReadyEventService) {
        this.ranVesSender = ranVesSender;
        this.ftpServerService = ftpServerService;
        this.xmlFileService = xmlFileService;
        this.fileReadyEventService = fileReadyEventService;
    }

    /**
     * Run entire process: PM Bulk File creation-> upload to FTP -> delete temp PM Bulk File -> create File Ready Event - > send it to VES Collector
     * collectedEvents are synchronized to not be updated by other threads during PM Bulk File creation
     */
    public void createPMBulkFileAndSendFileReadyMessage() {
        synchronized (getCollectedEvents()) {
            Mono.justOrEmpty(getCollectedEvents())
                    .filter(this::areSomeEventsStored)
                    .flatMap(xmlFileService::generatePMBulkFileXml)
                    .map(ftpServerService::uploadFileToFtp)
                    .flatMap(fileReadyEventService::createFileReadyEventAndDeleteTmpFile)
                    .doOnNext(this::sendEventToVesCollector)
                    .subscribe(fileData -> informAboutSuccess(), this::informAboutError);
        }
    }

    /**
     * Adds current event to the memory, which is List<EventMemoryHolder>
     *
     * @param vesEvent event from specific cell
     * @throws VesBrokerException in case of any problem with adding to List, it throws an exception
     */
    public void saveEventToMemory(VesEvent vesEvent, String cellId, String jobId, Integer granPeriod) throws VesBrokerException {
        try {
            getCollectedEvents().add(new EventMemoryHolder(cellId, jobId, granPeriod, ZonedDateTime.now(), vesEvent));
            log.trace("Saving VES event for cell {} with granularity period {} and sequence number {}", cellId, granPeriod, getCollectedEvents().size());
        } catch (Exception e) {
            String errorMsg = "Failed to save VES event to memory with exception:" + e;
            throw new VesBrokerException(errorMsg);
        }
    }

    /**
     * Sends FileReadyEvent to VES Collector
     *
     * @param fileData object with FileReadyEvent file
     */
    protected void sendEventToVesCollector(FileData fileData) {
        ranVesSender.send(fileData.getFileReadyEvent());
    }

    /**
     * Log about successful operation
     */
    private void informAboutSuccess() {
        log.info("PM Bulk file was generated, uploaded to FTP and File ready event was send to VES Collector");
    }

    /**
     * Log an error if occurs during the process
     *
     * @param throwable - error raised in some of the steps
     */
    private void informAboutError(Throwable throwable) {
        log.info("File ready event was unsuccessful: {}", throwable.getMessage());
    }

    /**
     * Check if there are any Events stored in the memory. Used before creating PM Bulk File xml
     *
     * @param collectedEvents list of stored events
     * @return true there is at least one event / false - no event at all
     */
    private boolean areSomeEventsStored(List<EventMemoryHolder> collectedEvents) {
        return !CollectionUtils.isEmpty(collectedEvents);
    }

    /**
     * Factory to get List<EventMemoryHolder>
     *
     * @return existing or newly created List<EventMemoryHolder>
     */
    public List<EventMemoryHolder> getCollectedEvents() {
        if (isNull(collectedEvents)) {
            collectedEvents = Collections.synchronizedList(new ArrayList<>());
        }
        return collectedEvents;
    }
}
