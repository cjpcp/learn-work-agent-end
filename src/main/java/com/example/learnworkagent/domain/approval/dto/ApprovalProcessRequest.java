package com.example.learnworkagent.domain.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批流程请求DTO.
 * <p>封装审批流程的创建/更新请求参数.</p>
 *
 * @author system
 */
@Data
public class ApprovalProcessRequest {

    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @NotBlank(message = "流程类型不能为空")
    private String processType;

    private String description;

    private Boolean enabled = true;

    private Integer version = 1;
}
