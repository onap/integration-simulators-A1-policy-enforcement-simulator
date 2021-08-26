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

import static org.onap.a1pesimulator.TestHelpers.deleteTempFiles;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.VesBrokerServiceImplTest;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class CommonFileReady {

    public List<File> filesToDelete;  //we collect files created during testing and then delete them
    public static final String PM_BULK_FILE = "pmBulkFile.xml";
    public static final String ARCHIVED_PM_BULK_FILE = "pmBulkFile.xml.gz";
    public static final String TEST_CELL_ID = "Cell1";
    public static final Integer NO_OF_EVENTS = 3;
    public static final Integer NO_OF_CELLS = 2;

    @InjectMocks
    private ObjectMapper mapper;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        filesToDelete = Collections.synchronizedList(new ArrayList<>());
    }

    @AfterEach
    void cleanUpFiles() {
        deleteTempFiles(filesToDelete);
    }

    /**
     * Create temp file with simple text and adds it to filesToDelete list
     *
     * @param fileName name of file
     * @return created file
     */
    public File createTempFile(String fileName) {
        try {
            File tmpFile = new File(TEMP_DIR, fileName);
            tmpFile.createNewFile();
            Files.write(tmpFile.toPath(), "sample text".getBytes());
            filesToDelete.add(tmpFile);
            return tmpFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate NO_OF_EVENTS test EventMemoryHolder list
     *
     * @return EventMemoryHolder list
     */
    protected List<EventMemoryHolder> getTestEvents() {
        List<EventMemoryHolder> collectedEvents = new ArrayList<>();
        for (int i = 0; i < NO_OF_EVENTS; i++) {
            EventMemoryHolder eventMemoryHolder = new EventMemoryHolder(TEST_CELL_ID, UUID.randomUUID().toString(), 10, ZonedDateTime.now(),
                    loadEventFromFile());
            collectedEvents.add(eventMemoryHolder);
        }
        return collectedEvents;
    }

    /**
     * Generate events by CellId
     *
     * @return Map by CellId and list of events
     */
    protected Map<String, List<EventMemoryHolder>> getTestEventsByCells(List<EventMemoryHolder> eventList) {
        Map<String, List<EventMemoryHolder>> collectedEventsByCell = new HashMap<>();
        for (int cellId = 0; cellId < NO_OF_CELLS; cellId++) {
            collectedEventsByCell.put("Cell" + cellId, eventList);
        }
        return collectedEventsByCell;
    }

    /**
     * Converts json to VESEvent object
     *
     * @return created VESEvent
     */
    protected VesEvent loadEventFromFile() {
        try {
            return mapper.readValue(loadFileContent("VesBrokerControllerTest_pm_ves.json"), VesEvent.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get json string from specified json file
     *
     * @param fileName name of test json file
     * @return json file as string
     */
    private String loadFileContent(String fileName) {
        Path path;
        try {
            path = Paths.get(VesBrokerServiceImplTest.class.getResource(fileName).toURI());
            return new String(Files.readAllBytes(path));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create common log
     *
     * @return ListAppender<ILoggingEvent>
     */
    protected ListAppender<ILoggingEvent> createCommonLog(Class clazz) {
        Logger testLog = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        testLog.addAppender(appender);
        return appender;
    }
}
