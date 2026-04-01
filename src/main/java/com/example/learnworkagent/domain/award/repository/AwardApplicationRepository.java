package com.example.learnworkagent.domain.award.repository;

import com.example.learnworkagent.domain.award.entity.AwardApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 奖助申请仓库
 */
@Repository
public interface AwardApplicationRepository extends JpaRepository<AwardApplication, Long>, JpaSpecificationExecutor<AwardApplication> {

    /**
     * 根据申请人ID分页查询
     */
    Page<AwardApplication> findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, Pageable pageable);

    /**
     * 根据申请人ID和审批状态查询
     */
    Page<AwardApplication> findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, String approvalStatus, Pageable pageable);

}
