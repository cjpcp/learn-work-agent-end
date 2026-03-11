package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 审批实例仓库
 */
@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

    /**
     * 根据业务类型和业务ID查询审批实例
     */
    Optional<ApprovalInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId);
}
