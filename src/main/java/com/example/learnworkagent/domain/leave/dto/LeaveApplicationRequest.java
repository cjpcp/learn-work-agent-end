package com.example.learnworkagent.domain.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 请假申请请求DTO.
 * <p>封装请假申请的类型、日期、原因等请求参数.</p>
 *
 * @author system
 */
@Data
public class LeaveApplicationRequest {

    @NotBlank(message = "请假类型不能为空")
    private String leaveType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String reason;

    private String attachmentUrl;

    private String studentName;

    private String departmentName;

    private String grade;

    private String className;
}
