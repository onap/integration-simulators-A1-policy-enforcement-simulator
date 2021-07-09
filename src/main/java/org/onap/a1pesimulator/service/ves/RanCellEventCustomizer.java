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

package org.onap.a1pesimulator.service.ves;

import java.util.List;
import java.util.Optional;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.service.common.EventCustomizer;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.util.Constants;
import org.onap.a1pesimulator.util.JsonUtils;
import org.onap.a1pesimulator.util.RanVesUtils;
import org.springframework.stereotype.Service;

@Service
public class RanCellEventCustomizer implements EventCustomizer {

    private static final String UE_PARAM_TRAFFIC_MODEL_RANGE = "[[20-50]]";
    private final RanUeHolder ranUeHolder;

    public RanCellEventCustomizer(RanUeHolder ueHolder) {
        this.ranUeHolder = ueHolder;
    }

    @Override
    public VesEvent apply(VesEvent t) {
        VesEvent event = JsonUtils.INSTANCE.clone(t);
        return customizeEvent(event);
    }

    private VesEvent customizeEvent(VesEvent event) {
        RanVesUtils.updateHeader(event);
        enrichWithUeData(event);
        randomizeEvent(event);
        return event;
    }

    private void randomizeEvent(VesEvent event) {
        List<AdditionalMeasurement> additionalMeasurementsToRandomize =
                event.getMeasurementFields().getAdditionalMeasurements();
        event.getMeasurementFields().setAdditionalMeasurements(
                RanVesUtils.randomizeAdditionalMeasurements(additionalMeasurementsToRandomize));
    }

    private void enrichWithUeData(VesEvent event) {

        Optional<AdditionalMeasurement> identity = event.getMeasurementFields().getAdditionalMeasurements().stream()
                .filter(msrmnt -> Constants.MEASUREMENT_FIELD_IDENTIFIER
                        .equalsIgnoreCase(
                                msrmnt.getName()))
                .findAny();
        identity.ifPresent(m -> addTrafficModelMeasurement(event, m));
    }

    private void addTrafficModelMeasurement(VesEvent event, AdditionalMeasurement identity) {
        AdditionalMeasurement trafficModelMeasurement =
                RanVesUtils.buildTrafficModelMeasurement(identity, ranUeHolder, UE_PARAM_TRAFFIC_MODEL_RANGE);
        event.getMeasurementFields().getAdditionalMeasurements().add(trafficModelMeasurement);
    }
}
