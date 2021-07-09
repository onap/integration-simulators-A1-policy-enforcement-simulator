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
import java.util.Map;
import java.util.Optional;

import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.fileready.RanPeriodicFileReadyEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.springframework.http.ResponseEntity;

public interface RanVesBrokerService {

    ResponseEntity<String> startSendingVesEvents(String identifier, VesEvent vesEvent, Integer interval, ReportingMethodEnum reportingMethods);

    Optional<RanPeriodicFileReadyEvent> stopSendingVesEvents(String identifier);

    Map<String, RanPeriodicFileReadyEvent> getPeriodicEventsCache();

    Collection<String> getEnabledEventElementIdentifiers();

    VesEvent getEventStructure(String identifier);

    VesEvent startSendingFailureVesEvents(String identifier, ReportingMethodEnum reportingMethods);

    VesEvent getGlobalPmVesStructure();

    void setGlobalPmVesStructure(VesEvent event);

    Integer getGlobalVesInterval();

    void setGlobalVesInterval(Integer interval);
}
