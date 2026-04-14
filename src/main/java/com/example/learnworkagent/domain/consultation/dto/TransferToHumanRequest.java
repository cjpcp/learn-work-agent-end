package com.example.learnworkagent.domain.consultation.dto;

import lombok.Data;

/**
 * 转人工请求DTO.
 * <p>封装用户申请转人工服务的请求参数.</p>
 *
 * @author system
 */
@Data
public class TransferToHumanRequest {

    /**
     * 转人工原因
     */
    private String reason;

    /**
     * 问题类型
     */
    private String questionType;

    /**
     * 用户填写的转接问题描述
     */
    private String questionText;
}
