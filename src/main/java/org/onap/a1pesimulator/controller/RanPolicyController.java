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
import org.onap.a1pesimulator.service.a1.A1Service;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class RanPolicyController {

    private final A1Service a1Service;

    public RanPolicyController(A1Service a1Service) {
        this.a1Service = a1Service;
    }

    /**
     * Method for reading all policies of given policy type from A1 PE Simulator in one go
     *
     * @return Policy instance list for wanted policyType
     * @throws IOException
     * @throws URISyntaxException
     */
    @GetMapping(value = "${restapi.version}/ran/policies/{policyTypeId}")
    public ResponseEntity<String> getAllPolicies(@PathVariable Integer policyTypeId)
            throws IOException, URISyntaxException {
        return a1Service.getAllPoliciesForType(policyTypeId);
    }
}
