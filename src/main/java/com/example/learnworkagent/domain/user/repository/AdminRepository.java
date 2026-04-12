package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.domain.user.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 管理员账户仓储层.
 * <p>提供对admin表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Admin> findByRoleId(Long roleId);
}
