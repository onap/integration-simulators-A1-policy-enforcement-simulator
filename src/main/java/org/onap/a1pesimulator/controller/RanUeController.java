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
import org.onap.a1pesimulator.data.ue.RanUserEquipment;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.ue.RanUeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"${restapi.version}/ran/ues"})
public class RanUeController {

    private final RanUeService ranUeService;

    public RanUeController(RanUeService ranUeService) {
        this.ranUeService = ranUeService;
    }

    @GetMapping
    public ResponseEntity<RanUserEquipment> getUes() {
        return ResponseEntity.ok(ranUeService.getUes());
    }

    @GetMapping(value = "/{identifier}")
    public ResponseEntity<UserEquipment> getUeById(final @PathVariable String identifier) {

        Optional<UserEquipment> userEquipment = ranUeService.getUserEquipment(identifier);
        return userEquipment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

    }
}
