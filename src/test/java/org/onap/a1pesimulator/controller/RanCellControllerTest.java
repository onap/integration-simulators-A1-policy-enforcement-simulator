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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.a1pesimulator.controller.URLHelper.getRanCellControllerEndpoint;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.a1pesimulator.data.cell.CellDetails;
import org.onap.a1pesimulator.data.cell.RanCell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"localStore"})
public class RanCellControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    protected String mapToJson(Object obj) throws JsonProcessingException {
        mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

    protected <T> T mapFromJson(String json, Class<T> clazz)
            throws JsonParseException, JsonMappingException, IOException {
        mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetCells() throws Exception {
        MvcResult mvcResult = this.mvc.perform(
                MockMvcRequestBuilders.get(getRanCellControllerEndpoint()).accept(MediaType.APPLICATION_JSON_VALUE))
                                      .andDo(MockMvcResultHandlers.print()).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();

        RanCell[] ranCell = this.mapFromJson(content, RanCell[].class);

        assertTrue(ranCell.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetCellById() throws Exception {
        MvcResult mvcResult = this.mvc.perform(
                MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/{identifier}", 1)
                        .accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();

        CellDetails cell = this.mapFromJson(content, CellDetails.class);
        assertEquals(cell.getId(), "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartSendingFailureVesEvents() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/1/startFailure")
                                 .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStopSendingFailureVesEvents() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/1/stopFailure")
                                 .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartSendingVesEvents() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/1/start")
                                 .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStopSendingVesEvents() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/1/stop")
                                 .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVesEventStructure() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(getRanCellControllerEndpoint() + "/1/structure")
                                 .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());
    }
}
