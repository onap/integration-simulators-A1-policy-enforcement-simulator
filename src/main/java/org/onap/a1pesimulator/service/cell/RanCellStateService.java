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

import java.util.Optional;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.state.CellStateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RanCellStateService {

    private static final Logger log = LoggerFactory.getLogger(RanCellStateService.class);

    private final RanCellsHolder cellsHolder;
    private final SimpMessagingTemplate messagingTemplate;

    public static final String TOPIC_CELL = "/topic/cellStatus";

    public RanCellStateService(RanCellsHolder cellsHolder, SimpMessagingTemplate messagingTemplate) {
        this.cellsHolder = cellsHolder;
        this.messagingTemplate = messagingTemplate;
    }

    public boolean activateState(String identifier) {
        Optional<CellDetails> cellDetails = getCell(identifier);
        if (cellExist(cellDetails, identifier, "Activate")) {
            boolean changed = nextStateIfPossible(cellDetails.get(), CellStateEnum.INACTIVE);
            if (changed) {
                sendCellNotification(cellDetails.get());
                return true;
            }
        }

        return false;
    }

    public boolean failingState(String identifier) {
        Optional<CellDetails> cellDetails = getCell(identifier);
        if (cellExist(cellDetails, identifier, "Failing")) {
            boolean changed = nextStateIfPossible(cellDetails.get(), CellStateEnum.ACTIVE);
            if (changed) {
                sendCellNotification(cellDetails.get());
                return true;
            }
        }

        return false;
    }

    public boolean stopState(String identifier) {
        Optional<CellDetails> cellDetails = getCell(identifier);
        if (cellExist(cellDetails, identifier, "Stop")) {
            boolean changed = previousStateIfPossible(cellDetails.get());
            if (changed) {
                sendCellNotification(cellDetails.get());
                return true;
            }
        }

        return false;
    }

    private boolean cellExist(Optional<CellDetails> cellDetails, String identifier, String actionName) {
        if (cellDetails.isEmpty()) {
            log.info("Cell not found for {} identifier! '{}' action won't be executed!", identifier, actionName);
            return false;
        }

        return true;
    }

    private boolean previousStateIfPossible(CellDetails cell) {

        CellStateEnum state = cell.getCellStateMachine().getState();
        if (state == CellStateEnum.SLEEPING || state == CellStateEnum.GOING_TO_SLEEP || state == CellStateEnum.ACTIVE) {
            cell.previousState();
        } else {
            log.info("Cell {} is in {} state! The changing of the state isn't allowed."
                             + "Supported states are: GOING_TO_SLEEP, SLEEPING, ACTIVE.", cell.getId(),
                    cell.getCellStateMachine().getState().value);
            return false;
        }

        return true;
    }

    private boolean nextStateIfPossible(CellDetails cellDetails, CellStateEnum shouldBe) {

        if (cellDetails.getCellStateMachine().getState() == shouldBe) {
            cellDetails.nextState();
        } else {
            log.info(
                    "Cell {} is in {} state. The changing of the state isn't allowed. " + "The supported state is: {}!",
                    cellDetails.getId(), cellDetails.getCellStateMachine().getState().value, shouldBe.value);
            return false;
        }

        return true;
    }

    private Optional<CellDetails> getCell(String identifier) {
        CellDetails cell = cellsHolder.getCellById(identifier);
        if (cell == null) {
            return Optional.empty();
        }

        return Optional.of(cell);
    }

    private void sendCellNotification(CellDetails cellDetails) {
        messagingTemplate.convertAndSend(TOPIC_CELL, cellDetails);
    }
}
