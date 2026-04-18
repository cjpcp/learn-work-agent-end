package com.example.learnworkagent.domain.approval.service;

import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;

import java.util.List;

/**
 * 审批服务接口.
 * <p>定义审批流程的核心业务逻辑，包括创建审批实例、处理审批任务等.</p>
 *
 * @author system
 */
public interface ApprovalService {

    /**
     * 为业务创建审批流程实例.
     *
     * @param businessType  业务类型（如leave、award等）
     * @param businessId    业务记录ID
     * @param applicantId   申请人ID
     * @param applicantInfo 申请人信息（JSON格式）
     */
    void createApprovalInstance(String businessType, Long businessId, Long applicantId, String applicantInfo);

    /**
     * 处理指定的审批任务.
     *
     * @param taskId     审批任务ID
     * @param approverId 审批人ID
     * @param status     审批状态（通过/拒绝）
     * @param comment    审批意见
     * @return 更新后的审批任务
     */
    ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment);

    /**
     * 获取指定业务的审批实例.
     *
     * @param businessType 业务类型
     * @param businessId   业务记录ID
     * @return 审批实例，若不存在则返回null
     */
    ApprovalInstance getApprovalInstance(String businessType, Long businessId);

    /**
     * 获取指定审批人的待审批任务列表.
     *
     * @param approverId 审批人ID
     * @return 待审批任务列表
     */
    java.util.List<ApprovalTaskDTO> getPendingTasks(Long approverId);

    /**
     * 获取审批实例当前进行中的所有审批任务.
     *
     * @param instance 审批实例
     * @return 进行中的审批任务列表
     */
    java.util.List<ApprovalTask> getCurrentTasks(ApprovalInstance instance);

    /**
     * 取消指定业务的审批流程实例.
     *
     * @param businessType 业务类型（如leave、award等）
     * @param businessId   业务记录ID
     */
    void cancelApprovalInstance(String businessType, Long businessId);

    /**
     * 拒绝指定业务的所有进行中的审批任务（用于材料预审失败等情况）.
     *
     * @param businessType 业务类型
     * @param businessId   业务记录ID
     * @param reason       拒绝原因
     */
    void rejectPendingTasks(String businessType, Long businessId, String reason);

    ApprovalTask getTaskById(Long taskId);

    List<ApprovalTaskDTO> getCompletedTasksByApprover(Long approverId);
}
