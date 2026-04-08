package com.example.learnworkagent.domain.user.service.impl;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AdminRepository adminRepository;

    @Override
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Override
    public List<Admin> findByRoleId(Long roleId) {
        return adminRepository.findByRoleId(roleId);
    }

    @Override
    public List<Admin> findUsers(Long roleId, String username, String nick, Integer status) {
        return adminRepository.findAll(buildSpecification(roleId, username, nick, status));
    }

    @Override
    public PageResult<Admin> findUsersPage(Long roleId, String username, String nick, Integer status, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.ASC, "id")
        );
        Page<Admin> page = adminRepository.findAll(buildSpecification(roleId, username, nick, status), pageable);
        return new PageResult<>(page.getContent(), page.getTotalElements(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    private Specification<Admin> buildSpecification(Long roleId, String username, String nick, Integer status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (roleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("roleId"), roleId));
            }
            if (hasText(username)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            }
            if (hasText(nick)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nick")), "%" + nick.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
