package com.example.learnworkagent.domain.process.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程列表响应DTO.
 * <p>封装待办和已完成流程的列表.</p>
 *
 * @author system
 */
@Data
public class ProcessListResponse {
    private List<ProcessItem> pending;
    private List<ProcessItem> completed;
}
