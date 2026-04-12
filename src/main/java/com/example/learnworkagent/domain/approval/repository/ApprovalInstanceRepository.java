package com.example.learnworkagent.domain.approval.repository;

import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 审批实例仓储层.
 * <p>提供对approval_instance表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

    /**
     * 根据业务类型和业务ID查询审批实例.
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 审批实例
     */
    Optional<ApprovalInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId);
}
