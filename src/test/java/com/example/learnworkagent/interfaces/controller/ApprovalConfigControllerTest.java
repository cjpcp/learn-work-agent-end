package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.approval.dto.ApprovalProcessRequest;
import com.example.learnworkagent.domain.approval.dto.ApprovalStepRequest;
import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import com.example.learnworkagent.domain.approval.repository.ApprovalProcessRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalProcessRepository processRepository;

    @MockBean
    private ApprovalStepRepository stepRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getProcesses_shouldReturnProcessList() throws Exception {
        when(processRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/approval/config/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createProcess_withAdminRole_shouldReturnSuccess() throws Exception {
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setProcessName("Test Process");
        request.setProcessType("leave");
        request.setDescription("Test Description");

        ApprovalProcess savedProcess = new ApprovalProcess();
        savedProcess.setId(1L);
        savedProcess.setProcessName("Test Process");

        when(processRepository.save(any(ApprovalProcess.class))).thenReturn(savedProcess);

        mockMvc.perform(post("/api/v1/approval/config/processes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createProcess_withUserRole_shouldBeHandled() throws Exception {
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setProcessName("Test Process");
        request.setProcessType("leave");

        mockMvc.perform(post("/api/v1/approval/config/processes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateProcess_shouldReturnUpdatedProcess() throws Exception {
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setProcessName("Updated Process");
        request.setProcessType("award");
        request.setDescription("Updated Description");

        ApprovalProcess existingProcess = new ApprovalProcess();
        existingProcess.setId(1L);
        existingProcess.setProcessName("Original Process");

        ApprovalProcess savedProcess = new ApprovalProcess();
        savedProcess.setId(1L);
        savedProcess.setProcessName("Updated Process");

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(existingProcess));
        when(processRepository.save(any(ApprovalProcess.class))).thenReturn(savedProcess);

        mockMvc.perform(put("/api/v1/approval/config/processes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteProcess_shouldReturnSuccess() throws Exception {
        ApprovalProcess existingProcess = new ApprovalProcess();
        existingProcess.setId(1L);
        existingProcess.setProcessName("Test Process");

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(existingProcess));
        when(stepRepository.findByProcessOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(delete("/api/v1/approval/config/processes/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getSteps_shouldReturnStepList() throws Exception {
        ApprovalProcess process = new ApprovalProcess();
        process.setId(1L);

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(process));
        when(stepRepository.findByProcessOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/approval/config/processes/1/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addStep_shouldReturnCreatedStep() throws Exception {
        ApprovalStepRequest request = new ApprovalStepRequest();
        request.setProcessId(1L);
        request.setStepOrder(1);
        request.setStepName("Test Step");
        request.setApprovalType("ROLE");
        request.setApproverRole("ADMIN");

        ApprovalProcess process = new ApprovalProcess();
        process.setId(1L);

        ApprovalStep savedStep = new ApprovalStep();
        savedStep.setId(1L);
        savedStep.setStepOrder(1);

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(process));
        when(stepRepository.save(any(ApprovalStep.class))).thenReturn(savedStep);

        mockMvc.perform(post("/api/v1/approval/config/steps")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteStep_shouldReturnSuccess() throws Exception {
        ApprovalStep existingStep = new ApprovalStep();
        existingStep.setId(1L);

        when(stepRepository.findById(1L)).thenReturn(java.util.Optional.of(existingStep));

        mockMvc.perform(delete("/api/v1/approval/config/steps/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void enableProcess_shouldReturnEnabledProcess() throws Exception {
        ApprovalProcess process = new ApprovalProcess();
        process.setId(1L);
        process.setEnabled(false);

        ApprovalProcess savedProcess = new ApprovalProcess();
        savedProcess.setId(1L);
        savedProcess.setEnabled(true);

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(process));
        when(processRepository.save(any(ApprovalProcess.class))).thenReturn(savedProcess);

        mockMvc.perform(post("/api/v1/approval/config/processes/1/enable")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void disableProcess_shouldReturnDisabledProcess() throws Exception {
        ApprovalProcess process = new ApprovalProcess();
        process.setId(1L);
        process.setEnabled(true);

        ApprovalProcess savedProcess = new ApprovalProcess();
        savedProcess.setId(1L);
        savedProcess.setEnabled(false);

        when(processRepository.findById(1L)).thenReturn(java.util.Optional.of(process));
        when(processRepository.save(any(ApprovalProcess.class))).thenReturn(savedProcess);

        mockMvc.perform(post("/api/v1/approval/config/processes/1/disable")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }
}