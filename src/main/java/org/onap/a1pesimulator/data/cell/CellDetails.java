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

package org.onap.a1pesimulator.data.cell;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.onap.a1pesimulator.data.cell.state.CellStateEnum;
import org.onap.a1pesimulator.data.cell.state.CellStateMachine;
import org.onap.a1pesimulator.data.cell.state.machine.InactiveState;

@Getter
@Builder
public class CellDetails {

    private String id;
    private Double latitude;
    private Double longitude;

    @Setter
    @JsonIgnore
    @Builder.Default
    private CellStateMachine cellStateMachine = new InactiveState();

    @Setter
    private Collection<String> connectedUserEquipments;

    public void previousState() {
        cellStateMachine.prev(this);
    }

    public void nextState() {
        cellStateMachine.next(this);
    }

    @JsonProperty("currentState")
    public CellStateEnum getCurrentState() {
        return cellStateMachine.getState();
    }
}
