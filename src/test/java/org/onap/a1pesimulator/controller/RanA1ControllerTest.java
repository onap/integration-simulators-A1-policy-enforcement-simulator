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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_CELL_ID;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_HANDOVER_CELL;
import static org.onap.a1pesimulator.TestHelpers.FIRST_UE_ID;
import static org.onap.a1pesimulator.controller.URLHelper.getHealthCheckEndpoint;
import static org.onap.a1pesimulator.controller.URLHelper.getPolicyPath;
import static org.onap.a1pesimulator.controller.URLHelper.getPolicyTypePath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.service.ue.RanUeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class RanA1ControllerTest {

    private static final String FIRST_POLICY_TYPE_ID = "1000";
    private static final Policy.Preference FIRST_POLICY_PREFERENCE = Policy.Preference.AVOID;
    private static final String FIRST_POLICY_ORIGINAL_ID = "ue1";
    private static final String TEST_POLICY_SCHEMA =
            "{\n" + "  \"name\": \"samsung_policy_type\",\n" + "  \"description\": \"samsung policy type;\",\n"
                    + "  \"policy_type_id\": 1000,\n" + "  \"create_schema\": {\n"
                    + "    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                    + "    \"title\": \"Samsung_demo\",\n" + "    \"description\": \"Samsung demo policy type\",\n"
                    + "    \"type\": \"object\",\n" + "    \"additionalProperties\": false,\n" + "    \"required\": [\n"
                    + "      \"scope\",\n" + "      \"resources\"\n" + "    ]\n" + "  }\n" + "}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    RanUeService ranUeService;

    @Test
    public void testHealthcheck() throws Exception {
        this.mvc.perform(get(getHealthCheckEndpoint()));
    }

    @Test
    public void testGetPolicyTypes() throws Exception {
        String firstPolicyURL = URLHelper.getPolicyTypePath("1000");
        mvc.perform(put(firstPolicyURL).content(TEST_POLICY_SCHEMA)).andExpect(status().isCreated());

        String[] policyTypes = mapper.readValue(getFromController(getPolicyTypePath()), String[].class);

        assertEquals(FIRST_POLICY_TYPE_ID, policyTypes[0]);
    }

    @Test
    public void testGetPolicy() throws Exception {
        // Remove escaping
        Policy policy = mapper.readValue(getFromController(getPolicyPath("3", "1")), Policy.class);

        assertEquals(FIRST_POLICY_ORIGINAL_ID, policy.getScope().getUeId());
        assertEquals(FIRST_POLICY_PREFERENCE, policy.getResources().get(0).getPreference());
    }

    @Test
    public void testPutPolicyAndTriggerHandover() throws Exception {
        String policyURL = getPolicyPath("3", "1");

        mvc.perform(put(policyURL).content(getHandoverPolicy())).andExpect(status().isAccepted());

        // Remove escaping
        Policy policy = mapper.readValue(getFromController(policyURL), Policy.class);

        assertEquals(FIRST_UE_ID, policy.getScope().getUeId());

        assertEquals(FIRST_POLICY_PREFERENCE, policy.getResources().get(0).getPreference());
        assertEquals(FIRST_UE_CELL_ID, policy.getResources().get(0).getCellIdList().get(0));

        UserEquipment userEquipment = ranUeService.getUserEquipment(FIRST_UE_ID).orElse(null);

        assertNotNull(userEquipment);
        assertEquals(FIRST_UE_HANDOVER_CELL, userEquipment.getCellId());

        mvc.perform(put(policyURL).content(getOriginalPolicy())).andExpect(status().isAccepted());
    }

    @Test
    public void testPutPolicyAndCantHandover() throws Exception {
        String policyURL = getPolicyPath("3", "1");

        // trigger cellId==null in the onPolicy
        mvc.perform(put(policyURL).content(getHandoverPolicyWithoutCellId())).andExpect(status().isAccepted());

        // trigger canHandover==false in the onPolicy
        mvc.perform(put(policyURL).content(getOriginalPolicy())).andExpect(status().isAccepted());
    }

    @Test
    public void shouldDeletePolicyInstanceAndReturn200() throws Exception {
        String url = getPolicyPath("3", "1");
        mvc.perform(delete(url)).andExpect(status().isAccepted());
    }

    @Test
    public void shouldReturn404WhenPolicyInstanceNotExists() throws Exception {
        String url = getPolicyPath("10", "321123");
        mvc.perform(delete(url)).andExpect(status().isNotFound());
    }

    private String getFromController(String url) throws Exception {
        return this.mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String getOriginalPolicy() throws JsonProcessingException {
        return mapper.writeValueAsString(
                Policy.builder().scope(Policy.Scope.builder().ueId(FIRST_POLICY_ORIGINAL_ID).build()).resources(
                        Lists.newArrayList(Policy.Resources.builder().cellIdList(Lists.newArrayList(FIRST_UE_CELL_ID))
                                                   .preference(Policy.Preference.AVOID).build())).build());
    }

    private String getHandoverPolicy() throws JsonProcessingException {
        return mapper.writeValueAsString(Policy.builder().scope(Policy.Scope.builder().ueId(FIRST_UE_ID).build())
                                                 .resources(Lists.newArrayList(Policy.Resources.builder().cellIdList(
                                                         Lists.newArrayList(FIRST_UE_CELL_ID)).preference(
                                                         Policy.Preference.AVOID).build())).build());
    }

    private String getHandoverPolicyWithoutCellId() throws JsonProcessingException {
        return mapper.writeValueAsString(Policy.builder().scope(Policy.Scope.builder().ueId(FIRST_UE_ID).build())
                                                 .resources(Lists.newArrayList(
                                                         Policy.Resources.builder().cellIdList(Lists.newArrayList())
                                                                 .preference(Policy.Preference.AVOID).build()))
                                                 .build());
    }

    @Data
    @Builder
    @JsonSerialize
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Policy {

        private Scope scope;
        private List<Resources> resources;

        @Data
        @Builder
        @JsonSerialize
        @NoArgsConstructor
        @AllArgsConstructor
        private static class Scope {

            private String ueId;
        }

        @Data
        @Builder
        @JsonSerialize
        @NoArgsConstructor
        @AllArgsConstructor
        private static class Resources {

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
}
