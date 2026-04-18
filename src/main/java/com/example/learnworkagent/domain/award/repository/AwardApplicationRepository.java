package com.example.learnworkagent.domain.award.repository;

import com.example.learnworkagent.domain.award.entity.AwardApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 奖助申请仓储层.
 * <p>提供对award_application表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface AwardApplicationRepository extends JpaRepository<AwardApplication, Long>, JpaSpecificationExecutor<AwardApplication> {

    /**
     * 根据申请人ID分页查询奖助申请.
     *
     * @param applicantId 申请人ID
     * @param pageable    分页参数
     * @return 奖助申请分页列表
     */
    Page<AwardApplication> findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, Pageable pageable);

    /**
     * 根据申请人ID和审批状态查询.
     *
     * @param applicantId   申请人ID
     * @param approvalStatus 审批状态
     * @param pageable      分页参数
     * @return 奖助申请分页列表
     */
    Page<AwardApplication> findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, String approvalStatus, Pageable pageable);

}