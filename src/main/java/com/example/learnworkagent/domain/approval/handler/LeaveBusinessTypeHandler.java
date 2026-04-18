package com.example.learnworkagent.domain.approval.handler;

import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveTypeEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveBusinessTypeHandler implements BusinessTypeHandler {

    private static final String INSTANCE_PENDING = ApprovalStatusEnum.PENDING.getCode();

    private final LeaveApplicationRepository leaveApplicationRepository;

    @Override
    public String getBusinessType() {
        return NotificationBusinessTypeEnum.LEAVE.getCode();
    }

    @Override
    public String getBusinessName() {
        return "请假申请";
    }

    @Override
    public void updateStatus(Long businessId, String status, Long approverId, String approvalComment) {
        leaveApplicationRepository.findById(businessId).ifPresent(application -> {
            application.setApprovalStatus(status);
            application.setApproverId(approverId);
            if (INSTANCE_PENDING.equals(status)) {
                application.setApprovalComment(null);
                application.setApprovalTime(null);
            } else {
                application.setApprovalComment(approvalComment);
                application.setApprovalTime(LocalDateTime.now());
            }
            leaveApplicationRepository.save(application);
        });
    }

    @Override
    public void fillNotificationBusinessInfo(NotificationMessage message, Long businessId) {
        LeaveApplication application = leaveApplicationRepository.findById(businessId).orElse(null);
        if (application != null) {
            message.setApplicantName(application.getStudentName());
            message.setApplicationType(NotificationBusinessTypeEnum.LEAVE.getCode());
            message.setAwardName(LeaveTypeEnum.getDescriptionByCode(application.getLeaveType()));
            message.setLeaveStartDate(application.getStartDate());
            message.setLeaveEndDate(application.getEndDate());
            message.setLeaveDays(application.getDays());
            message.setLeaveReason(application.getReason());
        }
    }
}