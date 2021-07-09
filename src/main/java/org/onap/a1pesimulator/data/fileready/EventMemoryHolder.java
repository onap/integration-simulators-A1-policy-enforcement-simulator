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

import java.time.ZonedDateTime;

import org.onap.a1pesimulator.data.ves.VesEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * EventMemoryHolder object stores event's information which are collacted for a specific reporting period
 */
@Getter
@Setter
@AllArgsConstructor
public class EventMemoryHolder {

    private String cellId;
    private String jobId;
    private Integer granPeriod;
    private ZonedDateTime eventDate;
    private VesEvent event;
}
