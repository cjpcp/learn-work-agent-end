package com.example.learnworkagent.domain.process.dto;

import lombok.Data;


@Data
public class ProcessItem {
    private String id;
    private String name;
    private String type;
    private String createTime;
    private String status;
    private String description;
    private Boolean allowAction;
}
