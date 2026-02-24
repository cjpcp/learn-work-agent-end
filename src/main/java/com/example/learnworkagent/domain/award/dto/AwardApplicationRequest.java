package com.example.learnworkagent.domain.award.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 奖助申请请求DTO
 */
@Data
public class AwardApplicationRequest {

    /**
     * 申请类型
     */
    @NotBlank(message = "申请类型不能为空")
    private String applicationType;

    /**
     * 奖助名称
     */
    @NotBlank(message = "申请名称不能为空")
    private String awardName;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 申请理由
     */
    private String reason;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;
}
