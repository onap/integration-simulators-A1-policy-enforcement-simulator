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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiFunction;
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory.Mode;
import org.onap.a1pesimulator.service.ves.RanSendVesRunnable.EventCustomizer;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class RanVesHolder {

    private final Map<String, RanPeriodicVesEvent> periodicEventsCache = new ConcurrentHashMap<>();

    private final RanVesDataProvider vesDataProvider;
    private final RanEventCustomizerFactory eventCustomizerFactory;
    private final ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler;
    private final Collection<OnEventAction> onEventActions;
    private final RanVesSender vesSender;

    public RanVesHolder(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, RanVesSender vesSender,
            RanEventCustomizerFactory eventCustomizerFactory, RanVesDataProvider vesDataProvider,
            Collection<OnEventAction> onEventActions) {
        this.vesPmThreadPoolTaskScheduler = vesPmThreadPoolTaskScheduler;
        this.vesSender = vesSender;
        this.eventCustomizerFactory = eventCustomizerFactory;
        this.vesDataProvider = vesDataProvider;
        this.onEventActions = onEventActions;
    }

    Map<String, RanPeriodicVesEvent> getPeriodicEventsCache() {
        return periodicEventsCache;
    }

    ResponseEntity<String> startSendingVesEvents(String identifier, Event vesEvent, Integer interval) {

        periodicEventsCache.compute(identifier,
                new ThreadCacheUpdateFunction(vesPmThreadPoolTaskScheduler, vesEvent, interval,
                        eventCustomizerFactory.getEventCustomizer(vesEvent, Mode.REGULAR), onEventActions, vesSender));
        return ResponseEntity.accepted().body("VES Event sending started");
    }

    ResponseEntity<String> startSendingFailureVesEvents(String identifier, Event vesEvent) {

        periodicEventsCache.compute(identifier, new ThreadCacheUpdateFunction(vesPmThreadPoolTaskScheduler, vesEvent,
                vesDataProvider.getFailureVesInterval(),
                eventCustomizerFactory.getEventCustomizer(vesEvent, Mode.FAILURE), onEventActions, vesSender));
        return ResponseEntity.accepted().body("Failure VES Event sending started");
    }

    Optional<RanPeriodicVesEvent> stopSendingVesEvents(String identifier) {
        RanPeriodicVesEvent periodicEvent = periodicEventsCache.remove(identifier);
        if (periodicEvent == null) {
            return Optional.empty();
        }
        periodicEvent.getScheduledFuture().cancel(false);
        return Optional.of(periodicEvent);
    }

    Collection<String> getEnabledEventElementIdentifiers() {
        return periodicEventsCache.keySet();
    }

    public boolean isEventEnabled(String identifier) {
        return periodicEventsCache.containsKey(identifier);
    }

    Event getEventStructure(String identifier) {
        if (!periodicEventsCache.containsKey(identifier)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Cannot find event for given source {0}", identifier));
        }
        return periodicEventsCache.get(identifier).getEvent();
    }

    private static class ThreadCacheUpdateFunction
            implements BiFunction<String, RanPeriodicVesEvent, RanPeriodicVesEvent> {

        private final Integer interval;
        private final ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler;
        private final Event vesEvent;
        private final EventCustomizer eventCustomizer;
        private final Collection<OnEventAction> onEventActions;
        private final RanVesSender vesSender;

        public ThreadCacheUpdateFunction(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, Event vesEvent,
                Integer interval, EventCustomizer eventCustomizer, Collection<OnEventAction> onEventActions,
                RanVesSender vesSender) {
            this.vesPmThreadPoolTaskScheduler = vesPmThreadPoolTaskScheduler;
            this.vesEvent = vesEvent;
            this.interval = interval;
            this.eventCustomizer = eventCustomizer;
            this.onEventActions = onEventActions;
            this.vesSender = vesSender;
        }

        @Override
        public RanPeriodicVesEvent apply(String key, RanPeriodicVesEvent value) {
            if (value != null) {
                // if thread is registered then cancel it and schedule a new one
                value.getScheduledFuture().cancel(false);
            }
            RanSendVesRunnable sendVesRunnable =
                    new RanSendVesRunnable(vesSender, vesEvent, eventCustomizer, onEventActions);
            ScheduledFuture<?> scheduledFuture =
                    vesPmThreadPoolTaskScheduler.scheduleAtFixedRate(sendVesRunnable, interval * 1000L);
            return RanPeriodicVesEvent.builder().event(vesEvent).interval(interval).scheduledFuture(scheduledFuture)
                           .sendVesRunnable(sendVesRunnable).build();
        }

    }
}
