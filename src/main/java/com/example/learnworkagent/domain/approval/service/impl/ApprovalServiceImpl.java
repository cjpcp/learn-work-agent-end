package com.example.learnworkagent.domain.approval.service.impl;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.repository.ApprovalInstanceRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalProcessRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalStepRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalTaskRepository;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.example.learnworkagent.infrastructure.external.notification.WebSocketNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.learnworkagent.common.enums.RoleEnum.COLLEGE_LEADER;
import static com.example.learnworkagent.common.enums.RoleEnum.COUNSELOR;
import static com.example.learnworkagent.common.enums.RoleEnum.DEPARTMENT_LEADER;

/**
 * 审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private static final String INSTANCE_PENDING = "PENDING";
    private static final String INSTANCE_APPROVED = "APPROVED";
    private static final String INSTANCE_REJECTED = "REJECTED";

    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_PROCESSING = "PROCESSING";
    private static final String TASK_APPROVED = "APPROVED";
    private static final String TASK_REJECTED = "REJECTED";

    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalTaskRepository taskRepository;
    private final ApprovalProcessRepository processRepository;
    private final ApprovalStepRepository stepRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApprovalInstance createApprovalInstance(String businessType, Long businessId, Long applicantId, String applicantInfo) {
        Optional<ApprovalProcess> processOptional = processRepository.findByProcessTypeAndEnabledTrue(businessType);
        if (processOptional.isEmpty()) {
            throw new RuntimeException("未找到启用的审批流程: " + businessType);
        }

        ApprovalProcess process = processOptional.get();
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        if (steps.isEmpty()) {
            throw new RuntimeException("审批流程未配置审批步骤 " + businessType);
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setProcess(process);
        instance.setCurrentStep(steps.get(0).getStepOrder());
        instance.setStatus(INSTANCE_PENDING);
        instance = instanceRepository.save(instance);

        for (ApprovalStep step : steps) {
            createApprovalTasks(instance, step, applicantInfo);
        }

        activateStep(instance, instance.getCurrentStep());
        updateBusinessStatus(instance);
        return instance;
    }

    @Override
    @Transactional
    public ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("审批任务不存在 " + taskId));

        if (!TASK_PROCESSING.equals(task.getStatus())) {
            throw new RuntimeException("当前任务未到审批阶段或已处理");
        }

        if (!task.getApproverId().equals(approverId)) {
            throw new RuntimeException("无权审批此任务");
        }

        if (!TASK_APPROVED.equals(status) && !TASK_REJECTED.equals(status)) {
            throw new RuntimeException("无效的审批状态 " + status);
        }

        task.setStatus(status);
        task.setComment(comment);
        task.setApprovalTime(LocalDateTime.now());
        task = taskRepository.save(task);

        ApprovalInstance instance = task.getInstance();
        handleStepProgress(instance, task.getStep().getStepOrder(), status);
        updateBusinessStatus(instance);
        return task;
    }

    @Override
    public ApprovalInstance getApprovalInstance(String businessType, Long businessId) {
        return instanceRepository.findByBusinessTypeAndBusinessId(businessType, businessId)
                .orElse(null);
    }

    @Override
    public List<ApprovalTask> getPendingTasks(Long approverId) {
        return taskRepository.findByApproverIdAndStatus(approverId, TASK_PROCESSING);
    }

    @Override
    public List<ApprovalTask> getCurrentTasks(ApprovalInstance instance) {
        if (instance == null || instance.getCurrentStep() == null) {
            return List.of();
        }
        return taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, instance.getCurrentStep());
    }

    private void createApprovalTasks(ApprovalInstance instance, ApprovalStep step, String applicantInfo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantData = objectMapper.readValue(applicantInfo, Map.class);
            List<Long> approvers = findApprovers(step, applicantData).stream().distinct().toList();
            if (approvers.isEmpty()) {
                throw new BusinessException("审批步骤未找到审批人: " + step.getStepName());
            }

            for (int i = 0; i < approvers.size(); i++) {
                ApprovalTask task = new ApprovalTask();
                task.setInstance(instance);
                task.setStep(step);
                task.setApproverId(approvers.get(i));
                task.setStatus(TASK_PENDING);
                task.setTaskOrder(i + 1);
                taskRepository.save(task);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建审批任务失败", e);
            throw new RuntimeException("创建审批任务失败", e);
        }
    }

    private List<Long> findApprovers(ApprovalStep step, Map<String, Object> applicantData) {
        List<Long> approvers = new ArrayList<>();
        Long departmentId;
        Object departmentIdObj = applicantData.get("departmentId");
        String grade = (String) applicantData.get("grade");
        switch (step.getApproverRole()) {
            case "COUNSELOR":
                if (departmentIdObj == null) {
                    throw new BusinessException("本次申请学院id为空！");
                }
                if (grade == null || grade.isEmpty()) {
                    throw new BusinessException("本次申请年级为空！");
                }
                departmentId = Long.valueOf(departmentIdObj.toString());
                List<User> counselorsByGrade = userRepository.findByDepartmentIdAndGradeAndRole(departmentId, grade, COUNSELOR.getCode());
                approvers.addAll(counselorsByGrade.stream().map(User::getId).toList());
                break;
            case "COLLEGE_LEADER":
                if (departmentIdObj == null) {
                    throw new BusinessException("本次申请学院id为空！");
                }
                departmentId = Long.valueOf(departmentIdObj.toString());
                List<User> collegeLeadersByDept = userRepository.findByDepartmentIdAndRole(departmentId, COLLEGE_LEADER.getCode());
                approvers.addAll(collegeLeadersByDept.stream().map(User::getId).toList());
                break;
            case "DEPARTMENT_LEADER":
                if (step.getApproverUserId() != null) {
                    approvers.add(step.getApproverUserId());
                    break;
                }
                departmentId = step.getDepartmentId();
                if (departmentId == null) {
                    throw new BusinessException("部门领导审批，但未设置领导的部门！");
                }
                List<User> deptLeadersByDept = userRepository.findByDepartmentIdAndRole(departmentId, DEPARTMENT_LEADER.getCode());
                approvers.addAll(deptLeadersByDept.stream().map(User::getId).toList());
                break;
            default:
                throw new BusinessException("未知的审批人角色: " + step.getApproverRole());
        }
        return approvers;
    }

    private void handleStepProgress(ApprovalInstance instance, Integer stepOrder, String taskStatus) {
        List<ApprovalTask> stepTasks = taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder);
        if (stepTasks.isEmpty()) {
            throw new RuntimeException("审批步骤任务不存在 stepOrder=" + stepOrder);
        }

        ApprovalStep step = stepTasks.get(0).getStep();
        if (TASK_REJECTED.equals(taskStatus) && !canContinueAfterReject(step)) {
            instance.setStatus(INSTANCE_REJECTED);
            instance.setCompletedTime(LocalDateTime.now());
            instanceRepository.save(instance);
            return;
        }

        if (!isStepApproved(stepTasks)) {
            instance.setStatus(INSTANCE_PENDING);
            instanceRepository.save(instance);
            return;
        }

        ApprovalStep nextStep = findNextStep(instance.getProcess(), stepOrder);
        if (nextStep == null) {
            instance.setStatus(INSTANCE_APPROVED);
            instance.setCompletedTime(LocalDateTime.now());
            instance.setCurrentStep(stepOrder);
            instanceRepository.save(instance);
            return;
        }

        instance.setStatus(INSTANCE_PENDING);
        instance.setCurrentStep(nextStep.getStepOrder());
        instanceRepository.save(instance);
        activateStep(instance, nextStep.getStepOrder());
    }

    private boolean isStepApproved(List<ApprovalTask> stepTasks) {
        ApprovalStep step = stepTasks.get(0).getStep();
        if ("MULTIPLE".equals(step.getApprovalType())) {
            if (Boolean.TRUE.equals(step.getMustPass())) {
                return stepTasks.stream().allMatch(task -> TASK_APPROVED.equals(task.getStatus()));
            }
            return stepTasks.stream().anyMatch(task -> TASK_APPROVED.equals(task.getStatus()));
        }
        return stepTasks.stream().anyMatch(task -> TASK_APPROVED.equals(task.getStatus()));
    }

    private boolean canContinueAfterReject(ApprovalStep step) {
        return "MULTIPLE".equals(step.getApprovalType()) && Boolean.FALSE.equals(step.getMustPass());
    }

    private void activateStep(ApprovalInstance instance, Integer stepOrder) {
        List<ApprovalTask> stepTasks = taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder);
        for (ApprovalTask task : stepTasks) {
            if (TASK_PENDING.equals(task.getStatus())) {
                task.setStatus(TASK_PROCESSING);
                taskRepository.save(task);
                sendApprovalNotification(task.getApproverId(), instance.getBusinessType(), instance.getBusinessId());
            }
        }
    }

    private ApprovalStep findNextStep(ApprovalProcess process, Integer currentStepOrder) {
        return stepRepository.findByProcessOrderByStepOrderAsc(process).stream()
                .filter(step -> step.getStepOrder() > currentStepOrder)
                .min(Comparator.comparing(ApprovalStep::getStepOrder))
                .orElse(null);
    }

    private void updateBusinessStatus(ApprovalInstance instance) {
        String businessType = instance.getBusinessType();
        Long businessId = instance.getBusinessId();
        String status = instance.getStatus();
        Long currentApproverId = getCurrentApproverId(instance);

        try {
            if ("LEAVE".equals(businessType)) {
                LeaveApplication leaveApplication = leaveApplicationRepository.findById(businessId).orElse(null);
                if (leaveApplication != null) {
                    leaveApplication.setApprovalStatus(status);
                    leaveApplication.setApproverId(currentApproverId);
                    if (INSTANCE_PENDING.equals(status)) {
                        leaveApplication.setApprovalComment(null);
                        leaveApplication.setApprovalTime(null);
                    } else {
                        leaveApplication.setApprovalTime(LocalDateTime.now());
                    }
                    leaveApplicationRepository.save(leaveApplication);
                }
            } else if ("AWARD".equals(businessType)) {
                AwardApplication awardApplication = awardApplicationRepository.findById(businessId).orElse(null);
                if (awardApplication != null) {
                    awardApplication.setApprovalStatus(status);
                    awardApplication.setApproverId(currentApproverId);
                    if (INSTANCE_PENDING.equals(status)) {
                        awardApplication.setApprovalComment(null);
                        awardApplication.setApprovalTime(null);
                    } else {
                        awardApplication.setApprovalTime(LocalDateTime.now());
                    }
                    awardApplicationRepository.save(awardApplication);
                }
            }
        } catch (Exception e) {
            log.error("更新业务状态失败", e);
        }
    }

    private Long getCurrentApproverId(ApprovalInstance instance) {
        return getCurrentTasks(instance).stream()
                .filter(task -> TASK_PROCESSING.equals(task.getStatus()))
                .map(ApprovalTask::getApproverId)
                .findFirst()
                .orElse(null);
    }

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
