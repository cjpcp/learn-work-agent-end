package com.example.learnworkagent.domain.notification.repository;

import com.example.learnworkagent.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知仓库
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查询用户的通知列表
     */
    Page<Notification> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的未读通知
     */
    List<Notification> findByUserIdAndIsReadFalseAndDeletedFalseOrderByCreateTimeDesc(Long userId);

    /**
     * 统计用户未读通知数量
     */
    long countByUserIdAndIsReadFalseAndDeletedFalse(Long userId);
}
