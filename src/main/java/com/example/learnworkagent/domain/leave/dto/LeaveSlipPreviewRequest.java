package com.example.learnworkagent.domain.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveSlipPreviewRequest {

    @NotBlank(message = "姓名不能为空")
    private String studentName;

    private String cardNumber;

    private String grade;

    private String className;

    private String phone;

    @NotBlank(message = "请假类型不能为空")
    private String leaveType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String reason;
}