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

package org.onap.a1pesimulator.service.ue;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.cell.RanCellsHolder;
import org.onap.a1pesimulator.service.distance.DistanceService;
import org.onap.a1pesimulator.util.TopologyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RanUeHolder {

    private static final Logger log = LoggerFactory.getLogger(RanUeHolder.class);

    private Map<String, UserEquipment> userEquipmentsById;

    private final TopologyReader topologyReader;
    private final DistanceService distanceService;

    private final RanCellsHolder ranCellsHolder;

    public RanUeHolder(TopologyReader topologyReader, DistanceService distanceService, RanCellsHolder ranCellsHolder) {
        this.topologyReader = topologyReader;
        this.distanceService = distanceService;
        this.ranCellsHolder = ranCellsHolder;
        refresh();
    }

    public Collection<UserEquipment> getUserEquipments() {
        return userEquipmentsById.values();
    }

    public Collection<UserEquipment> getUserEquipmentsConnectedToCell(String cellId) {
        return userEquipmentsById.values().stream().filter(ue -> cellId.equalsIgnoreCase(ue.getCellId()))
                       .collect(Collectors.toList());
    }

    public Optional<UserEquipment> getUserEquipment(String id) {
        return userEquipmentsById.values().stream().filter(ue -> id.equalsIgnoreCase(ue.getId())).findAny();
    }

    public void refresh() {
        Collection<UserEquipment> ues = topologyReader.loadUeTopology();
        userEquipmentsById = ues.stream().filter(this::validate)
                                     .collect(Collectors.toMap(UserEquipment::getId, Function.identity()));
    }

    public boolean hasChanged() {
        return topologyReader.topologyUeHasChanged();
    }

    private boolean validate(UserEquipment ue) {
        CellDetails cell = ranCellsHolder.getCellById(ue.getCellId());
        boolean inRange = distanceService.isInRange(cell, ue);
        if (!inRange) {
            log.warn("UE {} is not in range of preferred cell {}", ue.getId(), cell.getId());
        }
        return true;
    }
}
