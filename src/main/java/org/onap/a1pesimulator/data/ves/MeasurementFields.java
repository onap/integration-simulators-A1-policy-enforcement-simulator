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

package org.onap.a1pesimulator.data.ves;

import static org.onap.a1pesimulator.util.Constants.MEASUREMENT_FIELD_VALUE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasurementFields {

    private Integer measurementInterval;
    private String measurementFieldsVersion;

    private List<AdditionalMeasurement> additionalMeasurements;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdditionalMeasurement {

        private String name;
        private Map<String, String> hashMap;

        @JsonIgnore
        public String getMeasurementValue() {
            if (hashMap.containsKey(MEASUREMENT_FIELD_VALUE)) {
                return hashMap.get(MEASUREMENT_FIELD_VALUE);
            } else {
                return hashMap.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue())
                               .collect(Collectors.joining(","));
            }
        }
    }
}
