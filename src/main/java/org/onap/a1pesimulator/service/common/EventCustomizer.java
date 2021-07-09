package org.onap.a1pesimulator.service.common;

import java.util.function.Function;

import org.onap.a1pesimulator.data.ves.VesEvent;

@FunctionalInterface
public interface EventCustomizer extends Function<VesEvent, VesEvent> {

}
