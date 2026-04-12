package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批步骤仓储层.
 * <p>提供对approval_stage表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    /**
     * 根据流程查询步骤列表，按步骤顺序排序.
     *
     * @param process 审批流程
     * @return 步骤列表
     */
    List<ApprovalStep> findByProcessOrderByStepOrderAsc(ApprovalProcess process);

}
