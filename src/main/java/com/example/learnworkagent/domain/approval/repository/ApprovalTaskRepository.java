package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批任务仓库
 */
@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {

    /**
     * 根据审批实例查询任务
     */
    List<ApprovalTask> findByInstanceOrderByTaskOrderAsc(ApprovalInstance instance);

    /**
     * 根据审批人ID和状态查询待审批任务
     */
    List<ApprovalTask> findByApproverIdAndStatus(Long approverId, String status);

    /**
     * 根据审批实例和步骤查询任务
     */
    List<ApprovalTask> findByInstanceAndStepId(ApprovalInstance instance, Long stepId);
}
