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

package org.onap.a1pesimulator.service.cell;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.a1pesimulator.data.Topology;
import org.onap.a1pesimulator.data.cell.Cell;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.CellWithStatus;
import org.onap.a1pesimulator.data.cell.RanCell;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.service.report.RanVesHolder;
import org.springframework.stereotype.Service;

@Service
public class RanCellServiceImpl implements RanCellService {

    private final RanCellsHolder ranCellsHolder;
    private final RanUeHolder ueHolder;
    private final RanVesHolder vesHolder;

    public RanCellServiceImpl(RanCellsHolder ranCellsHolder, RanUeHolder ueHolder, RanVesHolder vesHolder) {
        this.ranCellsHolder = ranCellsHolder;
        this.ueHolder = ueHolder;
        this.vesHolder = vesHolder;
    }

    @Override
    public Set<String> getCellIds() {
        return ranCellsHolder.getCellIds();
    }

    @Override
    public CellDetails getCellById(String id) {
        CellDetails cellDetails = ranCellsHolder.getCellById(id);
        cellDetails.setConnectedUserEquipments(getConnectedUserEquipments(cellDetails.getId()));
        return cellDetails;
    }

    @Override
    public RanCell getCells() {
        Collection<CellDetails> cellDetails = ranCellsHolder.getAllCells();
        cellDetails.forEach(cell -> cell.setConnectedUserEquipments(getConnectedUserEquipments(cell.getId())));
        return new RanCell(cellDetails, cellDetails.size());
    }

    @Override
    public Topology getTopology() {
        Collection<CellDetails> cellList = ranCellsHolder.getCellDetailsList();
        cellList.forEach(cell -> cell.setConnectedUserEquipments(getConnectedUserEquipments(cell.getId())));
        return Topology.builder().cells(cellList).userEquipments(ueHolder.getUserEquipments()).build();
    }

    private Set<String> getConnectedUserEquipments(String cellId) {
        Collection<UserEquipment> cellUes = ueHolder.getUserEquipmentsConnectedToCell(cellId);
        return cellUes.stream().map(UserEquipment::getId).collect(Collectors.toSet());
    }

    @Override
    public void failure(String id) {
        ranCellsHolder.markCellInFailure(id);
    }

    @Override
    public void recoverFromFailure(String id) {
        ranCellsHolder.unmarkCellInFailure(id);
    }

    @Override
    public Collection<CellWithStatus> getAllCellsWithStatus() {
        return ranCellsHolder.getAllCells().stream().map(this::wrapCellWithStatus).collect(Collectors.toList());
    }

    private CellWithStatus wrapCellWithStatus(CellDetails cell) {
        Cell c = Cell.builder().identifier(cell.getId()).build();
        return CellWithStatus.builder().cell(c).failureMode(ranCellsHolder.isInFailureMode(cell.getId()))
                       .vesEnabled(vesHolder.isEventEnabled(cell.getId())).state(cell.getCurrentState()).build();
    }
}
