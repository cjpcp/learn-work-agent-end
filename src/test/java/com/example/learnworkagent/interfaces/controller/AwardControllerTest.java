package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.award.dto.AwardApplicationRequest;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.service.AwardApplicationService;
import com.example.learnworkagent.domain.user.entity.Admin;
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

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AwardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AwardApplicationService awardApplicationService;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private RoleRepository roleRepository;

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
    void submitApplication_shouldReturnCreatedApplication() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testAdmin));

        AwardApplicationRequest request = new AwardApplicationRequest();
        request.setApplicationType("scholarship");
        request.setAwardName("National Scholarship");
        request.setAmount(new BigDecimal("5000"));
        request.setReason("Excellent academic performance");

        AwardApplication savedApplication = new AwardApplication();
        savedApplication.setId(1L);
        savedApplication.setApplicationType("scholarship");
        savedApplication.setAwardName("National Scholarship");

        when(awardApplicationService.submitAwardApplication(any(), any())).thenReturn(savedApplication);

        mockMvc.perform(post("/api/v1/award/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getApplication_shouldReturnApplication() throws Exception {
        AwardApplication application = new AwardApplication();
        application.setId(1L);
        application.setApplicationType("scholarship");

        when(awardApplicationService.getApplicationById(1L)).thenReturn(application);

        mockMvc.perform(get("/api/v1/award/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getMyApplications_shouldReturnPageResult() throws Exception {
        PageResult<AwardApplication> pageResult = new PageResult<>(Collections.emptyList(), 0L, 1, 10);

        when(adminRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testAdmin));
        when(awardApplicationService.getUserApplications(any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/award/applications/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getPendingApplications_shouldReturnPageResult() throws Exception {
        PageResult<AwardApplication> pageResult = new PageResult<>(Collections.emptyList(), 0L, 1, 10);

        when(adminRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testAdmin));
        when(awardApplicationService.getPendingApplications(any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/award/applications/pending")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void approveApplication_shouldReturnSuccess() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testAdmin));

        mockMvc.perform(post("/api/v1/award/applications/1/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvalStatus\":\"APPROVED\",\"approvalComment\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void cancelApplication_shouldReturnSuccess() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testAdmin));

        mockMvc.perform(delete("/api/v1/award/applications/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}