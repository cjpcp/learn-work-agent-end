package com.example.learnworkagent.domain.user.service.impl;

import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.example.learnworkagent.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public List<User> findByDepartmentIdAndRole(Long workDepartmentId, String role) {
        return userRepository.findByDepartmentIdAndRole(workDepartmentId, role);
    }
}
