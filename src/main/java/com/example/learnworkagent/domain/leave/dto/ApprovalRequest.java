package com.example.learnworkagent.domain.leave.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批请求DTO.
 * <p>封装审批操作的状态和意见.</p>
 *
 * @author system
 */
@Data
public class ApprovalRequest {

    @NotBlank(message = "审批状态不能为空")
    private String approvalStatus;

    private String approvalComment;
}
