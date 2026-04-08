package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.domain.user.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Admin> findByRoleId(Long roleId);
}
