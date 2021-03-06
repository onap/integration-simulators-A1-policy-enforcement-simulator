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

import org.onap.a1pesimulator.data.ves.GlobalVesConfiguration;
import org.onap.a1pesimulator.service.report.RanReportsBrokerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"${restapi.version}/ran/globalPMConfig"})
public class RanEventConfigureController {

    private final RanReportsBrokerService ranReportsBrokerService;

    public RanEventConfigureController(RanReportsBrokerService ranReportsBrokerService) {
        this.ranReportsBrokerService = ranReportsBrokerService;
    }

    @GetMapping
    public ResponseEntity<GlobalVesConfiguration> getGlobalPMConfig() {
        GlobalVesConfiguration config = new GlobalVesConfiguration(ranReportsBrokerService.getGlobalVesInterval(),
                ranReportsBrokerService.getGlobalPmVesStructure(), ranReportsBrokerService.getGlobalReportingMethod());
        return ResponseEntity.ok(config);
    }

    @PostMapping
    public ResponseEntity<Void> setGlobalPMConfig(final @RequestBody GlobalVesConfiguration config) {
        ranReportsBrokerService.setGlobalPmVesStructure(config.getEvent());
        ranReportsBrokerService.setGlobalVesInterval(config.getInterval());
        ranReportsBrokerService.setGlobalReportingMethod(config.getReportingMethod());
        return ResponseEntity.ok().build();
    }
}
