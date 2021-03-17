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
import java.util.Optional;
import java.util.stream.Collectors;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.ue.RanUserEquipment;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.cell.RanCellsHolder;
import org.onap.a1pesimulator.service.distance.DistanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RanUeServiceImpl implements RanUeService {

    private static final Logger log = LoggerFactory.getLogger(RanUeServiceImpl.class);

    private final RanUeHolder ueHolder;
    private final RanCellsHolder ranCellsHolder;
    private final DistanceService distanceService;

    public RanUeServiceImpl(RanUeHolder ueHolder, RanCellsHolder ranCellsHolder, DistanceService distanceService) {
        this.ueHolder = ueHolder;
        this.ranCellsHolder = ranCellsHolder;
        this.distanceService = distanceService;
    }

    @Override
    public Collection<UserEquipment> getUserEquipments() {
        return ueHolder.getUserEquipments();
    }

    @Override
    public RanUserEquipment getUes() {
        Collection<UserEquipment> uesCollection = ueHolder.getUserEquipments();
        return new RanUserEquipment(uesCollection, uesCollection.size());
    }

    @Override
    public Collection<UserEquipment> getUserEquipmentsConnectedToCell(String cellId) {
        return ueHolder.getUserEquipmentsConnectedToCell(cellId);
    }

    @Override
    public Optional<UserEquipment> getUserEquipment(String id) {
        Optional<UserEquipment> userEquipment = ueHolder.getUserEquipment(id);
        userEquipment.ifPresent(ue -> ue.setCellsInRange(getCellsIdsInRange(ue)));
        return userEquipment;
    }

    @Override
    public void handover(String ueId, String cellId) {
        Optional<UserEquipment> userEquipment = getUserEquipment(ueId);
        if (!userEquipment.isPresent()) {
            log.warn("Cannot handover ue {} to cell {}, because ue does not exist!", ueId, cellId);
            return;
        }
        userEquipment.get().setCellId(cellId);
    }

    @Override
    public boolean canHandover(String ueId, String cellId) {
        Optional<UserEquipment> userEquipment = getUserEquipment(ueId);
        return userEquipment.map(equipment -> equipment.getCellsInRange().stream().anyMatch(cellId::equalsIgnoreCase))
                       .orElse(false);
    }

    private Collection<String> getCellsIdsInRange(UserEquipment ue) {
        return ranCellsHolder.getAllCells().stream().filter(cell -> distanceService.isInRange(cell, ue))
                       .map(CellDetails::getId).collect(Collectors.toList());
    }
}
