package com.example.learnworkagent.domain.approval.service.impl;

import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.repository.*;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.user.entity.Department;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.example.learnworkagent.domain.user.service.DepartmentService;
import com.example.learnworkagent.infrastructure.external.notification.WebSocketNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalTaskRepository taskRepository;
    private final ApprovalProcessRepository processRepository;
    private final ApprovalStepRepository stepRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApprovalInstance createApprovalInstance(String businessType, Long businessId, Long applicantId, String applicantInfo) {
        // 查找对应业务类型的启用流程
        Optional<ApprovalProcess> processOptional = processRepository.findByProcessTypeAndEnabledTrue(businessType);
        if (processOptional.isEmpty()) {
            throw new RuntimeException("未找到启用的审批流程: " + businessType);
        }

        ApprovalProcess process = processOptional.get();
        log.info("找到启用的审批流程: {}，流程ID: {}", businessType, process.getId());

        // 创建审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setProcess(process);
        instance.setCurrentStep(1); // 从第一步开始
        instance.setStatus("PENDING");
        instance = instanceRepository.save(instance);
        log.info("创建审批实例成功: 实例ID: {}", instance.getId());

        // 为每个步骤创建审批任务
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        log.info("审批流程包含 {} 个步骤", steps.size());
        for (ApprovalStep step : steps) {
            log.info("处理步骤: {}，步骤顺序: {}", step.getStepName(), step.getStepOrder());
            createApprovalTasks(instance, step, applicantId, applicantInfo);
        }

        return instance;
    }

    @Override
    @Transactional
    public ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment) {

        // 获取审批任务
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("审批任务不存在: " + taskId));

        // 验证任务状态
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务已处理，无法重复审批");
        }

        // 验证审批人
        if (!task.getApproverId().equals(approverId)) {
            throw new RuntimeException("无权审批此任务");
        }

        // 更新任务状态
        task.setStatus(status);
        task.setComment(comment);
        task.setApprovalTime(LocalDateTime.now());
        task = taskRepository.save(task);

        // 更新审批实例状态
        updateInstanceStatus(task.getInstance());
        ApprovalInstance instance = task.getInstance();
        sendApprovalNotification(approverId, instance.getBusinessType(), instance.getBusinessId());
        return task;
    }

    @Override
    public ApprovalInstance getApprovalInstance(String businessType, Long businessId) {
        return instanceRepository.findByBusinessTypeAndBusinessId(businessType, businessId)
                .orElse(null);
    }

    @Override
    public List<ApprovalTask> getPendingTasks(Long approverId) {
        return taskRepository.findByApproverIdAndStatus(approverId, "PENDING");
    }

    /**
     * 创建审批任务
     */
    private void createApprovalTasks(ApprovalInstance instance, ApprovalStep step, Long applicantId, String applicantInfo) {
        try {
            Map<String, Object> applicantData = objectMapper.readValue(applicantInfo, Map.class);
            log.info("解析申请人信息成功: {}", applicantData);
            
            List<Long> approvers = findApprovers(step, applicantData);
            log.info("找到 {} 个审批人", approvers.size());

            for (int i = 0; i < approvers.size(); i++) {
                ApprovalTask task = new ApprovalTask();
                task.setInstance(instance);
                task.setStep(step);
                task.setApproverId(approvers.get(i));
                task.setStatus("PENDING");
                task.setTaskOrder(i + 1);
                task = taskRepository.save(task);
                log.info("创建审批任务成功: 任务ID: {}，审批人ID: {}", task.getId(), approvers.get(i));

                // 发送通知
                sendApprovalNotification(approvers.get(i), instance.getBusinessType(), instance.getBusinessId());
            }
        } catch (Exception e) {
            log.error("创建审批任务失败", e);
            throw new RuntimeException("创建审批任务失败", e);
        }
    }

    /**
     * 查找审批人
     */
    private List<Long> findApprovers(ApprovalStep step, Map<String, Object> applicantData) {

        List<Long> approvers = new ArrayList<>();
        log.info("查找审批人，审批人角色: {}", step.getApproverRole());
        // 优先使用departmentId查询部门
        Object departmentIdObj = applicantData.get("departmentId");
        String department = (String) applicantData.get("department");
        String grade = (String) applicantData.get("grade");
        log.info("按院系和年级分配，院系: {}, 院系ID: {}, 年级: {}", department, departmentIdObj, grade);
        
        Department dept = null;
        if (departmentIdObj != null) {
            try {
                Long departmentId = Long.valueOf(departmentIdObj.toString());
                dept = departmentService.getDepartmentById(departmentId);
                log.info("根据ID查询到部门: {}", dept.getName());
            } catch (Exception e) {
                log.error("根据ID查询部门失败: {}", e.getMessage());
            }
        }
        
        if (dept == null && department != null) {
            RoleEnum roleEnum = RoleEnum.getByCode(step.getApproverRole());
            try {
                // 先尝试根据代码查询部门
                dept = departmentService.getDepartmentByCode(department);
                log.info("根据代码查询到部门: {}", dept.getName());
            } catch (Exception e) {
                try {
                    // 如果根据代码查询失败，尝试根据名称查询部门
                    dept = departmentService.getDepartmentByName(department);
                    log.info("根据名称查询到部门: {}", dept.getName());
                } catch (Exception ex) {
                    log.error("查询部门失败: {}", ex.getMessage());
                }
            }
        }
        
        if (dept != null) {
            RoleEnum roleEnum = RoleEnum.getByCode(step.getApproverRole());
            
            // 首先尝试根据部门ID和年级查询审批人
            if (grade != null && !grade.isEmpty()) {
                List<User> counselorsByGrade = userRepository.findByWorkDepartmentIdAndGradeAndRole(dept.getId(), grade, roleEnum);
                log.info("找到 {} 个院系+年级审批人", counselorsByGrade.size());
                for (User user : counselorsByGrade) {
                    approvers.add(user.getId());
                    log.info("添加审批人: {} (ID: {})", user.getRealName(), user.getId());
                }
            }
            
            // 如果根据年级找不到审批人，再尝试根据部门ID查询
            if (approvers.isEmpty()) {
                List<User> counselors = userRepository.findByWorkDepartmentIdAndRole(dept.getId(), roleEnum);
                log.info("找到 {} 个院系审批人", counselors.size());
                for (User user : counselors) {
                    approvers.add(user.getId());
                    log.info("添加审批人: {} (ID: {})", user.getRealName(), user.getId());
                }
            }
        } else {
            log.warn("院系信息为空，无法分配审批人");
        }

        log.info("最终找到 {} 个审批人", approvers.size());
        
        // 如果找不到审批人，向前端报错
        if (approvers.isEmpty()) {
            throw new RuntimeException("未找到合适的审批人，请联系管理员");
        }
        
        return approvers;
    }

    /**
     * 更新审批实例状态
     */
    private void updateInstanceStatus(ApprovalInstance instance) {
        List<ApprovalTask> tasks = taskRepository.findByInstanceOrderByTaskOrderAsc(instance);
        boolean allApproved = true;
        boolean anyRejected = false;

        for (ApprovalTask task : tasks) {
            if ("REJECTED".equals(task.getStatus())) {
                anyRejected = true;
                break;
            }
            if ("PENDING".equals(task.getStatus())) {
                allApproved = false;
            }
        }

        if (anyRejected) {
            instance.setStatus("REJECTED");
            instance.setCompletedTime(LocalDateTime.now());
        } else if (allApproved) {
            instance.setStatus("APPROVED");
            instance.setCompletedTime(LocalDateTime.now());
        }

        instanceRepository.save(instance);

        // 更新业务状态
        updateBusinessStatus(instance);
    }

    /**
     * 更新业务状态
     */
    private void updateBusinessStatus(ApprovalInstance instance) {
        String businessType = instance.getBusinessType();
        Long businessId = instance.getBusinessId();
        String status = instance.getStatus();

        try {
            if ("LEAVE".equals(businessType)) {
                LeaveApplication leaveApplication = leaveApplicationRepository.findById(businessId).orElse(null);
                if (leaveApplication != null) {
                    leaveApplication.setApprovalStatus(status);
                    leaveApplicationRepository.save(leaveApplication);
                }
            } else if ("AWARD".equals(businessType)) {
                AwardApplication awardApplication = awardApplicationRepository.findById(businessId).orElse(null);
                if (awardApplication != null) {
                    awardApplication.setApprovalStatus(status);
                    awardApplicationRepository.save(awardApplication);
                }
            }
        } catch (Exception e) {
            log.error("更新业务状态失败", e);
        }
    }

    /**
     * 发送审批通知
     */
    private void sendApprovalNotification(Long approverId, String businessType, Long businessId) {
        try {
            User approver = userRepository.findById(approverId).orElse(null);
            if (approver != null) {
                String message = "您有新的审批任务: " + 
                        ("LEAVE".equals(businessType) ? "请假申请" : "奖助申请") + 
                        " #" + businessId;
                Notification notification = Notification.builder()
                        .userId(approverId)
                        .title("审批任务通知")
                        .content(message)
                        .type("APPROVAL_TASK")
                        .isRead(false)
                        .build();
                notificationService.sendNotificationToUser(approverId, notification);
            }
        } catch (Exception e) {
            log.error("发送审批通知失败", e);
        }
    }
}
