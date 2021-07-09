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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiFunction;

import org.onap.a1pesimulator.data.ReportingMethodEnum;
import org.onap.a1pesimulator.data.RequestParameters;
import org.onap.a1pesimulator.data.fileready.RanPeriodicFileReadyEvent;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
import org.onap.a1pesimulator.service.common.AbstractRanRunnable;
import org.onap.a1pesimulator.service.common.AbstractThreadFunction;
import org.onap.a1pesimulator.service.common.EventCustomizer;
import org.onap.a1pesimulator.service.fileready.RanFileReadyHolder;
import org.onap.a1pesimulator.service.fileready.RanSaveFileReadyRunnable;
import org.onap.a1pesimulator.service.fileready.RanSendReportsRunnable;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory.Mode;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class RanVesHolder {

    private static final Logger log = LoggerFactory.getLogger(RanVesHolder.class);
    private final Map<String, RanPeriodicFileReadyEvent> periodicEventsCache = new ConcurrentHashMap<>();

    private final RanVesDataProvider vesDataProvider;
    private final RanEventCustomizerFactory eventCustomizerFactory;
    private final ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler;
    private final Collection<OnEventAction> onEventActions;
    private final RanFileReadyHolder ranFileReadyHolder;
    private final RanVesSender vesSender;
    private final VnfConfigReader vnfConfigReader;
    private ThreadVesSendFunction threadVesSendFunction;
    private ThreadSendReportFunction threadSendReportFunction;

    public RanVesHolder(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, RanFileReadyHolder ranFileReadyHolder, RanVesSender vesSender,
            VnfConfigReader vnfConfigReader,
            RanEventCustomizerFactory eventCustomizerFactory, RanVesDataProvider vesDataProvider,
            Collection<OnEventAction> onEventActions) {
        this.vesPmThreadPoolTaskScheduler = vesPmThreadPoolTaskScheduler;
        this.ranFileReadyHolder = ranFileReadyHolder;
        this.vesSender = vesSender;
        this.vnfConfigReader = vnfConfigReader;
        this.eventCustomizerFactory = eventCustomizerFactory;
        this.vesDataProvider = vesDataProvider;
        this.onEventActions = onEventActions;
    }

    /**
     * Thread for periodical sending of PM Bulk Files and fileReady Events
     *
     * @param vesEvent initial event from input
     */
    private void startSendingReports(VesEvent vesEvent) {
        if (isNull(threadVesSendFunction) || !threadVesSendFunction.isProcessRunning()) {
            int repPeriod = vnfConfigReader.getVnfConfig().getRepPeriod();
            threadSendReportFunction = new ThreadSendReportFunction(vesPmThreadPoolTaskScheduler, vesEvent, repPeriod,
                    eventCustomizerFactory.getEventCustomizer(vesEvent, Mode.REGULAR), onEventActions, ranFileReadyHolder);
            threadSendReportFunction.startEvent();
            log.info("Start sending reports every {} seconds", repPeriod);
        }
    }

    /**
     * Stops sending the report after the last cell was stopped. It send the last report before stop completely
     */
    private void stopSendingReports() {
        if (nonNull(threadSendReportFunction) && !isAnyEventRunning()) {
            threadSendReportFunction.ranPeriodicVesEvent.getScheduledFuture().cancel(false);
            sendLastReportAfterCancel();
            log.info("Stop sending reports every {} seconds", vnfConfigReader.getVnfConfig().getRepPeriod());
        }
    }

    /**
     * Sends the last report after all threads were stopped
     */
    private void sendLastReportAfterCancel() {
        log.trace("Send last report after report thread was canceled");
        ranFileReadyHolder.createPMBulkFileAndSendFileReadyMessage();
    }

    Map<String, RanPeriodicFileReadyEvent> getPeriodicEventsCache() {
        return periodicEventsCache;
    }

    ResponseEntity<String> startSendingVesEvents(String identifier, VesEvent vesEvent, Integer interval, ReportingMethodEnum reportingMethod) {

        periodicEventsCache.compute(identifier,
                new ThreadCacheUpdateFunction(vesPmThreadPoolTaskScheduler, eventCustomizerFactory.getEventCustomizer(vesEvent, Mode.REGULAR), onEventActions,
                        ranFileReadyHolder, vesSender, RequestParameters.builder()
                        .vesEvent(vesEvent).identifier(identifier).reportingMethod(reportingMethod).interval(interval).build()));
        if (ReportingMethodEnum.FILE_READY.equals(reportingMethod)) {
            startSendingReports(vesEvent);
        }
        return ResponseEntity.accepted().body("VES Event sending started");
    }

    ResponseEntity<String> startSendingFailureVesEvents(String identifier, VesEvent vesEvent, ReportingMethodEnum reportingMethod) {

        periodicEventsCache.compute(identifier,
                new ThreadCacheUpdateFunction(vesPmThreadPoolTaskScheduler, eventCustomizerFactory.getEventCustomizer(vesEvent, Mode.FAILURE), onEventActions,
                        ranFileReadyHolder,
                        vesSender, RequestParameters.builder().vesEvent(vesEvent).identifier(identifier).interval(vesDataProvider.getFailureVesInterval())
                        .reportingMethod(reportingMethod).build()));
        if (ReportingMethodEnum.FILE_READY.equals(reportingMethod)) {
            startSendingReports(vesEvent);
        }
        return ResponseEntity.accepted().body("Failure VES Event sending started");
    }

    Optional<RanPeriodicFileReadyEvent> stopSendingVesEvents(String identifier) {
        RanPeriodicFileReadyEvent periodicEvent = periodicEventsCache.remove(identifier);
        if (periodicEvent == null) {
            return Optional.empty();
        }
        periodicEvent.getScheduledFuture().cancel(false);
        stopSendingReports();
        return Optional.of(periodicEvent);
    }

    Collection<String> getEnabledEventElementIdentifiers() {
        return periodicEventsCache.keySet();
    }

    public boolean isEventEnabled(String identifier) {
        return periodicEventsCache.containsKey(identifier);
    }

    public boolean isAnyEventRunning() {
        return !periodicEventsCache.isEmpty();
    }

    VesEvent getEventStructure(String identifier) {
        if (!periodicEventsCache.containsKey(identifier)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Cannot find event for given source {0}", identifier));
        }
        return periodicEventsCache.get(identifier).getEvent();
    }

    private static class ThreadCacheUpdateFunction
            implements BiFunction<String, RanPeriodicFileReadyEvent, RanPeriodicFileReadyEvent> {

        private final Integer interval;
        private final ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler;
        private final VesEvent vesEvent;
        private final EventCustomizer eventCustomizer;
        private final Collection<OnEventAction> onEventActions;
        private final RanFileReadyHolder fileReadyHolder;
        private final RanVesSender vesSender;
        private final String cellId;
        private final ReportingMethodEnum reportingMethod;

        public ThreadCacheUpdateFunction(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, EventCustomizer eventCustomizer,
                Collection<OnEventAction> onEventActions,
                RanFileReadyHolder fileReadyHolder, RanVesSender vesSender, RequestParameters requestParameters) {
            this.vesPmThreadPoolTaskScheduler = vesPmThreadPoolTaskScheduler;
            this.vesEvent = requestParameters.getVesEvent();
            this.interval = requestParameters.getInterval();
            this.eventCustomizer = eventCustomizer;
            this.onEventActions = onEventActions;
            this.fileReadyHolder = fileReadyHolder;
            this.vesSender = vesSender;
            this.cellId = requestParameters.getIdentifier();
            this.reportingMethod = requestParameters.getReportingMethod();
        }

        @Override
        public RanPeriodicFileReadyEvent apply(String key, RanPeriodicFileReadyEvent value) {
            if (value != null) {
                // if thread is registered then cancel it and schedule a new one
                value.getScheduledFuture().cancel(false);
            }
            AbstractRanRunnable ranRunnable = (ReportingMethodEnum.FILE_READY.equals(reportingMethod)) ?
                    new RanSaveFileReadyRunnable(fileReadyHolder, cellId, vesEvent, eventCustomizer, interval, onEventActions) :
                    new RanSendVesRunnable(vesSender, vesEvent, eventCustomizer, onEventActions);

            ScheduledFuture<?> scheduledFuture =
                    vesPmThreadPoolTaskScheduler.scheduleAtFixedRate(ranRunnable, interval * 1000L);
            return RanPeriodicFileReadyEvent.builder().event(vesEvent).interval(interval).scheduledFuture(scheduledFuture)
                    .saveFileReadyRunnable(ranRunnable).build();
        }

    }

    private static class ThreadVesSendFunction extends AbstractThreadFunction {

        private final RanVesSender vesSender;

        public ThreadVesSendFunction(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, VesEvent vesEvent,
                Integer interval, EventCustomizer eventCustomizer, Collection<OnEventAction> onEventActions, RanFileReadyHolder ranFileReadyHolder,
                RanVesSender vesSender) {
            super(vesPmThreadPoolTaskScheduler, vesEvent, interval, eventCustomizer, onEventActions, ranFileReadyHolder);
            this.vesSender = vesSender;
        }

        public void startEvent() {
            RanSendVesRunnable sendVesRunnable =
                    new RanSendVesRunnable(vesSender, vesEvent, eventCustomizer, onEventActions);
            scheduledFuture =
                    vesPmThreadPoolTaskScheduler.scheduleAtFixedRate(sendVesRunnable, interval * 1000L);
            this.ranPeriodicVesEvent = RanPeriodicVesEvent.builder().event(vesEvent).interval(interval).scheduledFuture(scheduledFuture)
                    .sendVesRunnable(sendVesRunnable).build();
        }

        public boolean isProcessRunning() {
            return (nonNull(scheduledFuture) && !(scheduledFuture.isCancelled() || scheduledFuture.isDone()));
        }
    }

    private static class ThreadSendReportFunction extends AbstractThreadFunction {

        public ThreadSendReportFunction(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, VesEvent vesEvent,
                Integer interval, EventCustomizer eventCustomizer, Collection<OnEventAction> onEventActions, RanFileReadyHolder ranFileReadyHolder) {
            super(vesPmThreadPoolTaskScheduler, vesEvent, interval, eventCustomizer, onEventActions, ranFileReadyHolder);
        }

        public void startEvent() {
            RanSendReportsRunnable sendVesRunnable =
                    new RanSendReportsRunnable(ranFileReadyHolder, vesEvent, eventCustomizer, onEventActions);
            scheduledFuture =
                    vesPmThreadPoolTaskScheduler.scheduleAtFixedRate(sendVesRunnable, interval * 1000L);
            this.ranPeriodicVesEvent = RanPeriodicVesEvent.builder().event(vesEvent).interval(interval).scheduledFuture(scheduledFuture)
                    .sendVesRunnable(sendVesRunnable).build();
        }

        public boolean isProcessRunning() {
            return (nonNull(scheduledFuture) && !(scheduledFuture.isCancelled() || scheduledFuture.isDone()));
        }
    }

}
