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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.a1pesimulator.TestHelpers.FIRST_CELL_CONNECTED_UE_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_CELL_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_CELL_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_HANDOVER_CELL;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_ID;
import static org.onap.a1pesimulator.TestHelpers.UE_ARRAY_SIZE;
import static org.onap.a1pesimulator.TestHelpers.checkFirstUserEquipment;

import java.util.Collection;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.ue.RanUeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class UeServiceTest {

    @Autowired
    private RanUeService ueService;

    @Test
    public void testGetUes() {
        Collection<UserEquipment> userEquipments = ueService.getUserEquipments();

        assertNotNull(userEquipments);
        assertEquals(UE_ARRAY_SIZE, userEquipments.size());

        UserEquipment userEquipment = userEquipments.iterator().next();
        checkFirstUserEquipment(userEquipment);
    }

    @Test
    public void testGetUeConnectedToCell() {
        Collection<UserEquipment> userEquipments = ueService.getUserEquipmentsConnectedToCell(FIRST_CELL_ID);

        assertNotNull(userEquipments);
        assertEquals(1, userEquipments.size());

        UserEquipment userEquipment = userEquipments.iterator().next();
        assertEquals(FIRST_CELL_CONNECTED_UE_ID, userEquipment.getId());
    }

    @Test
    public void testGetUeByID() {
        Optional<UserEquipment> userEquipmentOpt = ueService.getUserEquipment(FIRST_UE_ID);

        assertNotNull(userEquipmentOpt);
        assertTrue(userEquipmentOpt.isPresent());

        UserEquipment userEquipment = userEquipmentOpt.get();
        checkFirstUserEquipment(userEquipment);
    }

    @Test
    public void testGetUeByNotCorrectID() {
        Optional<UserEquipment> userEquipmentOpt = ueService.getUserEquipment("BAD_ID");

        assertFalse(userEquipmentOpt.isPresent());
        assertNull(userEquipmentOpt.orElse(null));
    }

    @Test
    public void testHandoverFlow() {
        boolean canHandover = ueService.canHandover(FIRST_UE_ID, FIRST_UE_HANDOVER_CELL);
        assertTrue(canHandover);

        ueService.handover(FIRST_UE_ID, FIRST_UE_HANDOVER_CELL);

        UserEquipment userEquipment = ueService.getUserEquipment(FIRST_UE_ID).orElse(null);
        assertNotNull(userEquipment);
        assertEquals(FIRST_UE_HANDOVER_CELL, userEquipment.getCellId());

        ueService.handover(FIRST_UE_ID, FIRST_UE_CELL_ID);
    }

    @Test
    public void testCantHandoverFlow() {
        boolean canHandover = ueService.canHandover(FIRST_UE_ID, "BAD_CELL_ID");
        assertFalse(canHandover);
    }
}
