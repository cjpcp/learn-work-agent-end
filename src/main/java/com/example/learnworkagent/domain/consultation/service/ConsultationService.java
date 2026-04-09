package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.consultation.dto.ConsultationRequest;
import com.example.learnworkagent.domain.consultation.dto.ConversationMessageDTO;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.repository.ConsultationQuestionRepository;
import com.example.learnworkagent.infrastructure.external.dify.DifyChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 咨询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationQuestionRepository consultationQuestionRepository;
    private final ConsultationAgentService consultationAgentService;
    private final DifyChatService difyChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 提交咨询问题
     */
    @Transactional
    public ConsultationQuestion submitQuestion(Long userId, String questionText, String questionType,
                                               String category, String voiceUrl, String sessionId,
                                               List<ConsultationRequest.FileInput> files) {
        ConsultationQuestion question = new ConsultationQuestion();
        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setCategory(category);
        question.setVoiceUrl(voiceUrl);
        question.setSessionId(sessionId);
        question.setStatus("PENDING");
        question.setFileUrls(serializeFileUrls(files));

        ConsultationQuestion saved = consultationQuestionRepository.save(question);

        // 异步调用Agent服务进行智能回答
        consultationAgentService.processQuestionAsync(saved.getId());

        return saved;
    }

    /**
     * 获取问题详情
     */
    public ConsultationQuestion getQuestionById(Long questionId) {
        return consultationQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "咨询问题不存在"));
    }

    /**
     * 分页查询用户的问题
     */
    public PageResult<ConsultationQuestion> getUserQuestions(Long userId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<ConsultationQuestion> page = consultationQuestionRepository
                .findByUserIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    /**
     * 查询用户在指定问题之前（含）的历史咨询记录
     */
    public List<ConsultationQuestion> getHistoryByUserIdUpToQuestion(Long userId, Long questionId) {
        ConsultationQuestion question = consultationQuestionRepository.findById(questionId).orElse(null);
        if (question != null && question.getSessionId() != null && !question.getSessionId().isBlank()) {
            return consultationQuestionRepository.findBySessionIdOrderByCreateTimeAsc(question.getSessionId());
        }
        return consultationQuestionRepository.findHistoryByUserIdUpToQuestion(userId, questionId);
    }

    /**
     * 获取问题的对话历史详情
     * 如果存在 conversation_id，则从 Dify 获取完整对话历史
     * 否则返回本地存储的问题信息
     */
    public List<ConversationMessageDTO> getConversationHistory(Long questionId) {
        ConsultationQuestion question = getQuestionById(questionId);

        if (question.getConversationId() != null && !question.getConversationId().isBlank()) {
            List<Map<String, Object>> messages = difyChatService.getConversationMessages(
                    question.getConversationId(),
                    String.valueOf(question.getUserId())
            );

            return messages.stream().map(this::convertToDTO).toList();
        }

        return List.of(convertLocalQuestionToDTO(question));
    }

    private ConversationMessageDTO convertToDTO(Map<String, Object> message) {
        ConversationMessageDTO dto = new ConversationMessageDTO();
        dto.setId((String) message.get("id"));
        dto.setConversationId((String) message.get("conversation_id"));
        dto.setInputs((Map<String, Object>) message.get("inputs"));
        dto.setQuery((String) message.get("query"));
        dto.setAnswer((String) message.get("answer"));
        dto.setMessageType((String) message.get("message_type"));
        dto.setCreatedAt((String) message.get("created_at"));
        return dto;
    }

    private ConversationMessageDTO convertLocalQuestionToDTO(ConsultationQuestion question) {
        ConversationMessageDTO dto = new ConversationMessageDTO();
        dto.setId(String.valueOf(question.getId()));
        dto.setConversationId(question.getConversationId());
        dto.setQuery(question.getQuestionText());
        dto.setAnswer(question.getAiAnswer());
        dto.setMessageType(question.getQuestionType());
        dto.setCreatedAt(question.getCreateTime() != null ? question.getCreateTime().toString() : null);
        return dto;
    }

    /**
     * 评价问题回答
     */
    @Transactional
    public void rateQuestion(Long questionId, Integer satisfactionScore) {
        ConsultationQuestion question = getQuestionById(questionId);
        question.setSatisfactionScore(satisfactionScore);
        consultationQuestionRepository.save(question);
    }

    /**
     * 提交咨询问题（流式响应）
     */
    public Flux<String> submitQuestionStream(Long userId, String questionText, String questionType,
                                             String category, String voiceUrl, String sessionId,
                                             List<ConsultationRequest.FileInput> files) {
        // 保存问题
        ConsultationQuestion question = new ConsultationQuestion();
        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setCategory(category);
        question.setVoiceUrl(voiceUrl);
        question.setSessionId(sessionId);
        question.setStatus("PENDING");
        question.setFileUrls(serializeFileUrls(files));

        ConsultationQuestion saved = consultationQuestionRepository.save(question);

        Flux<String> res = consultationAgentService.processQuestionStream(saved.getId());
        log.info("res1:{}", res);
        return res;
    }

    /**
     * 将文件列表的 URL 序列化为 JSON 字符串保存到数据库
     */
    private String serializeFileUrls(List<ConsultationRequest.FileInput> files) {
        if (files == null || files.isEmpty()) return null;
        try {
            List<String> urls = files.stream()
                    .map(ConsultationRequest.FileInput::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
            return urls.isEmpty() ? null : objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            log.warn("序列化文件URL失败", e);
            return null;
        }
    }
}
