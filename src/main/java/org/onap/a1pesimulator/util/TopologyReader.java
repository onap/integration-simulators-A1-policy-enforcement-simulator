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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.onap.a1pesimulator.data.cell.CellList;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TopologyReader {

    private final String topologyCellConfigFile;
    private final String topologyUeConfigFile;

    private long topologyCellLastModified = 0L;
    private long topologyUeLastModified = 0L;

    private TopologyReader(@Value("${topology.cell.config.file}") final String topologyCellConfigFile,
            @Value("${topology.ue.config.file}") final String topologyUeConfigFile) {
        this.topologyCellConfigFile = topologyCellConfigFile;
        this.topologyUeConfigFile = topologyUeConfigFile;
    }

    public CellList loadCellTopology() {
        final File file = new File(topologyCellConfigFile);
        topologyCellLastModified = file.lastModified();

        if (!file.exists()) {
            return new CellList();
        }

        return JsonUtils.INSTANCE.deserializeFromFile(topologyCellConfigFile, CellList.class);
    }

    public Collection<UserEquipment> loadUeTopology() {
        final File file = new File(topologyUeConfigFile);
        topologyUeLastModified = file.lastModified();

        if (!file.exists()) {
            return Collections.emptyList();
        }

        final UserEquipment[] userEquipments =
                JsonUtils.INSTANCE.deserializeFromFile(topologyUeConfigFile, UserEquipment[].class);
        return Arrays.asList(userEquipments);
    }

    public boolean topologyCellHasChanged() {
        return topologyCellLastModified != new File(topologyCellConfigFile).lastModified();
    }

    public boolean topologyUeHasChanged() {
        return topologyUeLastModified != new File(topologyUeConfigFile).lastModified();
    }
}
