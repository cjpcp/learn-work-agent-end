package com.example.learnworkagent.domain.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalTaskDTO {
    private Long id;
    private Long businessId;
    private String businessType;
    private String status;
    private LocalDateTime createTime;
    private Long approverId;
    private String stepName;
}