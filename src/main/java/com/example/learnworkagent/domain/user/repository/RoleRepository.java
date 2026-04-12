package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色仓储层.
 * <p>提供对role表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);
}
