package com.example.learnworkagent.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 人工转接配置请求
 */
@Data
public class HumanTransferConfigRequest {

    @NotBlank(message = "业务分类不能为空")
    private String businessType;

    @NotBlank(message = "分配模式不能为空")
    private String assignMode;

    private Long roleId;

    private List<Long> userIds;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    private String remark;
}
