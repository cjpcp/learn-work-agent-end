package com.example.learnworkagent.domain.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批任务DTO.
 * <p>封装审批任务的展示信息.</p>
 *
 * @author system
 */
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