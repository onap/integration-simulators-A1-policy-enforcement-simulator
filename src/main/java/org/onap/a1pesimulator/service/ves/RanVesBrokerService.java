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
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
import org.springframework.http.ResponseEntity;

public interface RanVesBrokerService {

    ResponseEntity<String> startSendingVesEvents(String identifier, Event vesEvent, Integer interval);

    Optional<RanPeriodicVesEvent> stopSendingVesEvents(String identifier);

    Map<String, RanPeriodicVesEvent> getPeriodicEventsCache();

    Collection<String> getEnabledEventElementIdentifiers();

    Event getEventStructure(String identifier);

    Event startSendingFailureVesEvents(String identifier);

    Event getGlobalPmVesStructure();

    void setGlobalPmVesStructure(Event event);

    Integer getGlobalVesInterval();

    void setGlobalVesInterval(Integer interval);
}