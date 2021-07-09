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

import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.RanCell;
import org.onap.a1pesimulator.data.fileready.RanPeriodicFileReadyEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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

    @ApiOperation("Start sending failure VES events for specific cell")
    @PostMapping(value = "/{identifier}/startFailure")
    public ResponseEntity<String> startSendingFailureVesEvents(@ApiParam(value = "Cell Id") final @PathVariable String identifier,
            @ApiParam(value = "Reporting Method", defaultValue = "FILE_READY", required = true) final @RequestParam() ReportingMethodEnum reportingMethod) {

        ranCellService.failure(identifier);
        ranVesBrokerService.startSendingFailureVesEvents(identifier, reportingMethod);
        ranCellStateService.failingState(identifier);

        return ResponseEntity.accepted().body("Failure VES Event sending started");
    }

    @ApiOperation("Stop sending failure VES events for specific cell")
    @PostMapping(value = "/{identifier}/stopFailure")
    public ResponseEntity<Void> stopSendingFailureVesEvents(@ApiParam(value = "Cell Id") final @PathVariable String identifier) {

        ranCellService.recoverFromFailure(identifier);

        Optional<RanPeriodicFileReadyEvent> vesEvent = ranVesBrokerService.stopSendingVesEvents(identifier);

        if (!vesEvent.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ranCellStateService.stopState(identifier);
        return ResponseEntity.accepted().build();
    }

    @ApiOperation("Start sending normal VES events for specific cell and  in specific granularity period")
    @PostMapping(value = "/{identifier}/start")
    public ResponseEntity<String> startSendingVesEvents(@ApiParam(value = "Standard Measurement Event JSON") final @RequestBody Optional<VesEvent> vesEventOpt,
            @ApiParam(value = "Cell Id") final @PathVariable String identifier,
            @ApiParam(value = "Granularity period in seconds", example = "60") final @RequestParam(required = false) Integer interval,
            @ApiParam(value = "Reporting Method", defaultValue = "FILE_READY", required = true) final @RequestParam() ReportingMethodEnum reportingMethod) {
        log.info("Start sending ves events every {} seconds for {} ", getInterval(interval), identifier);

        VesEvent vesEvent = vesEventOpt.orElse(ranVesBrokerService.getGlobalPmVesStructure());

        ResponseEntity<String> responseEntity =
                ranVesBrokerService.startSendingVesEvents(identifier, vesEvent, getInterval(interval), reportingMethod);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity;
        }

        ranCellStateService.activateState(identifier);
        return responseEntity;
    }

    @ApiOperation("Stop sending normal VES events for specific cell")
    @PostMapping(value = "/{identifier}/stop")
    public ResponseEntity<Void> stopSendingVesEvents(@ApiParam(value = "Cell Id") final @PathVariable String identifier) {
        log.info("Stop sending custom ves events for {}", identifier);
        Optional<RanPeriodicFileReadyEvent> vesEvent = ranVesBrokerService.stopSendingVesEvents(identifier);
        if (!vesEvent.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ranCellStateService.stopState(identifier);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/{identifier}/eventStructure")
    public ResponseEntity<VesEvent> getVesEventStructure(final @PathVariable String identifier) {
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
