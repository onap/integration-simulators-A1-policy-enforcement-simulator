package org.onap.a1pesimulator.service.common;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.data.ves.RanPeriodicVesEvent;
import org.onap.a1pesimulator.service.fileready.RanFileReadyHolder;
import org.onap.a1pesimulator.service.ves.OnEventAction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public abstract class AbstractThreadFunction {

    protected final Integer interval;
    protected final ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler;
    protected final VesEvent vesEvent;
    protected final EventCustomizer eventCustomizer;
    protected final Collection<OnEventAction> onEventActions;
    protected final RanFileReadyHolder ranFileReadyHolder;
    public RanPeriodicVesEvent ranPeriodicVesEvent;
    protected ScheduledFuture<?> scheduledFuture;

    protected AbstractThreadFunction(ThreadPoolTaskScheduler vesPmThreadPoolTaskScheduler, VesEvent vesEvent,
            Integer interval, EventCustomizer eventCustomizer, Collection<OnEventAction> onEventActions, RanFileReadyHolder ranFileReadyHolder) {
        this.vesPmThreadPoolTaskScheduler = vesPmThreadPoolTaskScheduler;
        this.vesEvent = vesEvent;
        this.interval = interval;
        this.eventCustomizer = eventCustomizer;
        this.onEventActions = onEventActions;
        this.ranFileReadyHolder = ranFileReadyHolder;
    }

    public abstract void startEvent();
}
