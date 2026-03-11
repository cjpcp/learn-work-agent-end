package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.common.enums.DepartmentTypeEnum;
import com.example.learnworkagent.domain.user.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部门仓储接口
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 根据编码查询部门
     */
    Optional<Department> findByCodeAndDeletedFalse(String code);

    /**
     * 查询所有启用的部门
     */
    List<Department> findByEnabledAndDeletedFalseOrderBySortOrderAsc(Boolean enabled);

    /**
     * 根据编码判断部门是否存在
     */
    boolean existsByCodeAndDeletedFalse(String code);

    /**
     * 根据类型查询所有启用的部门/学院
     */
    List<Department> findByTypeAndEnabledAndDeletedFalseOrderBySortOrderAsc(DepartmentTypeEnum type, Boolean enabled);
}
