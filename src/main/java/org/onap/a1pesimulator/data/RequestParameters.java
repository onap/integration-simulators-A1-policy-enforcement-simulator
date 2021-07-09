package org.onap.a1pesimulator.data;

import org.onap.a1pesimulator.data.ves.VesEvent;

import lombok.Builder;
import lombok.Data;

/**
 * Request parameters object to pass multiply param values to methods
 */
@Data
@Builder
public class RequestParameters {

    String identifier;
    VesEvent vesEvent;
    Integer interval;
    ReportingMethodEnum reportingMethod;
}
