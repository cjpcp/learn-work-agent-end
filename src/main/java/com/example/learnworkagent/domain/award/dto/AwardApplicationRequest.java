package com.example.learnworkagent.domain.award.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 奖助申请请求DTO.
 * <p>封装奖助申请的类型、名称、金额等请求参数.</p>
 *
 * @author system
 */
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

    /** 院系名称 */
    private String departmentName;

    /** 年级 */
    private String grade;

    /** 班级 */
    private String className;
}