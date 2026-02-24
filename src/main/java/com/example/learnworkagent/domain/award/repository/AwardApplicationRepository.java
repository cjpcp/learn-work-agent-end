package com.example.learnworkagent.domain.award.repository;

import com.example.learnworkagent.domain.award.entity.AwardApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 奖助申请仓库
 */
@Repository
public interface AwardApplicationRepository extends JpaRepository<AwardApplication, Long> {

    /**
     * 根据申请人ID分页查询
     */
    Page<AwardApplication> findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, Pageable pageable);

    /**
     * 根据审批人ID分页查询
     */
    Page<AwardApplication> findByApproverIdAndDeletedFalseOrderByCreateTimeDesc(Long approverId, Pageable pageable);

    /**
     * 根据审批状态查询
     */
    Page<AwardApplication> findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(String approvalStatus, Pageable pageable);

    /**
     * 根据申请类型查询
     */
    Page<AwardApplication> findByApplicationTypeAndDeletedFalseOrderByCreateTimeDesc(String applicationType, Pageable pageable);

    /**
     * 根据材料预审状态查询
     */
    Page<AwardApplication> findByMaterialStatusAndDeletedFalseOrderByCreateTimeDesc(String materialStatus, Pageable pageable);

    /**
     * 根据审批状态和审批人ID查询
     */
    Page<AwardApplication> findByApprovalStatusAndApproverIdAndDeletedFalseOrderByCreateTimeDesc(String approvalStatus, Long approverId, Pageable pageable);
}
