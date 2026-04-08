package com.example.learnworkagent.domain.leave.repository;

import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 请假申请仓库。
 */
@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long>, JpaSpecificationExecutor<LeaveApplication> {

    /**
     * 根据申请人ID分页查询。
     *
     * @param applicantId 申请人ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<LeaveApplication> findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, Pageable pageable);

    /**
     * 查询指定日期范围内有交集的请假申请。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 重叠的请假申请列表
     */
    @Query("SELECT la FROM LeaveApplication la WHERE la.startDate <= :endDate AND la.endDate >= :startDate AND la.deleted = false")
    List<LeaveApplication> findOverlappingLeaves(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 根据申请人ID和审批状态分页查询。
     *
     * @param applicantId 申请人ID
     * @param approvalStatus 审批状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<LeaveApplication> findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(Long applicantId,
                                                                                                   String approvalStatus,
                                                                                                   Pageable pageable);

    /**
     * 查询待审批销假申请（由指定审批人审批）。
     *
     * @param approverId 审批人ID（原请假审批人）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT la FROM LeaveApplication la WHERE la.approverId = :approverId AND la.cancelRequested = true AND la.cancelApprovalStatus = 'PENDING' AND la.deleted = false")
    Page<LeaveApplication> findPendingCancelRequestsByApproverId(@Param("approverId") Long approverId, Pageable pageable);

}
