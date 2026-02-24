package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 咨询问题仓库
 */
@Repository
public interface ConsultationQuestionRepository extends JpaRepository<ConsultationQuestion, Long> {

    /**
     * 根据用户ID分页查询
     */
    Page<ConsultationQuestion> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据状态查询
     */
    Page<ConsultationQuestion> findByStatusAndDeletedFalseOrderByCreateTimeDesc(String status, Pageable pageable);

    /**
     * 根据分类查询
     */
    Page<ConsultationQuestion> findByCategoryAndDeletedFalseOrderByCreateTimeDesc(String category, Pageable pageable);

    /**
     * 查询需要转人工的问题
     */
    @Query("SELECT cq FROM ConsultationQuestion cq WHERE cq.transferredToHuman = true AND cq.status = 'TRANSFERRED' AND cq.deleted = false ORDER BY cq.createTime DESC")
    Page<ConsultationQuestion> findTransferredQuestions(Pageable pageable);

    /**
     * 统计某时间段的咨询数量
     */
    @Query("SELECT COUNT(cq) FROM ConsultationQuestion cq WHERE cq.createTime BETWEEN :startTime AND :endTime AND cq.deleted = false")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
