package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.consultation.service.HumanTransferConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HumanTransferConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HumanTransferConfigService humanTransferConfigService;

    @Test
    void listConfigs_withoutAuth_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/consultation/transfer-config"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void createConfig_withUserRole_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/consultation/transfer-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"general\",\"assignMode\":\"ROLE\",\"roleId\":1,\"priority\":1,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateConfig_withUserRole_shouldBeForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/consultation/transfer-config/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"general\",\"assignMode\":\"ROLE\",\"roleId\":1,\"priority\":1,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void deleteConfig_withUserRole_shouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/consultation/transfer-config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }
}