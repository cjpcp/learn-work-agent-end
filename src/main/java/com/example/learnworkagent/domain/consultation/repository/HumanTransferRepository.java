package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * 根据状态分页查询
     */
    Page<HumanTransfer> findByStatusAndDeletedFalseOrderByCreateTimeDesc(String status, Pageable pageable);

    /**
     * 根据问题ID查询
     */
    HumanTransfer findByQuestionIdAndDeletedFalse(Long questionId);
}
