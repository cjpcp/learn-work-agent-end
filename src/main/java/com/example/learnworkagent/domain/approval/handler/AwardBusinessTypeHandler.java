package com.example.learnworkagent.domain.approval.handler;

import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwardBusinessTypeHandler implements BusinessTypeHandler {

    private static final String INSTANCE_PENDING = ApprovalStatusEnum.PENDING.getCode();

    private final AwardApplicationRepository awardApplicationRepository;

    @Override
    public String getBusinessType() {
        return NotificationBusinessTypeEnum.AWARD.getCode();
    }

    @Override
    public String getBusinessName() {
        return "奖助申请";
    }

    @Override
    public void updateStatus(Long businessId, String status, Long approverId, String approvalComment) {
        awardApplicationRepository.findById(businessId).ifPresent(application -> {
            application.setApprovalStatus(status);
            application.setApproverId(approverId);
            if (INSTANCE_PENDING.equals(status)) {
                application.setApprovalComment(null);
                application.setApprovalTime(null);
            } else {
                application.setApprovalComment(approvalComment);
                application.setApprovalTime(LocalDateTime.now());
            }
            awardApplicationRepository.save(application);
        });
    }

    @Override
    public void fillNotificationBusinessInfo(NotificationMessage message, Long businessId) {
        AwardApplication application = awardApplicationRepository.findById(businessId).orElse(null);
        if (application != null) {
            message.setApplicantName(application.getStudentName());
            message.setApplicationType(application.getApplicationType());
            message.setAwardName(application.getAwardName());
            message.setAmount(application.getAmount());
        }
    }
}