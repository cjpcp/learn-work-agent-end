package com.example.learnworkagent.domain.consultation.dto;

import lombok.Data;

/**
 * 转人工请求DTO
 */
@Data
public class TransferToHumanRequest {

    /**
     * 转人工原因
     */
    private String reason;
}
