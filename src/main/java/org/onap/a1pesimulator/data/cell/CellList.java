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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CellList {

    private final List<CellData> cellList;

    public CellList() {
        cellList = Collections.emptyList();
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CellData {

        @JsonProperty("Cell")
        private Cell cell;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cell {

        private String nodeId;
        private Double latitude;
        private Double longitude;
    }
}
