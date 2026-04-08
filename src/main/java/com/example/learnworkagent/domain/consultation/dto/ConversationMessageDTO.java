package com.example.learnworkagent.domain.consultation.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ConversationMessageDTO {
    private String id;
    private String conversationId;
    private Map<String, Object> inputs;
    private String query;
    private String answer;
    private String messageType;
    private String createdAt;
    private List<Map<String, Object>> files;
}