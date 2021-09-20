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