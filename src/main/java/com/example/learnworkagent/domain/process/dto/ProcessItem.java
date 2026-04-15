package com.example.learnworkagent.domain.process.dto;

import lombok.Data;

/**
 * 流程项DTO.
 * <p>封装单个流程记录的展示信息.</p>
 *
 * @author system
 */
@Data
public class ProcessItem {
    private String id;
    private String name;
    private String type;
    private String createTime;
    private String status;
    private String description;
    private Boolean allowAction;
    private String comment;
    private String materialStatus;
    private String materialComment;
}
