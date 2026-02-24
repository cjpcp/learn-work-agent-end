package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    /**
     * 根据学号查询
     */
    Optional<User> findByStudentNoAndDeletedFalse(String studentNo);

    /**
     * 判断用户名是否存在
     */
    boolean existsByUsernameAndDeletedFalse(String username);
}
