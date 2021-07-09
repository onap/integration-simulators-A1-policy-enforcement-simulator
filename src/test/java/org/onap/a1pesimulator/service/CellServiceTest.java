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

package org.onap.a1pesimulator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.a1pesimulator.TestHelpers.CELL_ARRAY_SIZE;
import static org.onap.a1pesimulator.TestHelpers.CELL_PREFIX;
import static org.onap.a1pesimulator.TestHelpers.FIRST_CELL_ID;
import static org.onap.a1pesimulator.TestHelpers.UE_ARRAY_SIZE;
import static org.onap.a1pesimulator.TestHelpers.checkFirstCell;
import static org.onap.a1pesimulator.TestHelpers.checkFirstCellWithStatus;
import static org.onap.a1pesimulator.TestHelpers.checkFirstUserEquipment;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.a1pesimulator.data.Topology;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.CellWithStatus;
import org.onap.a1pesimulator.service.cell.RanCellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CellServiceTest {

    @Autowired
    private RanCellService cellService;

    @Test
    public void testGetTopology() {
        Topology topology = cellService.getTopology();

        assertNotNull(topology);

        assertEquals(CELL_ARRAY_SIZE, topology.getCells().size());
        assertEquals(UE_ARRAY_SIZE, topology.getUserEquipments().size());

        checkFirstCell(topology.getCells().iterator().next());
        checkFirstUserEquipment(topology.getUserEquipments().iterator().next());
    }

    @Test
    public void testGetCellById() {
        CellDetails cellDetails = cellService.getCellById(FIRST_CELL_ID);
        checkFirstCell(cellDetails);
    }

    @Test
    public void testGetCellIds() {
        Set<String> ids = cellService.getCellIds();
        assertEquals(CELL_ARRAY_SIZE, ids.size());
        ids.forEach(id -> assertTrue(id.startsWith(CELL_PREFIX)));
    }

    @Test
    public void testGetCellWithStatus() {
        Collection<CellWithStatus> cells = cellService.getAllCellsWithStatus();

        assertEquals(CELL_ARRAY_SIZE, cells.size());
        checkFirstCellWithStatus(cells.iterator().next());
    }
}
