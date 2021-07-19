package org.onap.a1pesimulator.service.fileready;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Mono;

class PMBulkFileServiceTest extends CommonFileReady {

      private PMBulkFileService pmBulkFileService;

    @InjectMocks
    VnfConfigReader vnfConfigReader;


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


}