package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 审批流程仓库
 */
@Repository
public interface ApprovalProcessRepository extends JpaRepository<ApprovalProcess, Long> {

    /**
     * 根据流程类型查询启用的流程
     */
    Optional<ApprovalProcess> findByProcessTypeAndEnabledTrue(String processType);

    /**
     * 根据流程类型查询最新版本的流程
     */
    Optional<ApprovalProcess> findFirstByProcessTypeOrderByVersionDesc(String processType);
}
