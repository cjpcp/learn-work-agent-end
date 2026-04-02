package com.example.learnworkagent.domain.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
