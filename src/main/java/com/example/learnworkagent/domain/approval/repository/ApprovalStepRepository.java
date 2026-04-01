package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批步骤仓库
 */
@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    /**
     * 根据流程ID查询步骤，按步骤顺序排序
     */
    List<ApprovalStep> findByProcessOrderByStepOrderAsc(ApprovalProcess process);

}
