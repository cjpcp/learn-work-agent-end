package com.example.learnworkagent.domain.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalStepRequest {

    @NotBlank(message = "步骤名称不能为空")
    private String stepName;

    @NotNull(message = "步骤顺序不能为空")
    private Integer stepOrder;

    @NotBlank(message = "审批类型不能为空")
    private String approvalType;

    @NotBlank(message = "审批角色不能为空")
    private String approverRole;

    private Long approverUserId;

    private Boolean mustPass = true;

    @NotNull(message = "流程ID不能为空")
    private Long processId;
}
