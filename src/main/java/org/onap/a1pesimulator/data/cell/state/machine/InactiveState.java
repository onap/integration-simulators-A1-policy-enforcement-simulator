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

package org.onap.a1pesimulator.data.cell.state.machine;

import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.state.CellStateEnum;
import org.onap.a1pesimulator.data.cell.state.CellStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InactiveState extends CellStateMachine {

    private static final Logger log = LoggerFactory.getLogger(InactiveState.class);

    public InactiveState() {
        super(CellStateEnum.INACTIVE);
    }

    @Override
    public void next(CellDetails cell) {
        cell.setCellStateMachine(new ActiveState());
    }

    @Override
    public void prev(CellDetails cell) {
        log.info("YOU ARE IN THE INACTIVE STATE, PREVIOUS STATE ISN'T AVAILABLE");
    }
}
