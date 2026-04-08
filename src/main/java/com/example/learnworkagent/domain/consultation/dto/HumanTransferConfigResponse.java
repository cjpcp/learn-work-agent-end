package com.example.learnworkagent.domain.consultation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HumanTransferConfigResponse {
    private Long id;
    private String businessType;
    private String assignMode;
    private Long roleId;
    private String roleName;
    private List<Long> userIds;
    private List<String> userNames;
    private Integer priority;
    private Boolean enabled;
    private String remark;
}
