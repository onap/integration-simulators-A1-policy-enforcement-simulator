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

package org.onap.a1pesimulator.service;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.report.RanReportsBrokerService;
import org.onap.a1pesimulator.service.report.RanVesSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class VesBrokerServiceImplTest {

    private static final String VES_COLLECTOR_URL = "someProtocol://someVesCollectorIP:someVesCollectorPort/somePath";

    @Autowired
    private RanReportsBrokerService vesBrokerService;
    @Autowired
    private RanVesSender vesSender;
    @Autowired
    private ObjectMapper mapper;
    @Mock
    private RestTemplate restTemplate;

    @Before
    public void before() {
        ReflectionTestUtils.setField(vesSender, "restTemplate", restTemplate);
    }

    @Test
    public void testStartSendingVes() throws Exception {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.exchange(ArgumentMatchers.eq(VES_COLLECTOR_URL), ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(String.class))).thenReturn(responseEntity);

        ResponseEntity<String> response = vesBrokerService.startSendingReports("CustomIdentifier",
                loadEventFromFile("VesBrokerControllerTest_pm_ves.json"), 10, ReportingMethodEnum.VES);

        Assert.assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    private VesEvent loadEventFromFile(String fileName) throws Exception {
        return mapper.readValue(loadFileContent(fileName), VesEvent.class);
    }

    private String loadFileContent(String fileName) throws IOException, URISyntaxException {
        Path path = Paths.get(VesBrokerServiceImplTest.class.getResource(fileName).toURI());
        return new String(Files.readAllBytes(path));
    }
}
