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

package org.onap.a1pesimulator.service.a1;

import java.util.List;

import org.onap.a1pesimulator.data.fileready.RanPeriodicEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.service.ves.RanVesBrokerService;
import org.onap.a1pesimulator.util.JsonUtils;
import org.onap.a1pesimulator.util.RanVesUtils;
import org.springframework.stereotype.Service;

@Service
public class SetLowRangeValuesOnPolicyAction implements OnPolicyAction {

    private final RanVesBrokerService vesBrokerService;

    public SetLowRangeValuesOnPolicyAction(RanVesBrokerService vesBrokerService) {
        this.vesBrokerService = vesBrokerService;
    }

    @Override
    public boolean isForMe(Integer policyTypeId, String policyId, String body) {
        // disabling for now
        return false;
    }

    @Override
    public void onPolicy(Integer policyTypeId, String policyId, String body) {
        vesBrokerService.getPeriodicEventsCache().values().forEach(this::updateEvent);
    }

    private void updateEvent(RanPeriodicEvent periodicEvent) {
        List<AdditionalMeasurement> lowRangeValues = RanVesUtils.setLowRangeValues(
                periodicEvent.getEvent().getMeasurementFields().getAdditionalMeasurements());
        VesEvent clonedEvent = JsonUtils.INSTANCE.clone(periodicEvent.getEvent());
        clonedEvent.getMeasurementFields().setAdditionalMeasurements(lowRangeValues);
        periodicEvent.getRanRunnable().updateEvent(clonedEvent);
    }

}
