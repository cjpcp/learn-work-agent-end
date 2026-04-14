package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Role;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.repository.RoleRepository;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalService approvalService;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private RoleRepository roleRepository;

    private Admin testAdmin;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRoleName("ADMIN");

        testAdmin = new Admin();
        testAdmin.setId(1L);
        testAdmin.setUsername("testadmin");
        testAdmin.setNick("Test Admin");
        testAdmin.setRoleId(1L);
        testAdmin.setStatus(1);
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void getPendingTasks_shouldReturnTaskList() throws Exception {
        when(adminRepository.findByUsername("testadmin")).thenReturn(java.util.Optional.of(testAdmin));
        when(approvalService.getPendingTasks(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/approval/tasks/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void processTask_shouldReturnSuccess() throws Exception {
        when(adminRepository.findByUsername("testadmin")).thenReturn(java.util.Optional.of(testAdmin));

        Map<String, Object> request = new HashMap<>();
        request.put("status", "APPROVED");
        request.put("comment", "Approved");

        mockMvc.perform(post("/api/v1/approval/tasks/1/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void processTask_withEmptyStatus_shouldHandleGracefully() throws Exception {
        when(adminRepository.findByUsername("testadmin")).thenReturn(java.util.Optional.of(testAdmin));

        Map<String, Object> request = new HashMap<>();
        request.put("status", "");
        request.put("comment", "Test");

        mockMvc.perform(post("/api/v1/approval/tasks/1/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void getApprovalInstance_shouldReturnInstance() throws Exception {
        when(approvalService.getApprovalInstance("leave", 1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/approval/instances/leave/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void getApprovalInstance_withAwardType_shouldReturnInstance() throws Exception {
        when(approvalService.getApprovalInstance("award", 1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/approval/instances/award/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }
}