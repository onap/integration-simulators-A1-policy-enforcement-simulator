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

package org.onap.a1pesimulator.data.fileready;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.concurrent.ScheduledFuture;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.common.AbstractRanRunnable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties({ "scheduledFuture", "ranRunnable" })
public class RanPeriodicEvent {

    private VesEvent event;
    private Integer interval;
    private String reportingMethod;
    private ScheduledFuture<?> scheduledFuture;
    private AbstractRanRunnable ranRunnable;
}
