package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 审批流程仓储层.
 * <p>提供对approval_process表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface ApprovalProcessRepository extends JpaRepository<ApprovalProcess, Long> {

    /**
     * 根据流程类型查询启用的流程.
     *
     * @param processType 流程类型
     * @return 启用的审批流程
     */
    Optional<ApprovalProcess> findByProcessTypeAndEnabledTrue(String processType);

}
