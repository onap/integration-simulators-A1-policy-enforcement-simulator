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

package org.onap.a1pesimulator.util;

import org.onap.a1pesimulator.service.cell.RanCellsHolder;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ItemsRefresher {

    private final RanCellsHolder cellsHolder;
    private final RanUeHolder ueHolder;

    public ItemsRefresher(final RanCellsHolder cellsHolder, final RanUeHolder ueHolder) {
        this.cellsHolder = cellsHolder;
        this.ueHolder = ueHolder;
    }

    @Scheduled(fixedRateString = "${refresher.fixed.rate.ms}")
    public void refresh() {
        if (cellsHolder.hasChanged()) {
            cellsHolder.refresh();
        }

        if (ueHolder.hasChanged()) {
            ueHolder.refresh();
        }
    }
}
