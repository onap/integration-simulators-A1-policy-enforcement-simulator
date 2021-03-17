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

import java.util.Collection;
import java.util.function.Function;
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RanSendVesRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RanSendVesRunnable.class);

    private final RanVesSender vesSender;
    private Event event;
    private final EventCustomizer eventCustomizer;
    private final Collection<OnEventAction> onEventAction;

    public RanSendVesRunnable(RanVesSender vesSender, Event event, EventCustomizer eventCustomizer,
            Collection<OnEventAction> onEventActions) {
        this.vesSender = vesSender;
        this.event = event;
        this.eventCustomizer = eventCustomizer;
        this.onEventAction = onEventActions;
    }

    @Override
    public void run() {
        try {
            Event customizedEvent = eventCustomizer.apply(event);
            onEventAction.forEach(action -> action.onEvent(customizedEvent));
            vesSender.send(customizedEvent);
        } catch (VesBrokerException e) {
            log.error("Sending scheduled event failed: {}", e.getMessage());
        }
    }

    public void updateEvent(Event event) {
        this.event = event;
    }

    @FunctionalInterface
    public interface EventCustomizer extends Function<Event, Event> { }
}
