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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.data.ue.UserEquipmentNotification;
import org.onap.a1pesimulator.service.cell.RanCellService;
import org.onap.a1pesimulator.service.ue.RanUeService;
import org.onap.a1pesimulator.util.JsonUtils;
import org.onap.a1pesimulator.util.JsonUtils.JsonUtilsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RanUeHandoverOnPolicyAction implements OnPolicyAction {

    private static final String TOPIC_UE = "/topic/userEquipment";
    private static final String POLICY_EXAMPLE =
            "{ \"scope\": { \"ueId\": \"emergency_samsung_s10_01\" }, \"resources\": [ { \"cellIdList\": [ \"Cell1\" ], \"preference\": \"AVOID\" } ] }";
    private static final Logger log = LoggerFactory.getLogger(RanUeHandoverOnPolicyAction.class);

    private final RanUeService ranUeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RanCellService ranCellService;
    private final PolicyInstancesHolder policyHolder;

    public RanUeHandoverOnPolicyAction(SimpMessagingTemplate messagingTemplate, RanUeService ranUeService,
            RanCellService ranCellService, PolicyInstancesHolder policyHolder) {
        this.messagingTemplate = messagingTemplate;
        this.ranUeService = ranUeService;
        this.ranCellService = ranCellService;
        this.policyHolder = policyHolder;
    }

    @Override
    public boolean isForMe(Integer policyTypeId, String policyId, String body) {
        try {
            JsonUtils.INSTANCE.deserialize(body, UeHandoverPolicy.class);
            return true;
        } catch (JsonUtilsException ex) {
            log.info(
                    "Policy {} is not for me because policy body doesn't comply with Ue Handover policy. Follow example: {}",
                    policyId, POLICY_EXAMPLE);
            return false;
        }
    }

    @Override
    public void onPolicy(Integer policyTypeId, String policyId, String body) {
        UeHandoverPolicy policy = JsonUtils.INSTANCE.deserialize(body, UeHandoverPolicy.class);
        String ueId = policy.getScope().getUeId();
        List<String> cellId = policy.getResources().stream().flatMap(resources -> resources.getCellIdList().stream())
                                      .collect(Collectors.toList());

        if (ueId == null || cellId.isEmpty()) {
            log.warn("Cannot handover because {} is not provided in preload! Follow example: {}",
                    ueId == null ? "ueId" : "cellId", POLICY_EXAMPLE);
            return;
        }

        Optional<String> activeCellId = getActiveCellForUE(ueId);

        if (!activeCellId.isPresent()) {
            log.warn("Cannot handover ue {} because there is no active cell in range", ueId);
            return;
        }

        ranUeService.handover(ueId, activeCellId.get());
        messagingTemplate.convertAndSend(TOPIC_UE, new UserEquipmentNotification(ueId, activeCellId.get()));
    }

    private Optional<String> getActiveCellForUE(String ue) {
        Optional<UserEquipment> equipment = ranUeService.getUserEquipment(ue);
        if (!equipment.isPresent()) {
            log.warn("Cannot handover because is not ue with id: {}", ue);
            return Optional.empty();
        }

        return ranCellService.getAllCellsWithStatus().stream().filter(c -> !c.isFailureMode())
                       .map(cellWithStatus -> cellWithStatus.getCell().getIdentifier())
                       .filter(cell -> ranUeService.canHandover(ue, cell))
                       .filter(cell -> !policyHolder.containsPoliciesForCell(cell)).findFirst();
    }

    @Getter
    public static class UeHandoverPolicy {

        private Scope scope;
        private List<Resources> resources;
    }

    @Getter
    public static class Scope {

        private String ueId;
    }

    @Getter
    public static class Resources {

        private List<String> cellIdList;
        private Preference preference;
    }

    public enum Preference {
        SHALL("SHALL"), PREFER("PREFER"), AVOID("AVOID"), FORBID("FORBID");
        public final String value;

        Preference(String stateName) {
            this.value = stateName;
        }
    }
}
