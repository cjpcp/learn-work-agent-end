package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.consultation.dto.HumanTransferConfigRequest;
import com.example.learnworkagent.domain.consultation.dto.HumanTransferConfigResponse;
import com.example.learnworkagent.domain.consultation.entity.HumanTransferConfig;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferConfigRepository;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Role;
import com.example.learnworkagent.domain.user.entity.Teacher;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.repository.RoleRepository;
import com.example.learnworkagent.domain.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HumanTransferConfigService {

    public static final String ASSIGN_MODE_USER = "USER";
    public static final String ASSIGN_MODE_ROLE = "ROLE";
    public static final String BUSINESS_TYPE_DEFAULT = "DEFAULT";

    private final HumanTransferConfigRepository configRepository;
    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    public List<HumanTransferConfigResponse> findAll() {
        return configRepository.findByDeletedFalseOrderByPriorityAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HumanTransferConfigResponse create(HumanTransferConfigRequest request) {
        HumanTransferConfig config = buildConfig(new HumanTransferConfig(), request);
        return toResponse(configRepository.save(config));
    }

    @Transactional
    public HumanTransferConfigResponse update(Long id, HumanTransferConfigRequest request) {
        HumanTransferConfig config = configRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "人工转接配置不存在"));
        return toResponse(configRepository.save(buildConfig(config, request)));
    }

    @Transactional
    public void delete(Long id) {
        HumanTransferConfig config = configRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "人工转接配置不存在"));
        config.setDeleted(true);
        configRepository.save(config);
    }

    public Long resolveStaffId(String businessType) {
        List<HumanTransferConfig> configs = configRepository
                .findByBusinessTypeAndEnabledTrueAndDeletedFalseOrderByPriorityAscIdAsc(normalizeBusinessType(businessType));
        Long staffId = resolveFromConfigs(configs);
        if (staffId != null) {
            return staffId;
        }
        if (!BUSINESS_TYPE_DEFAULT.equalsIgnoreCase(normalizeBusinessType(businessType))) {
            return resolveFromConfigs(configRepository.findByBusinessTypeAndEnabledTrueAndDeletedFalseOrderByPriorityAscIdAsc(BUSINESS_TYPE_DEFAULT));
        }
        return null;
    }

    private Long resolveFromConfigs(List<HumanTransferConfig> configs) {
        for (HumanTransferConfig config : configs) {
            if (ASSIGN_MODE_USER.equalsIgnoreCase(config.getAssignMode())) {
                List<Long> userIds = parseUserIds(config.getUserIds());
                if (!userIds.isEmpty()) {
                    int index = Math.abs(roundRobinCounter.getAndIncrement()) % userIds.size();
                    for (int i = 0; i < userIds.size(); i++) {
                        Long userId = userIds.get((index + i) % userIds.size());
                        if (isAvailableUser(userId)) {
                            return userId;
                        }
                    }
                }
                continue;
            }
            if (ASSIGN_MODE_ROLE.equalsIgnoreCase(config.getAssignMode()) && config.getRoleId() != null) {
                List<Admin> candidates = adminRepository.findByRoleId(config.getRoleId()).stream()
                        .filter(this::isAvailableAdmin)
                        .sorted(Comparator.comparing(Admin::getId))
                        .toList();
                if (!candidates.isEmpty()) {
                    int index = Math.abs(roundRobinCounter.getAndIncrement()) % candidates.size();
                    for (int i = 0; i < candidates.size(); i++) {
                        Admin admin = candidates.get((index + i) % candidates.size());
                        if (admin != null) {
                            return admin.getId();
                        }
                    }
                }
            }
        }
        return null;
    }

    private HumanTransferConfig buildConfig(HumanTransferConfig config, HumanTransferConfigRequest request) {
        String assignMode = normalizeAssignMode(request.getAssignMode());
        validateRequest(request, assignMode);
        config.setBusinessType(normalizeBusinessType(request.getBusinessType()));
        config.setAssignMode(assignMode);
        config.setRoleId(ASSIGN_MODE_ROLE.equals(assignMode) ? request.getRoleId() : null);
        config.setUserIds(ASSIGN_MODE_USER.equals(assignMode) ? joinUserIds(request.getUserIds()) : null);
        config.setPriority(request.getPriority());
        config.setEnabled(request.getEnabled());
        config.setRemark(request.getRemark());
        return config;
    }

    private void validateRequest(HumanTransferConfigRequest request, String assignMode) {
        if (request.getPriority() == null || request.getPriority() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优先级必须大于0");
        }
        if (ASSIGN_MODE_ROLE.equals(assignMode)) {
            if (request.getRoleId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "按角色分配时必须选择角色");
            }
            roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "角色不存在"));
            return;
        }
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "按用户分配时必须至少选择一个用户");
        }
        for (Long userId : userIds) {
            Admin admin = adminRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "用户不存在"));
            if (!isAvailableAdmin(admin)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "所选用户不可用于人工转接");
            }
        }
    }

    private HumanTransferConfigResponse toResponse(HumanTransferConfig config) {
        Role role = config.getRoleId() != null ? roleRepository.findById(config.getRoleId()).orElse(null) : null;
        List<Long> userIds = parseUserIds(config.getUserIds());
        List<String> userNames = userIds.stream()
                .map(id -> adminRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::buildUserDisplayName)
                .toList();
        return HumanTransferConfigResponse.builder()
                .id(config.getId())
                .businessType(config.getBusinessType())
                .assignMode(config.getAssignMode())
                .roleId(config.getRoleId())
                .roleName(role != null ? role.getRoleName() : "")
                .userIds(userIds)
                .userNames(userNames)
                .priority(config.getPriority())
                .enabled(config.getEnabled())
                .remark(config.getRemark())
                .build();
    }

    private String buildUserDisplayName(Admin admin) {
        Teacher teacher = admin.getTeacherId() != null && admin.getTeacherId() > 0
                ? teacherRepository.findById(admin.getTeacherId()).orElse(null)
                : null;
        return teacher != null && teacher.getName() != null && !teacher.getName().isBlank()
                ? teacher.getName() + "（" + admin.getUsername() + "）"
                : admin.getNick() + "（" + admin.getUsername() + "）";
    }

    private boolean isAvailableUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return adminRepository.findById(userId).map(this::isAvailableAdmin).orElse(false);
    }

    private boolean isAvailableAdmin(Admin admin) {
        if (admin == null || !admin.isEnabled()) {
            return false;
        }
        Long teacherId = admin.getTeacherId();
        if (teacherId == null || teacherId <= 0) {
            return true;
        }
        return teacherRepository.findById(teacherId).map(Teacher::isEnabled).orElse(false);
    }

    private List<Long> parseUserIds(String userIds) {
        if (userIds == null || userIds.trim().isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String item : userIds.split(",")) {
            if (!item.trim().isEmpty()) {
                result.add(Long.parseLong(item.trim()));
            }
        }
        return result;
    }

    private String joinUserIds(List<Long> userIds) {
        return userIds == null ? null : userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String normalizeBusinessType(String businessType) {
        return businessType == null ? BUSINESS_TYPE_DEFAULT : businessType.trim().toUpperCase();
    }

    private String normalizeAssignMode(String assignMode) {
        String value = assignMode == null ? "" : assignMode.trim().toUpperCase();
        if (!ASSIGN_MODE_USER.equals(value) && !ASSIGN_MODE_ROLE.equals(value)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分配模式仅支持 USER 或 ROLE");
        }
        return value;
    }
}
