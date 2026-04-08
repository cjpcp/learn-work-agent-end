package com.example.learnworkagent.domain.award.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AwardApplicationRequest {

    @NotBlank(message = "申请类型不能为空")
    private String applicationType;

    @NotBlank(message = "申请名称不能为空")
    private String awardName;

    private BigDecimal amount;

    private String reason;

    private String attachmentUrls;

    private String studentName;
}
