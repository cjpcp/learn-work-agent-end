package com.example.learnworkagent.domain.approval.handler;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;

public interface BusinessTypeHandler {

    String getBusinessType();

    String getBusinessName();

    void updateStatus(Long businessId, String status, Long approverId, String approvalComment);

    void fillNotificationBusinessInfo(NotificationMessage message, Long businessId);
}