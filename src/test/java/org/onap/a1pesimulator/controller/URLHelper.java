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

import org.springframework.stereotype.Component;

@Component
public class URLHelper {

    private static final String A1_CONTROLLER_PREFIX = "/v1/a1-p";

    private static final String POLICY_FORMAT = "%s/%s/policies/%s";

    private static final String RAN_CELL_CONTROLLER_PREFIX = "${restapi.version}/ran/cells";

    private static final String RAN_CONTROLLER_PREFIX = "${restapi.version}/ran";

    private static final String RAN_UE_CONTROLLER_PREFIX = "${restapi.version}/ran/ues";

    public static String getHealthCheckEndpoint() {
        return A1_CONTROLLER_PREFIX + "/healthcheck";
    }

    public static String getPolicyTypePath() {
        return A1_CONTROLLER_PREFIX + "/policytypes";
    }

    public static String getPolicyTypePath(String policyType) {
        return getPolicyTypePath() + "/" + policyType;
    }

    public static String getPolicyPath(String policyType, String policy) {
        return String.format(POLICY_FORMAT, getPolicyTypePath(), policyType, policy);
    }

    public static String getRanCellControllerEndpoint() {
        return RAN_CELL_CONTROLLER_PREFIX;
    }

    public static String getRanControllerEndpoint() {
        return RAN_CONTROLLER_PREFIX;
    }

    public static String getRanUeControllerEndpoint() {
        return RAN_UE_CONTROLLER_PREFIX;
    }
}
