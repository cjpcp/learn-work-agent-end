package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人工转接仓库
 */
@Repository
public interface HumanTransferRepository extends JpaRepository<HumanTransfer, Long> {

    /**
     * 根据用户ID分页查询
     */
    Page<HumanTransfer> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据工作人员ID分页查询
     */
    Page<HumanTransfer> findByStaffIdAndDeletedFalseOrderByCreateTimeDesc(Long staffId, Pageable pageable);

    /**
     * 根据工作人员ID和状态列表分页查询
     */
    Page<HumanTransfer> findByStaffIdAndStatusInAndDeletedFalseOrderByCreateTimeDesc(Long staffId, java.util.List<String> statusList, Pageable pageable);

    /**
     * 根据状态分页查询
     */
    Page<HumanTransfer> findByStatusInAndDeletedFalseOrderByCreateTimeDesc(List<String> status, Pageable pageable);

    /**
     * 根据问题ID查询
     */
    HumanTransfer findByQuestionIdAndDeletedFalse(Long questionId);
    
    /**
     * 根据工作人员ID和状态查询已完成的记录
     */
    Page<HumanTransfer> findByStaffIdAndStatusAndDeletedFalseOrderByCreateTimeDesc(Long staffId, String status, Pageable pageable);
}
