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

    /**
     * 根据工作部门和角色查询
     */
    List<User> findByWorkDepartmentAndRole(String workDepartment, RoleEnum role);

    /**
     * 根据工作部门ID和角色查询
     */
    List<User> findByWorkDepartmentIdAndRole(Long workDepartmentId, RoleEnum role);

    /**
     * 根据工作部门、职位和角色查询
     */
    List<User> findByWorkDepartmentAndPositionAndRole(String workDepartment, String position, RoleEnum role);

    /**
     * 根据工作部门ID、年级和角色查询
     */
    List<User> findByWorkDepartmentIdAndGradeAndRole(Long workDepartmentId, String grade, RoleEnum role);
}
