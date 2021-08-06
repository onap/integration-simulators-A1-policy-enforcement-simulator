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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.CellList.Cell;
import org.onap.a1pesimulator.data.cell.CellList.CellData;
import org.onap.a1pesimulator.exception.CellNotFoundException;
import org.onap.a1pesimulator.util.TopologyReader;
import org.springframework.stereotype.Service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Service
public class RanCellsHolder {

    private Map<String, CellDetails> cellDetailsById;
    private final Collection<CellInFailureMode> cellsInFailureMode = new HashSet<>();

    private final TopologyReader topologyReader;

    public RanCellsHolder(TopologyReader topologyReader) {
        this.topologyReader = topologyReader;
        refresh();
    }

    public Set<String> getCellIds() {
        return cellDetailsById.keySet();
    }

    public CellDetails getCellById(String id) {
        if (!cellDetailsById.containsKey(id)) {
            throw new CellNotFoundException(MessageFormat.format("Cell not found: {0}", id));
        }
        return cellDetailsById.get(id);
    }

    public Collection<CellDetails> getCellDetailsList() {
        return cellDetailsById.values();
    }

    public Collection<CellDetails> getAllCells() {
        return cellDetailsById.values();
    }

    public void markCellInFailure(String id) {
        cellsInFailureMode.add(CellInFailureMode.builder().id(id).build());
    }

    public boolean isInFailureMode(String id) {
        return cellsInFailureMode.stream().anyMatch(byIdPredicate(id));
    }

    public void unmarkCellInFailure(String id) {
        cellsInFailureMode.removeIf(byIdPredicate(id));
    }

    public Optional<CellInFailureMode> getCellsInFailureMode(String id) {
        return cellsInFailureMode.stream().filter(byIdPredicate(id)).findFirst();
    }

    @Getter
    @Builder
    public static class CellInFailureMode {

        private final String id;
        @Setter
        private Long sleepingModeDetectedTime;
    }

    public void refresh() {
        List<CellData> cellDatas = topologyReader.loadCellTopology().getCellList();
        cellDetailsById = cellDatas.stream().collect(Collectors.toMap(cellData -> cellData.getCell().getNodeId(),
                this::toCellDetails, throwingMerger(), TreeMap::new));
    }

    public boolean hasChanged() {
        return topologyReader.topologyCellHasChanged();
    }

    private <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private CellDetails toCellDetails(CellData data) {
        Cell cell = data.getCell();
        return CellDetails.builder().id(cell.getNodeId()).latitude(cell.getLatitude()).longitude(cell.getLongitude())
                       .build();
    }

    public static Predicate<CellInFailureMode> byIdPredicate(String id) {
        return cell -> cell.getId().equals(id);
    }
}
