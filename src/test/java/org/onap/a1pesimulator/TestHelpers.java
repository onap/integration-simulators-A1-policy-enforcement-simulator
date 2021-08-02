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

package org.onap.a1pesimulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.CellWithStatus;
import org.onap.a1pesimulator.data.ue.UserEquipment;

public class TestHelpers {

    public static final int CELL_ARRAY_SIZE = 5;
    public static final int UE_ARRAY_SIZE = 3;

    public static final String CELL_PREFIX = "Chn00";

    public static final String FIRST_CELL_ID = "Chn0000";
    public static final double FIRST_CELL_LATITUDE = 50.11;
    public static final double FIRST_CELL_LONGITUDE = 19.98;
    public static final int FIRST_CELL_CONNECTED_UE_SIZE = 1;
    public static final String FIRST_CELL_CONNECTED_UE_ID = "mobile_samsung_s10";

    public static final String FIRST_UE_ID = "emergency_police_111";
    public static final double FIRST_UE_LATITUDE = 50.035;
    public static final double FIRST_UE_LONGITUDE = 19.97;
    public static final String FIRST_UE_CELL_ID = "Chn0002";

    public static final String FIRST_UE_HANDOVER_CELL = "Chn0004";

    public static void checkFirstCell(CellDetails cellDetails) {
        assertNotNull(cellDetails);

        assertEquals(FIRST_CELL_ID, cellDetails.getId());
        assertEquals(FIRST_CELL_LATITUDE, cellDetails.getLatitude());
        assertEquals(FIRST_CELL_LONGITUDE, cellDetails.getLongitude());
        assertEquals(FIRST_CELL_CONNECTED_UE_SIZE, cellDetails.getConnectedUserEquipments().size());
        assertEquals(FIRST_CELL_CONNECTED_UE_ID, cellDetails.getConnectedUserEquipments().iterator().next());
    }

    public static void checkFirstUserEquipment(UserEquipment userEquipment) {
        assertNotNull(userEquipment);

        assertEquals(FIRST_UE_ID, userEquipment.getId());
        assertEquals(FIRST_UE_LATITUDE, userEquipment.getLatitude());
        assertEquals(FIRST_UE_LONGITUDE, userEquipment.getLongitude());
        assertEquals(FIRST_UE_CELL_ID, userEquipment.getCellId());
    }

    public static void checkFirstCellWithStatus(CellWithStatus cellWithStatus) {
        assertNotNull(cellWithStatus);

        assertEquals(FIRST_CELL_ID, cellWithStatus.getCell().getIdentifier());
        assertFalse(cellWithStatus.isFailureMode());
        assertFalse(cellWithStatus.isVesEnabled());
    }

    public static void deleteTempFiles(List<File> files) {
        Optional.ofNullable(files).orElse(Collections.emptyList()).forEach(file -> {
            try {
                if (Files.exists(file.toPath())) {
                    Files.delete(file.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private TestHelpers() {
    }
}
