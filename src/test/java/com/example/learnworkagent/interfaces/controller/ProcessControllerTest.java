package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.process.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessService processService;

    @Test
    void getPendingAll_withoutAuth_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/process/pending/all")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPendingAward_withoutAuth_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/process/pending/award")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPendingLeave_withoutAuth_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/process/pending/leave")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCompletedAll_withoutAuth_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/process/completed/all")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }
}