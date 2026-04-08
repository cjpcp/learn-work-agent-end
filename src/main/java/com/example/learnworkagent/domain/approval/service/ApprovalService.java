package com.example.learnworkagent.domain.approval.service;

import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;

/**
 * 审批服务
 */
public interface ApprovalService {

    /**
     * 为业务创建审批流程实例
     *
     * @param businessType  业务类型
     * @param businessId    业务ID
     * @param applicantId   申请人ID
     * @param applicantInfo 申请人信息（JSON格式）
     */
    void createApprovalInstance(String businessType, Long businessId, Long applicantId, String applicantInfo);

    /**
     * 处理审批任务
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param status 审批状态
     * @param comment 审批意见
     * @return 更新后的任务
     */
    ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment);

    /**
     * 获取审批实例
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 审批实例
     */
    ApprovalInstance getApprovalInstance(String businessType, Long businessId);

    /**
     * 获取用户的待审批任务
     * @param approverId 审批人ID
     * @return 待审批任务列表
     */
    java.util.List<ApprovalTaskDTO> getPendingTasks(Long approverId);

    /**
     * 获取实例当前进行中的审批任务
     * @param instance 审批实例
     * @return 当前任务
     */
    java.util.List<ApprovalTask> getCurrentTasks(ApprovalInstance instance);
}
