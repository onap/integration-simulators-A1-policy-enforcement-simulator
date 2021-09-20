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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.onap.a1pesimulator.service.pm.CommonFileReady.TEST_CELL_ID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.state.machine.ActiveState;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RanCellStateServiceTest {

    private RanCellStateService ranCellStateService;

    @Mock
    RanCellsHolder ranCellsHolder;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        ranCellStateService = spy(new RanCellStateService(ranCellsHolder, messagingTemplate));

    }

    @Test
    void activateState() {
        doReturn(getTestCellDetails()).when(ranCellsHolder).getCellById(TEST_CELL_ID);
        ranCellStateService.activateState(TEST_CELL_ID);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void failingState() {

        CellDetails cell = getTestCellDetails();
        doReturn(cell).when(ranCellsHolder).getCellById(TEST_CELL_ID);

        // Not allow changing state, cell is in INACTIVE state
        ranCellStateService.failingState(TEST_CELL_ID);
        verify(messagingTemplate, times(0)).convertAndSend(anyString(), (Object) any());

        // Allow changing state, cell is in ACTIVE state
        cell.setCellStateMachine(new ActiveState());
        doReturn(cell).when(ranCellsHolder).getCellById(TEST_CELL_ID);
        ranCellStateService.failingState(TEST_CELL_ID);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void stopState() {
        CellDetails cell = getTestCellDetails();
        doReturn(cell).when(ranCellsHolder).getCellById(TEST_CELL_ID);

        // Not allow changing state, cell is in INACTIVE state
        ranCellStateService.stopState(TEST_CELL_ID);
        verify(messagingTemplate, times(0)).convertAndSend(anyString(), (Object) any());

        // Allow changing state, cell is in ACTIVE state
        cell.setCellStateMachine(new ActiveState());
        doReturn(cell).when(ranCellsHolder).getCellById(TEST_CELL_ID);
        ranCellStateService.stopState(TEST_CELL_ID);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void cellDoesNotExist() {
        doReturn(null).when(ranCellsHolder).getCellById(TEST_CELL_ID);
        ranCellStateService.activateState(TEST_CELL_ID);
        verify(messagingTemplate, times(0)).convertAndSend(anyString(), (Object) any());

    }

    private CellDetails getTestCellDetails() {
        return CellDetails.builder().id(TEST_CELL_ID).latitude(23.5).longitude(45.8).build();
    }
}