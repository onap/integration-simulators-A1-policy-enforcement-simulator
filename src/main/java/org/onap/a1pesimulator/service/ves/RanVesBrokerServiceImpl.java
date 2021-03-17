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
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
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
    public Map<String, RanPeriodicVesEvent> getPeriodicEventsCache() {
        return vesHolder.getPeriodicEventsCache();
    }

    @Override
    public ResponseEntity<String> startSendingVesEvents(String identifier, Event vesEvent, Integer interval) {

        enrichWithIdentifier(identifier, vesEvent);
        vesHolder.startSendingVesEvents(identifier, vesEvent, interval);

        return ResponseEntity.accepted().body("VES Event sending started");
    }

    @Override
    public Event startSendingFailureVesEvents(String identifier) {

        Event vesEvent = vesDataProvider.getFailurePmVesEvent();

        enrichWithIdentifier(identifier, vesEvent);
        vesHolder.startSendingFailureVesEvents(identifier, vesEvent);
        return vesEvent;
    }

    @Override
    public Optional<RanPeriodicVesEvent> stopSendingVesEvents(String identifier) {
        return vesHolder.stopSendingVesEvents(identifier);
    }

    @Override
    public Collection<String> getEnabledEventElementIdentifiers() {
        return vesHolder.getEnabledEventElementIdentifiers();
    }

    @Override
    public Event getEventStructure(String identifier) {
        return vesHolder.getEventStructure(identifier);
    }

    @Override
    public Event getGlobalPmVesStructure() {
        return vesDataProvider.getPmVesEvent();
    }

    @Override
    public void setGlobalPmVesStructure(Event event) {
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

    private void enrichWithIdentifier(String identifier, Event event) {
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
            AdditionalMeasurement measurement = new AdditionalMeasurement();
            measurement.setName(Constants.MEASUREMENT_FIELD_IDENTIFIER);
            measurement.setHashMap(Collections.singletonMap(Constants.MEASUREMENT_FIELD_VALUE, identifier));
            additionalMeasurements.add(measurement);
        }
    }

}
