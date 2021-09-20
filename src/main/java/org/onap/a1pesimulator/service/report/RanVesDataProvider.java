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

import java.io.IOException;
import java.net.URL;

import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.Setter;

@Service
public class RanVesDataProvider {

    private static final String PM_VES_LOCATION = "classpath:pmVes.json";
    private static final String PM_FAILURE_VES_LOCATION = "classpath:failurePmVes.json";

    @Setter
    private VesEvent pmVesEvent;
    @Setter
    private VesEvent failurePmVesEvent;
    @Setter
    private Integer interval;
    @Setter
    private String reportingMethod;

    private final Integer defaultInterval;
    private final String defaultReportingMethod;
    private final ResourceLoader resourceLoader;

    public RanVesDataProvider(@Value("${ves.defaultInterval}") Integer defaultInterval,
            @Value("${ves.defaultReportingMethod}") String defaultReportingMethod,
            ResourceLoader resourceLoader) {
        this.defaultInterval = defaultInterval;
        this.defaultReportingMethod = defaultReportingMethod;
        this.resourceLoader = resourceLoader;
    }

    @Cacheable("pmVes")
    public VesEvent loadPmVesEvent() {
        URL resourceUrl = getResourceURL(resourceLoader.getResource(PM_VES_LOCATION));
        return JsonUtils.INSTANCE.deserializeFromFileUrl(resourceUrl, VesEvent.class);
    }

    @Cacheable("failurePmVes")
    public VesEvent loadFailurePmVesEvent() {
        URL resourceUrl = getResourceURL(resourceLoader.getResource(PM_FAILURE_VES_LOCATION));
        return JsonUtils.INSTANCE.deserializeFromFileUrl(resourceUrl, VesEvent.class);
    }

    public Integer getRegularVesInterval() {
        if (interval == null) {
            return defaultInterval;
        }
        return interval;
    }

    public String getReportingMethod() {
        if (reportingMethod == null) {
            return defaultReportingMethod;
        }

        return reportingMethod;
    }

    public Integer getFailureVesInterval() {
        return defaultInterval;
    }

    public VesEvent getPmVesEvent() {
        if (pmVesEvent == null) {
            return loadPmVesEvent();
        }
        return pmVesEvent;
    }

    public VesEvent getFailurePmVesEvent() {
        if (failurePmVesEvent == null) {
            return loadFailurePmVesEvent();
        }
        return failurePmVesEvent;
    }

    private URL getResourceURL(Resource resource) {
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new RuntimeException("Cannot get resource URL for: " + resource.getFilename());
        }
    }
}
