package com.example.learnworkagent.domain.notification.repository;

import com.example.learnworkagent.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知仓储层.
 * <p>提供对notification表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查询用户的通知列表.
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 通知分页列表
     */
    Page<Notification> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的未读通知列表.
     *
     * @param userId 用户ID
     * @return 未读通知列表
     */
    List<Notification> findByUserIdAndIsReadFalseAndDeletedFalse(Long userId);

    /**
     * 统计用户的未读通知数量.
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long countByUserIdAndIsReadFalseAndDeletedFalse(Long userId);
}