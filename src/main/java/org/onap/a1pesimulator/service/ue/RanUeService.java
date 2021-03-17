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

package org.onap.a1pesimulator.service.ue;

import java.util.Collection;
import java.util.Optional;
import org.onap.a1pesimulator.data.ue.RanUserEquipment;
import org.onap.a1pesimulator.data.ue.UserEquipment;

public interface RanUeService {

    Collection<UserEquipment> getUserEquipments();

    Collection<UserEquipment> getUserEquipmentsConnectedToCell(String cellId);

    Optional<UserEquipment> getUserEquipment(String id);

    RanUserEquipment getUes();

    void handover(String ueId, String cellId);

    boolean canHandover(String ueId, String cellId);
}
