package com.example.learnworkagent.domain.leave.repository;

import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 请假申请仓库
 */
@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    /**
     * 根据申请人ID分页查询
     */
    Page<LeaveApplication> findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(Long applicantId, Pageable pageable);

    /**
     * 根据审批人ID分页查询
     */
    Page<LeaveApplication> findByApproverIdAndDeletedFalseOrderByCreateTimeDesc(Long approverId, Pageable pageable);

    /**
     * 根据审批状态查询
     */
    Page<LeaveApplication> findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(String approvalStatus, Pageable pageable);

    /**
     * 根据请假类型查询
     */
    Page<LeaveApplication> findByLeaveTypeAndDeletedFalseOrderByCreateTimeDesc(String leaveType, Pageable pageable);

    /**
     * 查询指定日期范围内的请假申请
     */
    @Query("SELECT la FROM LeaveApplication la WHERE la.startDate <= :endDate AND la.endDate >= :startDate AND la.deleted = false")
    List<LeaveApplication> findOverlappingLeaves(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<LeaveApplication> findByApprovalStatusAndApproverIdAndDeletedFalseOrderByCreateTimeDesc(String pending, Long approverId, Pageable pageable);
}
