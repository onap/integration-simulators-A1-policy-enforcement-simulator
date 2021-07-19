package org.onap.a1pesimulator.service.common;

import java.util.Collection;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.ves.OnEventAction;

public abstract class AbstractRanRunnable implements Runnable {

    protected VesEvent event;
    protected final EventCustomizer eventCustomizer;
    protected final Collection<OnEventAction> onEventAction;


    protected AbstractRanRunnable(VesEvent event, EventCustomizer eventCustomizer,
            Collection<OnEventAction> onEventActions) {
        this.event = event;
        this.eventCustomizer = eventCustomizer;
        this.onEventAction = onEventActions;
    }

    public void updateEvent(VesEvent event) {
        this.event = event;
    }
}
