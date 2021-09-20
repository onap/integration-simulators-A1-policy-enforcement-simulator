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

import java.util.Collection;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.common.AbstractRanRunnable;
import org.onap.a1pesimulator.service.common.EventCustomizer;

public class RanSendVesRunnable extends AbstractRanRunnable {

    private final RanVesSender vesSender;

    public RanSendVesRunnable(RanVesSender vesSender, VesEvent event, EventCustomizer eventCustomizer,
            Collection<OnEventAction> onEventActions) {
        super(event, eventCustomizer, onEventActions);
        this.vesSender = vesSender;
    }

    @Override
    public void run() {
        VesEvent customizedEvent = eventCustomizer.apply(event);
        onEventAction.forEach(action -> action.onEvent(customizedEvent));
        vesSender.send(customizedEvent);
    }

    @Override
    public void updateEvent(VesEvent event) {
        this.event = event;
    }
}
