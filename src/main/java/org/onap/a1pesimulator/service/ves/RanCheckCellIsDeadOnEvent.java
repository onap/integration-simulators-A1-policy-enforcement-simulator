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

package org.onap.a1pesimulator.service.ves;

import static org.onap.a1pesimulator.service.cell.RanCellStateService.TOPIC_CELL;

import java.util.Optional;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.state.CellStateEnum;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.MeasurementFields;
import org.onap.a1pesimulator.service.cell.RanCellsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RanCheckCellIsDeadOnEvent implements OnEventAction {

    private static final Logger log = LoggerFactory.getLogger(RanCheckCellIsDeadOnEvent.class);

    private final RanCellsHolder ranCellsHolder;
    private final SimpMessagingTemplate messagingTemplate;

    private final Integer failingModeThroughputValue;
    private final Integer failingModeLatencyValue;
    private final Integer failingCheckoutDelayTimeInSec;

    private static final int TO_MICRO_SEC = 1_000_000;

    public RanCheckCellIsDeadOnEvent(RanCellsHolder ranCellsHolder, SimpMessagingTemplate messagingTemplate,
            @Value("${ves.failing.throughput}") Integer failingModeThroughputValue,
            @Value("${ves.failing.latency}") Integer failingModeLatencyValue,
            @Value("${ves.failing.checkout.delay}") Integer failingCheckoutDelayTimeInSec) {
        this.ranCellsHolder = ranCellsHolder;
        this.messagingTemplate = messagingTemplate;

        this.failingModeThroughputValue = failingModeThroughputValue;
        this.failingModeLatencyValue = failingModeLatencyValue;
        this.failingCheckoutDelayTimeInSec = failingCheckoutDelayTimeInSec;
    }

    @Override
    public void onEvent(VesEvent event) {
        Optional<String> cellId = getCellIdentifier(event);
        Optional<String> throughput = getCellThroughput(event);
        Optional<String> latency = getCellLatency(event);

        if (cellId.isPresent() && throughput.isPresent() && latency.isPresent()) {
            checkCell(cellId.get(), Integer.parseInt(throughput.get()), Integer.parseInt(latency.get()),
                    event.getCommonEventHeader().getLastEpochMicrosec());
        }
    }

    private void checkCell(String cellId, Integer throughput, Integer latency, Long lastEpochMicrosec) {
        if (throughput <= failingModeThroughputValue && latency >= failingModeLatencyValue) {
            log.info("Failure mode detected for cell {}", cellId);
            processSleepingMode(cellId, lastEpochMicrosec);
        }
    }

    private void processSleepingMode(String cellId, Long lastEpochMicrosec) {
        CellDetails cell = ranCellsHolder.getCellById(cellId);
        if (cell.getCellStateMachine().getState() == CellStateEnum.GOING_TO_SLEEP) {
            Optional<RanCellsHolder.CellInFailureMode> cellInFailureModeOpt =
                    ranCellsHolder.getCellsInFailureMode(cellId);
            if (cellInFailureModeOpt.isPresent()) {
                RanCellsHolder.CellInFailureMode cellInFailureMode = cellInFailureModeOpt.get();
                if (cellInFailureMode.getSleepingModeDetectedTime() == null) {
                    cellInFailureMode.setSleepingModeDetectedTime(lastEpochMicrosec);
                } else {
                    long waitingEpochMicrosec = addDelayTime(cellInFailureMode.getSleepingModeDetectedTime());
                    if (lastEpochMicrosec >= waitingEpochMicrosec) {
                        log.info("Cell {} is sleeping!", cellId);
                        cell.nextState();
                        messagingTemplate.convertAndSend(TOPIC_CELL, cell);
                    }
                }
            }
        }
    }

    private Optional<String> getCellIdentifier(VesEvent event) {
        return getValueFromAdditionalMeasurement(event, "identifier");
    }

    private Optional<String> getCellThroughput(VesEvent event) {
        return getValueFromAdditionalMeasurement(event, "throughput");
    }

    private Optional<String> getCellLatency(VesEvent event) {
        return getValueFromAdditionalMeasurement(event, "latency");
    }

    private Optional<String> getValueFromAdditionalMeasurement(VesEvent event, String key) {
        Optional<MeasurementFields.AdditionalMeasurement> measurement = getAdditionalMeasurement(event, key);
        return measurement.map(this::getValueFromAdditionalMeasurement);
    }

    private String getValueFromAdditionalMeasurement(MeasurementFields.AdditionalMeasurement measurement) {
        return measurement.getHashMap().get("value");
    }

    private Optional<MeasurementFields.AdditionalMeasurement> getAdditionalMeasurement(VesEvent event,
            String additionalMeasurement) {
        return event.getMeasurementFields().getAdditionalMeasurements().stream()
                .filter(e -> e.getName().equals(additionalMeasurement)).findFirst();
    }

    private long addDelayTime(long epoch) {
        return epoch + failingCheckoutDelayTimeInSec * TO_MICRO_SEC;
    }
}
