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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咨询服务.
 * <p>提供咨询问题的提交、查询、历史记录等业务逻辑.</p>
 *
 * @author system
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
     * 提交咨询问题.
     * <p>创建问题记录后，异步调用Agent服务进行智能回答.</p>
     *
     * @param userId      提问用户ID
     * @param questionText 问题文本内容
     * @param questionType 问题类型（TEXT/VOICE）
     * @param category    问题分类
     * @param voiceUrl    语音文件URL（可选）
     * @param sessionId   会话ID（可选，用于关联多轮对话）
     * @param files       附件列表（可选）
     * @return 创建的咨询问题记录
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

        consultationAgentService.processQuestionAsync(saved.getId());

        return saved;
    }

    /**
     * 根据ID获取咨询问题详情.
     *
     * @param questionId 问题ID
     * @return 咨询问题详情
     * @throws BusinessException 问题不存在时抛出
     */
    public ConsultationQuestion getQuestionById(Long questionId) {
        return consultationQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "咨询问题不存在"));
    }

    /**
     * 分页查询指定用户的咨询问题.
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 分页后的咨询问题列表
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
     * 查询指定会话中，在指定问题之前的咨询历史记录.
     * <p>如果问题存在sessionId，则查询同一会话的所有记录；否则查询用户在该问题之前的历史.</p>
     *
     * @param userId     用户ID
     * @param questionId 问题ID（作为时间锚点）
     * @return 咨询问题历史列表
     */
    public List<ConsultationQuestion> getHistoryByUserIdUpToQuestion(Long userId, Long questionId) {
        ConsultationQuestion question = consultationQuestionRepository.findById(questionId).orElse(null);
        if (question != null && question.getSessionId() != null && !question.getSessionId().isBlank()) {
            return consultationQuestionRepository.findBySessionIdOrderByCreateTimeAsc(question.getSessionId());
        }
        return consultationQuestionRepository.findHistoryByUserIdUpToQuestion(userId, questionId);
    }

    /**
     * 获取指定问题的对话历史详情.
     * <p>如果存在conversationId，则从Dify服务获取完整对话历史；否则返回本地存储的问题信息.</p>
     *
     * @param questionId 问题ID
     * @return 对话消息列表
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
        String fileUrlsJson = question.getFileUrls();
        if (fileUrlsJson != null && !fileUrlsJson.isBlank()) {
            try {
                List<String> urls = objectMapper.readValue(fileUrlsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                if (urls != null && !urls.isEmpty()) {
                    List<Map<String, Object>> files = new ArrayList<>();
                    for (String url : urls) {
                        Map<String, Object> fileMap = new HashMap<>();
                        fileMap.put("url", url);
                        String lower = url.toLowerCase();
                        fileMap.put("type", lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$") ? "image" : "document");
                        int lastSlash = url.lastIndexOf('/');
                        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                            String name = url.substring(lastSlash + 1);
                            int qIdx = name.indexOf('?');
                            if (qIdx > 0) name = name.substring(0, qIdx);
                            try { name = java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8.name()); } catch (Exception ignored) {}
                            fileMap.put("name", name);
                        } else {
                            fileMap.put("name", "附件");
                        }
                        files.add(fileMap);
                    }
                    dto.setFiles(files);
                }
            } catch (Exception e) {
                log.warn("解析fileUrls失败, questionId: {}", question.getId(), e);
            }
        }
        return dto;
    }

    /**
     * 对指定问题的回答进行满意度评价.
     *
     * @param questionId        问题ID
     * @param satisfactionScore 满意度评分
     */
    @Transactional
    public void rateQuestion(Long questionId, Integer satisfactionScore) {
        ConsultationQuestion question = getQuestionById(questionId);
        question.setSatisfactionScore(satisfactionScore);
        consultationQuestionRepository.save(question);
    }

    /**
     * 提交咨询问题并返回流式响应.
     * <p>用于需要实时获取AI回答的场景，返回Flux流式响应.</p>
     *
     * @param userId      提问用户ID
     * @param questionText 问题文本内容
     * @param questionType 问题类型（TEXT/VOICE）
     * @param category    问题分类
     * @param voiceUrl    语音文件URL（可选）
     * @param sessionId   会话ID（可选）
     * @param files       附件列表（可选）
     * @return AI回答内容的流
     */
    public Flux<String> submitQuestionStream(Long userId, String questionText, String questionType,
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

        Flux<String> res = consultationAgentService.processQuestionStream(saved.getId());
        log.info("res1:{}", res);
        return res;
    }

    /**
     * 将文件列表序列化为JSON字符串.
     *
     * @param files 文件输入列表
     * @return JSON字符串，若无文件则返回null
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
