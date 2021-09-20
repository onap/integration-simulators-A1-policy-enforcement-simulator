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

package org.onap.a1pesimulator.service.pm;

import java.util.Collection;
import java.util.UUID;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.onap.a1pesimulator.service.common.AbstractRanRunnable;
import org.onap.a1pesimulator.service.common.EventCustomizer;
import org.onap.a1pesimulator.service.report.OnEventAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RanSaveFileReadyRunnable extends AbstractRanRunnable {

    private static final Logger log = LoggerFactory.getLogger(RanSaveFileReadyRunnable.class);
    private final Integer granPeriod;
    private final String cellId;
    private final String jobId;
    private final RanFileReadyHolder ranFileReadyHolder;

    public RanSaveFileReadyRunnable(RanFileReadyHolder ranFileReadyHolder, String cellId, VesEvent event, EventCustomizer eventCustomizer, Integer interval,
            Collection<OnEventAction> onEventActions) {
        super(event, eventCustomizer, onEventActions);
        this.ranFileReadyHolder = ranFileReadyHolder;
        this.granPeriod = interval;
        this.cellId = cellId;
        this.jobId = UUID.randomUUID() + "-" + cellId;
    }

    @Override
    public void run() {
        try {
            VesEvent customizedEvent = eventCustomizer.apply(event);
            onEventAction.forEach(action -> action.onEvent(customizedEvent));
            ranFileReadyHolder.saveEventToMemory(customizedEvent, cellId, jobId, granPeriod);
        } catch (VesBrokerException e) {
            log.error("Saving file ready event failed: {}", e.getMessage());
        }
    }
}
