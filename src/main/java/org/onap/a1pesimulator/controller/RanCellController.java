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

package org.onap.a1pesimulator.controller;

import java.util.Optional;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.RanCell;
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
import org.onap.a1pesimulator.service.cell.RanCellService;
import org.onap.a1pesimulator.service.cell.RanCellStateService;
import org.onap.a1pesimulator.service.ves.RanVesBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"${restapi.version}/ran/cells"})
public class RanCellController {

    private static final Logger log = LoggerFactory.getLogger(RanCellController.class);
    private final RanCellService ranCellService;
    private final RanCellStateService ranCellStateService;
    private final RanVesBrokerService ranVesBrokerService;

    public RanCellController(RanCellService ranCellService, RanCellStateService ranCellStateService,
            RanVesBrokerService ranVesBrokerService) {
        this.ranCellService = ranCellService;
        this.ranCellStateService = ranCellStateService;
        this.ranVesBrokerService = ranVesBrokerService;
    }

    @GetMapping
    public ResponseEntity<RanCell> getCells() {
        return ResponseEntity.ok(ranCellService.getCells());
    }

    @GetMapping(value = "/{identifier}")
    public ResponseEntity<CellDetails> getCellById(final @PathVariable String identifier) {

        if (!ranCellService.getCellIds().contains(identifier)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ranCellService.getCellById(identifier));
    }

    @PostMapping(value = "/{identifier}/startFailure")
    public ResponseEntity<String> startSendingFailureVesEvents(final @PathVariable String identifier) {

        ranCellService.failure(identifier);
        ranVesBrokerService.startSendingFailureVesEvents(identifier);
        ranCellStateService.failingState(identifier);

        return ResponseEntity.accepted().body("Failure VES Event sending started");
    }

    @PostMapping(value = "/{identifier}/stopFailure")
    public ResponseEntity<Void> stopSendingFailureVesEvents(final @PathVariable String identifier) {

        ranCellService.recoverFromFailure(identifier);

        Optional<RanPeriodicVesEvent> vesEvent = ranVesBrokerService.stopSendingVesEvents(identifier);

        if (!vesEvent.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ranCellStateService.stopState(identifier);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/{identifier}/start")
    public ResponseEntity<String> startSendingVesEvents(final @RequestBody Optional<Event> vesEventOpt,
            final @PathVariable String identifier, final @RequestParam(required = false) Integer interval) {
        log.info("Start sending ves events every {} seconds for {} ", getInterval(interval), identifier);

        Event vesEvent = vesEventOpt.orElse(ranVesBrokerService.getGlobalPmVesStructure());

        ResponseEntity<String> responseEntity =
                ranVesBrokerService.startSendingVesEvents(identifier, vesEvent, getInterval(interval));
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity;
        }

        ranCellStateService.activateState(identifier);
        return responseEntity;
    }

    @PostMapping(value = "/{identifier}/stop")
    public ResponseEntity<Void> stopSendingVesEvents(final @PathVariable String identifier) {
        log.info("Stop sending custom ves events for {}", identifier);
        Optional<RanPeriodicVesEvent> vesEvent = ranVesBrokerService.stopSendingVesEvents(identifier);
        if (!vesEvent.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ranCellStateService.stopState(identifier);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/{identifier}/eventStructure")
    public ResponseEntity<Event> getVesEventStructure(final @PathVariable String identifier) {
        if (!ranVesBrokerService.getEnabledEventElementIdentifiers().contains(identifier)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ranVesBrokerService.getEventStructure(identifier));
    }

    private Integer getInterval(Integer requested) {
        if (requested == null) {
            return ranVesBrokerService.getGlobalVesInterval();
        }
        return requested;
    }
}
