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

import static org.onap.a1pesimulator.TestHelpers.FIRST_CELL_CONNECTED_UE_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_CELL_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_HANDOVER_CELL;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.ue.RanUeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RanUeServiceImplTest {

    @Autowired
    private RanUeServiceImpl ranUeService;

    @Test
    public void testHandover() {
        // when
        ranUeService.handover(FIRST_CELL_CONNECTED_UE_ID, FIRST_UE_HANDOVER_CELL);

        // then
        Optional<UserEquipment> optUserEquipment = ranUeService.getUserEquipment(FIRST_CELL_CONNECTED_UE_ID);

        Assert.assertTrue(optUserEquipment.isPresent());
        UserEquipment userEquipment = optUserEquipment.get();
        Assert.assertEquals(FIRST_UE_HANDOVER_CELL, userEquipment.getCellId());

        // cleanup
        userEquipment.setCellId(FIRST_CELL_ID);
    }
}
