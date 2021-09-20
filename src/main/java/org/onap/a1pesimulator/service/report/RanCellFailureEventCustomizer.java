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

package org.onap.a1pesimulator.service.report;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.service.common.EventCustomizer;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.util.Constants;
import org.onap.a1pesimulator.util.JsonUtils;
import org.onap.a1pesimulator.util.RanVesUtils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

public class RanCellFailureEventCustomizer implements EventCustomizer {

    private static final String UE_PARAM_TRAFFIC_MODEL_RANGE = "[[50->10]]";
    private final RanUeHolder ranUeHolder;
    private final VesEvent event;

    private final Map<Key, Value> additionalMeasurementsValues = new HashMap<>();
    private final ValueFactory valueFactory;

    public RanCellFailureEventCustomizer(VesEvent event, RanUeHolder ranUeHolder) {
        this.ranUeHolder = ranUeHolder;
        this.event = event;
        valueFactory = new ValueFactory();
        collectAdditionalMeasurementValues(event);
    }

    @Override
    public VesEvent apply(VesEvent t) {
        return customizeEvent(JsonUtils.INSTANCE.clone(this.event));
    }

    private void collectAdditionalMeasurementValues(VesEvent event) {
        Collection<AdditionalMeasurement> additionalMeasurementsToResolve =
                event.getMeasurementFields().getAdditionalMeasurements();
        additionalMeasurementsToResolve.forEach(this::collectAdditionalMeasurementValue);
    }

    private void collectAdditionalMeasurementValue(AdditionalMeasurement m) {
        for (Entry<String, String> entry : m.getHashMap().entrySet()) {
            if (!RanVesUtils.isRange(entry.getValue())) {
                continue;
            }
            additionalMeasurementsValues
                    .putIfAbsent(new Key(m.getName(), entry.getKey()), valueFactory.getInstance(entry.getValue()));
        }
    }

    private VesEvent customizeEvent(VesEvent event) {
        RanVesUtils.updateHeader(event);
        enrichWithUeData(event);
        resolveRanges(event);
        return event;
    }

    private void resolveRanges(VesEvent event) {
        List<AdditionalMeasurement> additionalMeasurementsToResolve =
                event.getMeasurementFields().getAdditionalMeasurements();

        additionalMeasurementsToResolve.forEach(this::resolveRanges);
        event.getMeasurementFields().setAdditionalMeasurements(additionalMeasurementsToResolve);
    }

    private void resolveRanges(AdditionalMeasurement m) {
        for (Entry<String, String> entry : m.getHashMap().entrySet()) {
            Key key = new Key(m.getName(), entry.getKey());
            if (!additionalMeasurementsValues.containsKey(key)) {
                continue;
            }
            Value value = additionalMeasurementsValues.get(key);
            value.current = value.calculateCurrentValue();
            entry.setValue(value.current.toString());
        }
    }

    private void enrichWithUeData(VesEvent event) {

        Optional<AdditionalMeasurement> identity = event.getMeasurementFields().getAdditionalMeasurements().stream()
                .filter(msrmnt -> Constants.MEASUREMENT_FIELD_IDENTIFIER
                        .equalsIgnoreCase(
                                msrmnt.getName()))
                .findAny();
        identity.ifPresent(m -> addTrafficModelMeasurement(event));
    }

    private void addTrafficModelMeasurement(VesEvent event) {
        AdditionalMeasurement trafficModelMeasurement =
                RanVesUtils.buildTrafficModelMeasurement(ranUeHolder, UE_PARAM_TRAFFIC_MODEL_RANGE);
        event.getMeasurementFields().getAdditionalMeasurements().add(trafficModelMeasurement);

        collectAdditionalMeasurementValue(trafficModelMeasurement);
    }

    // -----------helper classes

    private static class ValueFactory {

        public Value getInstance(String value) {
            String[] split;
            if (RanVesUtils.isRandomRange(value)) {
                split = RanVesUtils.splitRandomRange(value);
                return new RandomValue(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
            }
            if (RanVesUtils.isTrandingRange(value)) {
                split = RanVesUtils.splitTrendingRange(value);
                Integer start = Integer.valueOf(split[0]);
                Integer end = Integer.valueOf(split[1]);
                if (start < end) {
                    return new RaisingValue(start, end);
                } else if (start > end) {
                    return new DecreasingValue(start, end);
                }
            }
            throw new RuntimeException(MessageFormat.format("Cannot instantiate Value from string: {0}", value));
        }
    }

    private abstract static class Value {

        protected Integer start;
        protected Integer end;
        protected Integer current;

        public Value(Integer start, Integer end) {
            this.start = start;
            this.end = end;
        }

        public abstract Integer calculateCurrentValue();
    }

    private static class RaisingValue extends Value {

        private int increment;

        public RaisingValue(Integer start, Integer end) {
            super(start, end);
        }

        @Override
        public Integer calculateCurrentValue() {
            if (current == null) {
                return start;
            }
            if (increment == 0) {
                increment = 1;
            } else {
                increment = increment * 2;
            }
            Integer result = start + increment;
            if (result > end) {
                increment = 1;
                return end;
            }
            return result;
        }
    }

    private static class DecreasingValue extends Value {

        private int decrement;

        public DecreasingValue(Integer start, Integer end) {
            super(start, end);
        }

        @Override
        public Integer calculateCurrentValue() {
            if (current == null) {
                return start;
            }
            if (decrement == 0) {
                decrement = 1;
            } else {
                decrement = decrement * 2;
            }
            Integer result = start - decrement;
            if (result < end) {
                return end;
            }
            return result;
        }
    }

    private static class RandomValue extends Value {

        public RandomValue(Integer start, Integer end) {
            super(start, end);
        }

        @Override
        public Integer calculateCurrentValue() {
            return RanVesUtils.getRandomNumber(start, end);
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class Key {

        private String paramName;
        private String mapKey;
    }
}
