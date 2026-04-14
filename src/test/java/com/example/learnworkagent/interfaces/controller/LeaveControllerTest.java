package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveApplicationService leaveApplicationService;

    @MockBean
    private AdminRepository adminRepository;

    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = new Admin();
        testAdmin.setId(1L);
        testAdmin.setUsername("testuser");
        testAdmin.setNick("Test User");
        testAdmin.setRoleId(1L);
        testAdmin.setStatus(1);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getApplication_shouldReturnApplication() throws Exception {
        LeaveApplication application = new LeaveApplication();
        application.setId(1L);
        application.setLeaveType("sick");

        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));
        when(leaveApplicationService.getApplicationById(1L)).thenReturn(application);

        mockMvc.perform(get("/api/v1/leave/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void submitApplication_shouldReturnSuccessWhenUserExists() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));

        mockMvc.perform(post("/api/v1/leave/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"sick\",\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-03\",\"reason\":\"sick\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getMyApplications_shouldReturnSuccess() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));

        mockMvc.perform(get("/api/v1/leave/applications/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void withdrawApplication_shouldHandleWhenUserExists() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));

        mockMvc.perform(delete("/api/v1/leave/applications/1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}