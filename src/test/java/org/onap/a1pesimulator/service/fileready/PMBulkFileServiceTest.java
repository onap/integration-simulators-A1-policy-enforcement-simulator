package org.onap.a1pesimulator.service.fileready;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.VesBrokerServiceImplTest;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

class PMBulkFileServiceTest extends CommonFileReady {

    private static final Integer NO_OF_EVENTS = 3;

    private PMBulkFileService pmBulkFileService;

    @InjectMocks
    VnfConfigReader vnfConfigReader;

    @InjectMocks
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        super.setUp();
        ReflectionTestUtils.setField(vnfConfigReader, "vnfConfigFile", "src/test/resources/vnf.config");
        pmBulkFileService = new PMBulkFileService(vnfConfigReader);
    }

    @Test
    void generatePMBulkFileXml() {
        Mono<FileData> monoFileData = pmBulkFileService.generatePMBulkFileXml(getTestEvents());
        FileData fileData = monoFileData.block();
        assertNotNull(fileData);
        assertNotNull(fileData.getPmBulkFile());
    }

    /**
     * Generate NO_OF_EVENTS test EventMemoryHolder list
     *
     * @return EventMemoryHolder list
     */
    private List<EventMemoryHolder> getTestEvents() {
        List<EventMemoryHolder> collectedEvents = new ArrayList<>();
        for (int i = 0; i < NO_OF_EVENTS; i++) {
            EventMemoryHolder eventMemoryHolder = new EventMemoryHolder("Cell1", UUID.randomUUID().toString(), 10, ZonedDateTime.now(), loadEventFromFile());
            collectedEvents.add(eventMemoryHolder);
        }
        return collectedEvents;
    }

    /**
     * Converts json to VESEvent object
     *
     * @return created VESEvent
     */
    private VesEvent loadEventFromFile() {
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
}