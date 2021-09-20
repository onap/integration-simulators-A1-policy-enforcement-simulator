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

import static java.util.Objects.nonNull;

import org.onap.a1pesimulator.data.Event;
import org.onap.a1pesimulator.data.VnfConfig;
import org.onap.a1pesimulator.data.ves.CommonEventHeader;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.onap.a1pesimulator.util.JsonUtils;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Mono;

@Service
public class RanVesSender {

    private static final Logger log = LoggerFactory.getLogger(RanVesSender.class);

    private RestTemplate restTemplate;

    private String vesCollectorProtocol;

    private String vesCollectorPath;

    private VnfConfigReader vnfConfigReader;

    public RanVesSender(RestTemplate restTemplate, VnfConfigReader vnfConfigReader,
            @Value("${ves.collector.protocol}") String vesCollectorProtocol,
            @Value("${ves.collector.endpoint}") String vesCollectorPath) {
        this.restTemplate = restTemplate;
        this.vnfConfigReader = vnfConfigReader;
        this.vesCollectorProtocol = vesCollectorProtocol;
        this.vesCollectorPath = vesCollectorPath;
    }

    public Mono<HttpStatus> send(Event event) {
        if (nonNull(event)) {
            VnfConfig vnfConfig = vnfConfigReader.getVnfConfig();
            String url = getVesCollectorUrl(vnfConfig);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(vnfConfig.getVesUser(), vnfConfig.getVesPassword());

            setVnfInfo(event, vnfConfig);
            String eventInJson = JsonUtils.INSTANCE.objectToPrettyString(event);

            log.trace("Sending following event: {} ", eventInJson);

            HttpEntity<String> entity = new HttpEntity<>(eventInJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.debug("Response received: {}", response);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.ACCEPTED) {
                return Mono.just(response.getStatusCode());
            } else {
                String errorMsg =
                        "Failed to send VES event to the collector with response status code:" + response.getStatusCode();
                return Mono.error(new VesBrokerException(errorMsg));
            }
        }
        return Mono.error(new VesBrokerException("There is no event to send to the collector."));
    }

    private String getVesCollectorUrl(VnfConfig vnfConfig) {
        return vesCollectorProtocol + "://" + vnfConfig.getVesHost() + ":" + vnfConfig.getVesPort() + vesCollectorPath;
    }

    private void setVnfInfo(Event vesEvent, VnfConfig vnfConfig) {
        CommonEventHeader header = vesEvent.getCommonEventHeader();
        header.setSourceId(vnfConfig.getVnfId());
        header.setSourceName(vnfConfig.getVnfName());
        vesEvent.setCommonEventHeader(header);
    }

}
