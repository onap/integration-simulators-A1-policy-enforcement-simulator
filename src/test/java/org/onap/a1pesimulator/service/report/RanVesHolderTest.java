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

package org.onap.a1pesimulator.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.fileready.RanPeriodicEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.common.EventCustomizer;
import org.onap.a1pesimulator.service.pm.CommonFileReady;
import org.onap.a1pesimulator.service.pm.RanFileReadyHolder;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

class RanVesHolderTest extends CommonFileReady {

    private RanVesHolder ranCellsHolder;

    @Mock
    RanVesDataProvider vesDataProvider;

    @Mock
    Collection<OnEventAction> onEventActions;

    @Mock
    RanFileReadyHolder ranFileReadyHolder;

    @Mock
    RanVesSender vesSender;

    @Mock
    EventCustomizer regularEventCustomizer;

    @Mock
    RanUeHolder ranUeHolder;

    @InjectMocks
    VnfConfigReader vnfConfigReader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(vnfConfigReader, "vnfConfigFile", "src/test/resources/vnf.config");
        ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler = spy(new ThreadPoolTaskScheduler());
        vesPmThreadPoolTaskScheduler.initialize();
        RanEventCustomizerFactory eventCustomizerFactory = spy(new RanEventCustomizerFactory(regularEventCustomizer, ranUeHolder));
        ranCellsHolder = spy(new RanVesHolder(vesPmThreadPoolTaskScheduler, ranFileReadyHolder, vesSender,
                vnfConfigReader, eventCustomizerFactory, vesDataProvider, onEventActions));
    }

    @Test
    void getPeriodicEventsCache() {
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.FILE_READY);
        Map<String, RanPeriodicEvent> periodicEventsCache = ranCellsHolder.getPeriodicEventsCache();
        assertThat(periodicEventsCache).containsKey(TEST_CELL_ID);
    }

    @Test
    void startSendingVesEvents() {
        ResponseEntity<String> response = ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.FILE_READY);
        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("VES Event sending started");
    }

    @Test
    void startSendingFailureVesEvents() {
        doReturn(10).when(vesDataProvider).getFailureVesInterval();
        ResponseEntity<String> response = ranCellsHolder.startSendingFailureVesEvents(TEST_CELL_ID, loadEventFromFile(), ReportingMethodEnum.FILE_READY);
        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("Failure VES Event sending started");
    }

    @Test
    void stopSendingVesEvents() {
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.FILE_READY);
        Optional<RanPeriodicEvent> response = ranCellsHolder.stopSendingVesEvents(TEST_CELL_ID);
        assertThat(response).isPresent();
    }

    @Test
    void getEnabledEventElementIdentifiers() {
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.VES);
        Collection<String> elements = ranCellsHolder.getEnabledEventElementIdentifiers();
        assertThat(elements).isNotEmpty();
    }

    @Test
    void isEventEnabled() {
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.FILE_READY);
        boolean enabled = ranCellsHolder.isEventEnabled(TEST_CELL_ID);
        assertThat(enabled).isTrue();
    }

    @Test
    void isAnyEventRunning() {
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, loadEventFromFile(), 10, ReportingMethodEnum.FILE_READY);
        boolean isRunning = ranCellsHolder.isAnyEventRunning();
        assertThat(isRunning).isTrue();
    }

    @Test
    void getPMConfiguration() {
        VesEvent testedEvent = loadEventFromFile();
        ranCellsHolder.startSendingVesEvents(TEST_CELL_ID, testedEvent, 10, ReportingMethodEnum.FILE_READY);
        RanPeriodicEvent event = ranCellsHolder.getPeriodicEventForCell(TEST_CELL_ID);
        assertThat(event.getEvent()).isEqualTo(testedEvent);
        assertThat(event.getInterval()).isEqualTo(10);
        assertThat(event.getReportingMethod()).isEqualTo(ReportingMethodEnum.FILE_READY.getValue());
    }

    @Test
    void getPMConfigurationError() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ranCellsHolder.getPeriodicEventForCell(TEST_CELL_ID));
    }
}