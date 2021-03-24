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

package org.onap.a1pesimulator.service.distance;

import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.util.DistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private Double cellRange;

    public DistanceService(@Value("${topology.cell.range}") Double cellRange) {
        this.cellRange = cellRange;
    }

    public boolean isInRange(CellDetails cell, UserEquipment ue) {
        return DistanceCalculator
                       .isInRange(cell.getLatitude(), cell.getLongitude(), ue.getLatitude(), ue.getLongitude(),
                               cellRange);
    }
}
