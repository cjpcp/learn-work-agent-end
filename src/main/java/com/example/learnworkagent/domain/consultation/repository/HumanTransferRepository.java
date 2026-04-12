package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人工转接记录仓储层.
 * <p>提供对human_transfer表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface HumanTransferRepository extends JpaRepository<HumanTransfer, Long> {

    /**
     * 根据发起人ID分页查询转接记录.
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 转接记录分页列表
     */
    Page<HumanTransfer> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据工作人员ID和状态列表分页查询.
     *
     * @param staffId   工作人员ID
     * @param statuses  状态列表
     * @param pageable  分页参数
     * @return 转接记录分页列表
     */
    Page<HumanTransfer> findByStaffIdAndStatusInAndDeletedFalseOrderByCreateTimeDesc(Long staffId, List<String> statuses, Pageable pageable);

    /**
     * 根据工作人员ID和状态查询已完成的记录.
     *
     * @param staffId   工作人员ID
     * @param status    状态
     * @param pageable  分页参数
     * @return 转接记录分页列表
     */
    Page<HumanTransfer> findByStaffIdAndStatusAndDeletedFalseOrderByCreateTimeDesc(Long staffId, String status, Pageable pageable);
}