package com.example.learnworkagent.domain.process.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProcessListResponse {
    private List<ProcessItem> pending;
    private List<ProcessItem> completed;
}
