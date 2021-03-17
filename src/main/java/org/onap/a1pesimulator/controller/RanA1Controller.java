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

package org.onap.a1pesimulator.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import org.onap.a1pesimulator.data.PolicyNotification;
import org.onap.a1pesimulator.data.PolicyNotificationActionEnum;
import org.onap.a1pesimulator.service.a1.A1Service;
import org.onap.a1pesimulator.service.a1.OnPolicyAction;
import org.onap.a1pesimulator.service.a1.PolicyInstancesHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A1 interface facade
 * Only operations defined by OSC_2.1.0 should be provided here
 */

@RestController
@RequestMapping({"${restapi.version}/a1-p"})
public class RanA1Controller {

    private static final String TOPIC_POLICY = "/topic/policy";
    private final SimpMessagingTemplate messagingTemplate;
    private final A1Service a1Service;
    private final Collection<OnPolicyAction> onPolicyActions;
    private final PolicyInstancesHolder policyHolder;

    public RanA1Controller(A1Service a1Service, SimpMessagingTemplate messagingTemplate,
            Collection<OnPolicyAction> onPolicyActions, PolicyInstancesHolder policyHolder) {
        this.a1Service = a1Service;
        this.messagingTemplate = messagingTemplate;
        this.onPolicyActions = onPolicyActions;
        this.policyHolder = policyHolder;
    }

    @GetMapping(value = "/healthcheck")
    public ResponseEntity<String> healthcheck() throws URISyntaxException {
        return a1Service.healthCheck();
    }

    @PutMapping(value = "/policytypes/{policyTypeId}")
    public ResponseEntity<String> putPolicySchema(@PathVariable Integer policyTypeId, @RequestBody String body)
            throws URISyntaxException {
        return a1Service.putPolicySchema(policyTypeId, body);
    }

    @PutMapping(value = "/policytypes/{policyTypeId}/policies/{policyInstanceId}")
    public ResponseEntity<String> putPolicyInstance(@PathVariable Integer policyTypeId,
            @PathVariable String policyInstanceId, @RequestBody String body) throws URISyntaxException {
        ResponseEntity<String> response = a1Service.putPolicy(policyTypeId, policyInstanceId, body);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return response;
        }
        policyHolder.addPolicy(policyInstanceId, body);
        onPolicyActions.forEach(action -> handleOnPolicyAction(policyTypeId, policyInstanceId, body, action));
        messagingTemplate.convertAndSend(TOPIC_POLICY,
                new PolicyNotification(policyInstanceId, policyTypeId, PolicyNotificationActionEnum.CREATED, body));
        return response;
    }

    @DeleteMapping(value = "/policytypes/{policyTypeId}/policies/{policyInstanceId}")
    public ResponseEntity<String> deletePolicyInstance(@PathVariable Integer policyTypeId,
            @PathVariable String policyInstanceId) throws URISyntaxException {
        ResponseEntity<String> response = a1Service.deletePolicy(policyTypeId, policyInstanceId);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        policyHolder.removePolicy(policyInstanceId);
        messagingTemplate.convertAndSend(TOPIC_POLICY,
                new PolicyNotification(policyInstanceId, policyTypeId, PolicyNotificationActionEnum.DELETED));
        return response;
    }

    @GetMapping(value = "/policytypes")
    public ResponseEntity<String> getPolicyTypeIds() throws URISyntaxException {
        return a1Service.getPolicyTypeIds();
    }

    @GetMapping(value = "/policytypes/{policyTypeId}")
    public ResponseEntity<String> getPolicyType(@PathVariable Integer policyTypeId) throws URISyntaxException {
        return a1Service.getPolicyType(policyTypeId);
    }

    @GetMapping(value = "/policytypes/{policyTypeId}/policies")
    public ResponseEntity<String> getPolicyIdsOfType(@PathVariable Integer policyTypeId)
            throws URISyntaxException, IOException {
        return a1Service.getPolicyIdsOfType(policyTypeId);
    }

    @GetMapping(value = "/policytypes/{policyTypeId}/policies/{policyInstanceId}")
    public ResponseEntity<String> getPolicy(@PathVariable Integer policyTypeId, @PathVariable String policyInstanceId)
            throws URISyntaxException {
        return a1Service.getPolicy(policyTypeId, policyInstanceId);
    }

    private void handleOnPolicyAction(Integer policyTypeId, String policyId, String body, OnPolicyAction action) {
        if (action.isForMe(policyTypeId, policyId, body)) {
            action.onPolicy(policyTypeId, policyId, body);
        }
    }
}
