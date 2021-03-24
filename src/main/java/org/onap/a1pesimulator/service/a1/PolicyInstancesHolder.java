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

package org.onap.a1pesimulator.service.a1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.a1pesimulator.util.JsonUtils;
import org.springframework.stereotype.Service;

@Service
public class PolicyInstancesHolder {

    Map<String, String> cellPolicyMap = new HashMap<>();

    public void addPolicy(String policyId, String body) {
        cellPolicyMap.put(policyId, body);
    }

    public void removePolicy(String policyId) {
        cellPolicyMap.remove(policyId);
    }

    public boolean containsPoliciesForCell(String cell) {
        return cellPolicyMap.values().stream().map(this::getCellListFromPolicyInstance).flatMap(List::stream)
                       .anyMatch(c -> c.equals(cell));
    }

    private List<String> getCellListFromPolicyInstance(String policyInstance) {
        RanUeHandoverOnPolicyAction.UeHandoverPolicy policy =
                JsonUtils.INSTANCE.deserialize(policyInstance, RanUeHandoverOnPolicyAction.UeHandoverPolicy.class);
        return policy.getResources().stream().flatMap(resources -> resources.getCellIdList().stream())
                       .collect(Collectors.toList());
    }
}
