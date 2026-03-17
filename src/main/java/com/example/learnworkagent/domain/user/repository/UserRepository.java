package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据学号查询
     */
    Optional<User> findByStudentNoAndDeletedFalse(String studentNo);

    /**
     * 根据工作部门ID和角色查询
     */
    List<User> findByDepartmentIdAndRole(Long departmentId, String role);

    /**
     * 根据工作部门ID、年级和角色查询
     */


    List<User> findByDepartmentIdAndGradeAndRole(Long departmentId, String grade, String role);
}
