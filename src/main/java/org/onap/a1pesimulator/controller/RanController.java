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

import org.onap.a1pesimulator.data.Topology;
import org.onap.a1pesimulator.service.cell.RanCellService;
import org.onap.a1pesimulator.util.ItemsRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"${restapi.version}/ran"})
public class RanController {

    private static final Logger log = LoggerFactory.getLogger(RanController.class);
    private final RanCellService ranCellService;
    private final ItemsRefresher refresher;

    public RanController(RanCellService ranCellService, final ItemsRefresher refresher) {
        this.ranCellService = ranCellService;
        this.refresher = refresher;
    }

    @GetMapping
    public ResponseEntity<Topology> getRan() {
        return ResponseEntity.ok(ranCellService.getTopology());
    }

    @GetMapping(value = "/refresh")
    public ResponseEntity<Void> refreshRan() {
        refresher.refresh();
        log.info("Refreshed the items on request");
        return ResponseEntity.ok().build();
    }
}
