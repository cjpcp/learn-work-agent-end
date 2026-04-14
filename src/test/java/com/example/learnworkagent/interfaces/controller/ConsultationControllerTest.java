package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.consultation.dto.ConsultationRequest;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.service.ConsultationService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferConfigService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferService;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferRepository;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConsultationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultationService consultationService;

    @MockBean
    private HumanTransferService humanTransferService;

    @MockBean
    private HumanTransferConfigService humanTransferConfigService;

    @MockBean
    private HumanTransferRepository humanTransferRepository;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private OssService ossService;

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
    void submitQuestion_shouldReturnCreatedQuestion() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));

        ConsultationQuestion question = new ConsultationQuestion();
        question.setId(1L);
        question.setQuestionText("Test question");

        when(consultationService.submitQuestion(any(), any(), any(), any(), any(), any(), any())).thenReturn(question);

        mockMvc.perform(post("/api/v1/consultation/questions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Test question\",\"questionType\":\"general\",\"category\":\"tech\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getQuestion_shouldReturnQuestion() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));

        ConsultationQuestion question = new ConsultationQuestion();
        question.setId(1L);
        question.setQuestionText("Test question");

        when(consultationService.getQuestionById(1L)).thenReturn(question);

        mockMvc.perform(get("/api/v1/consultation/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getQuestionHistory_shouldReturnHistory() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));
        when(consultationService.getConversationHistory(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/consultation/questions/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getMyQuestions_shouldReturnPageResult() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));
        when(consultationService.getUserQuestions(any(), any())).thenReturn(new com.example.learnworkagent.common.dto.PageResult<>(Collections.emptyList(), 0L, 1, 10));

        mockMvc.perform(get("/api/v1/consultation/questions/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void uploadVoice_shouldReturnUrl() throws Exception {
        when(adminRepository.findByUsername("testuser")).thenReturn(Optional.of(testAdmin));
        when(ossService.uploadConsultationFile(any(), any(), any())).thenReturn("https://example.com/voice.wav");

        mockMvc.perform(multipart("/api/v1/consultation/upload/voice")
                        .file("file", "test content".getBytes())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }
}