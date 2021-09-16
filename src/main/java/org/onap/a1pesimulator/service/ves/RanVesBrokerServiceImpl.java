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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.fileready.RanPeriodicEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RanVesBrokerServiceImpl implements RanVesBrokerService {

    private final RanVesDataProvider vesDataProvider;

    private final RanVesHolder vesHolder;

    public RanVesBrokerServiceImpl(RanVesDataProvider vesDataProvider, RanVesHolder vesHolder) {
        this.vesDataProvider = vesDataProvider;
        this.vesHolder = vesHolder;
    }

    @Override
    public Map<String, RanPeriodicEvent> getPeriodicEventsCache() {
        return vesHolder.getPeriodicEventsCache();
    }

    @Override
    public ResponseEntity<String> startSendingVesEvents(String identifier, VesEvent vesEvent, Integer interval, ReportingMethodEnum reportingMethod) {
        enrichWithIdentifier(identifier, vesEvent);
        ResponseEntity<String> response = vesHolder.startSendingVesEvents(identifier, vesEvent, interval, reportingMethod);
        return ResponseEntity.accepted().body(response.getBody());
    }

    @Override
    public VesEvent startSendingFailureVesEvents(String identifier, ReportingMethodEnum reportingMethod) {

        var vesEvent = vesDataProvider.getFailurePmVesEvent();

        enrichWithIdentifier(identifier, vesEvent);
        vesHolder.startSendingFailureVesEvents(identifier, vesEvent, reportingMethod);
        return vesEvent;
    }

    @Override
    public Optional<RanPeriodicEvent> stopSendingVesEvents(String identifier) {
        return vesHolder.stopSendingVesEvents(identifier);
    }

    @Override
    public Collection<String> getEnabledEventElementIdentifiers() {
        return vesHolder.getEnabledEventElementIdentifiers();
    }

    @Override
    public RanPeriodicEvent getPeriodicEvent(String identifier) {
        return vesHolder.getPeriodicEventForCell(identifier);
    }

    @Override
    public VesEvent getGlobalPmVesStructure() {
        return vesDataProvider.getPmVesEvent();
    }

    @Override
    public void setGlobalPmVesStructure(VesEvent event) {
        vesDataProvider.setPmVesEvent(event);
    }

    @Override
    public Integer getGlobalVesInterval() {
        return vesDataProvider.getRegularVesInterval();
    }

    @Override
    public void setGlobalVesInterval(Integer interval) {
        vesDataProvider.setInterval(interval);
    }

    @Override
    public String getGlobalReportingMethod() {
        return vesDataProvider.getReportingMethod();
    }

    @Override
    public void setGlobalReportingMethod(String reportingMethod) {
        vesDataProvider.setReportingMethod(reportingMethod);
    }

    private void enrichWithIdentifier(String identifier, VesEvent event) {
        if (event.getMeasurementFields() == null || event.getMeasurementFields().getAdditionalMeasurements() == null) {
            return;
        }
        Collection<AdditionalMeasurement> additionalMeasurements =
                event.getMeasurementFields().getAdditionalMeasurements();
        Optional<AdditionalMeasurement> identityOpt = additionalMeasurements.stream()
                .filter(m -> Constants.MEASUREMENT_FIELD_IDENTIFIER
                        .equalsIgnoreCase(m.getName()))
                .findAny();
        if (identityOpt.isPresent()) {
            identityOpt.get().getHashMap().put(Constants.MEASUREMENT_FIELD_IDENTIFIER, identifier);
        } else {
            var measurement = new AdditionalMeasurement();
            measurement.setName(Constants.MEASUREMENT_FIELD_IDENTIFIER);
            measurement.setHashMap(Collections.singletonMap(Constants.MEASUREMENT_FIELD_VALUE, identifier));
            additionalMeasurements.add(measurement);
        }
    }

}
