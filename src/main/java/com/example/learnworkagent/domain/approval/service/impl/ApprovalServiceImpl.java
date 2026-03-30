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
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.example.learnworkagent.common.enums.RoleEnum.COLLEGE_LEADER;
import static com.example.learnworkagent.common.enums.RoleEnum.COUNSELOR;
import static com.example.learnworkagent.common.enums.RoleEnum.DEPARTMENT_LEADER;

/**
 * 审批服务实现
 * 通知策略：
 *   1. 提交申请/流转到新节点 -> 通知对应审批者（站内信+邮件）
 *   2. 审批结束（最终通过或拒绝）-> 通知申请者（站内信+邮件）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private static final String INSTANCE_PENDING  = "PENDING";
    private static final String INSTANCE_APPROVED = "APPROVED";
    private static final String INSTANCE_REJECTED = "REJECTED";
    private static final String TASK_PENDING      = "PENDING";
    private static final String TASK_PROCESSING   = "PROCESSING";
    private static final String TASK_APPROVED     = "APPROVED";
    private static final String TASK_REJECTED     = "REJECTED";

    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalTaskRepository     taskRepository;
    private final ApprovalProcessRepository  processRepository;
    private final ApprovalStepRepository     stepRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final UserRepository             userRepository;
    private final NotificationService        notificationService;
    private final ObjectMapper               objectMapper;

    // =======================================================
    // 公开接口
    // =======================================================

    @Override
    public ApprovalInstance createApprovalInstance(String businessType, Long businessId,
                                                   Long applicantId, String applicantInfo) {
        ApprovalProcess process = processRepository.findByProcessTypeAndEnabledTrue(businessType)
                .orElseThrow(() -> new RuntimeException("未找到启用的审批流程: " + businessType));
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        if (steps.isEmpty()) throw new RuntimeException("审批流程未配置审批步骤: " + businessType);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setApplicantId(applicantId);
        instance.setProcess(process);
        instance.setCurrentStep(steps.get(0).getStepOrder());
        instance.setStatus(INSTANCE_PENDING);
        instance = instanceRepository.save(instance);

        for (ApprovalStep step : steps) createApprovalTasks(instance, step, applicantInfo);
        activateStep(instance, instance.getCurrentStep());
        updateBusinessStatus(instance);
        return instance;
    }

    @Override
    public ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("审批任务不存在: " + taskId));
        if (!TASK_PROCESSING.equals(task.getStatus())) throw new RuntimeException("当前任务未到审批阶段或已处理");
        if (!task.getApproverId().equals(approverId)) throw new RuntimeException("无权审批此任务");
        if (!TASK_APPROVED.equals(status) && !TASK_REJECTED.equals(status))
            throw new RuntimeException("无效的审批状态: " + status);

        task.setStatus(status);
        task.setComment(comment);
        task.setApprovalTime(LocalDateTime.now());
        task = taskRepository.save(task);

        ApprovalInstance instance = task.getInstance();
        handleStepProgress(instance, task.getStep().getStepOrder(), status, approverId, comment);
        updateBusinessStatus(instance);
        return task;
    }

    @Override
    public ApprovalInstance getApprovalInstance(String businessType, Long businessId) {
        return instanceRepository.findByBusinessTypeAndBusinessId(businessType, businessId).orElse(null);
    }

    @Override
    public List<ApprovalTask> getPendingTasks(Long approverId) {
        return taskRepository.findByApproverIdAndStatus(approverId, TASK_PROCESSING);
    }

    @Override
    public List<ApprovalTask> getCurrentTasks(ApprovalInstance instance) {
        if (instance == null || instance.getCurrentStep() == null) return List.of();
        return taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, instance.getCurrentStep());
    }

    // =======================================================
    // 审批流程内部逻辑
    // =======================================================

    private void createApprovalTasks(ApprovalInstance instance, ApprovalStep step, String applicantInfo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(applicantInfo, Map.class);
            List<Long> approvers = findApprovers(step, data).stream().distinct().toList();
            if (approvers.isEmpty()) throw new BusinessException("审批步骤未找到审批人: " + step.getStepName());
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

    private List<Long> findApprovers(ApprovalStep step, Map<String, Object> data) {
        List<Long> approvers = new ArrayList<>();
        Object deptIdObj = data.get("departmentId");
        String grade = (String) data.get("grade");
        Long departmentId;
        switch (step.getApproverRole()) {
            case "COUNSELOR" -> {
                if (deptIdObj == null) throw new BusinessException("本次申请学院id为空！");
                if (grade == null || grade.isEmpty()) throw new BusinessException("本次申请年级为空！");
                departmentId = Long.valueOf(deptIdObj.toString());
                userRepository.findByDepartmentIdAndGradeAndRole(departmentId, grade, COUNSELOR.getCode())
                        .forEach(u -> approvers.add(u.getId()));
            }
            case "COLLEGE_LEADER" -> {
                if (deptIdObj == null) throw new BusinessException("本次申请学院id为空！");
                departmentId = Long.valueOf(deptIdObj.toString());
                userRepository.findByDepartmentIdAndRole(departmentId, COLLEGE_LEADER.getCode())
                        .forEach(u -> approvers.add(u.getId()));
            }
            case "DEPARTMENT_LEADER" -> {
                if (step.getApproverUserId() != null) {
                    approvers.add(step.getApproverUserId());
                } else {
                    departmentId = step.getDepartmentId();
                    if (departmentId == null) throw new BusinessException("部门领导审批，但未设置领导的部门！");
                    userRepository.findByDepartmentIdAndRole(departmentId, DEPARTMENT_LEADER.getCode())
                            .forEach(u -> approvers.add(u.getId()));
                }
            }
            default -> throw new BusinessException("未知的审批人角色: " + step.getApproverRole());
        }
        return approvers;
    }

    private void handleStepProgress(ApprovalInstance instance, Integer stepOrder,
                                    String taskStatus, Long approverId, String comment) {
        List<ApprovalTask> stepTasks = taskRepository
                .findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder);
        if (stepTasks.isEmpty()) throw new RuntimeException("审批步骤任务不存在 stepOrder=" + stepOrder);
        ApprovalStep step = stepTasks.get(0).getStep();

        if (TASK_REJECTED.equals(taskStatus) && !canContinueAfterReject(step)) {
            instance.setStatus(INSTANCE_REJECTED);
            instance.setCompletedTime(LocalDateTime.now());
            instanceRepository.save(instance);
            notifyApplicant(instance, INSTANCE_REJECTED, approverId, comment);
            return;
        }

        if (!isStepApproved(stepTasks)) {
            instance.setStatus(INSTANCE_PENDING);
            instanceRepository.save(instance);
            return;
        }

        ApprovalStep nextStep = findNextStep(instance.getProcess(), stepOrder);
        if (nextStep != null) {
            instance.setStatus(INSTANCE_PENDING);
            instance.setCurrentStep(nextStep.getStepOrder());
            instanceRepository.save(instance);
            activateStep(instance, nextStep.getStepOrder());
        } else {
            instance.setStatus(INSTANCE_APPROVED);
            instance.setCompletedTime(LocalDateTime.now());
            instance.setCurrentStep(stepOrder);
            instanceRepository.save(instance);
            notifyApplicant(instance, INSTANCE_APPROVED, approverId, comment);
        }
    }

    private boolean isStepApproved(List<ApprovalTask> stepTasks) {
        ApprovalStep step = stepTasks.get(0).getStep();
        if ("MULTIPLE".equals(step.getApprovalType()) && Boolean.TRUE.equals(step.getMustPass())) {
            return stepTasks.stream().allMatch(t -> TASK_APPROVED.equals(t.getStatus()));
        }
        return stepTasks.stream().anyMatch(t -> TASK_APPROVED.equals(t.getStatus()));
    }

    private boolean canContinueAfterReject(ApprovalStep step) {
        return "MULTIPLE".equals(step.getApprovalType()) && Boolean.FALSE.equals(step.getMustPass());
    }

    private void activateStep(ApprovalInstance instance, Integer stepOrder) {
        taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder)
                .forEach(task -> {
                    if (TASK_PENDING.equals(task.getStatus())) {
                        task.setStatus(TASK_PROCESSING);
                        taskRepository.save(task);
                        notifyApprover(task, instance);
                    }
                });
    }

    private ApprovalStep findNextStep(ApprovalProcess process, Integer currentStepOrder) {
        return stepRepository.findByProcessOrderByStepOrderAsc(process).stream()
                .filter(s -> s.getStepOrder() > currentStepOrder)
                .min(Comparator.comparing(ApprovalStep::getStepOrder))
                .orElse(null);
    }

    private void updateBusinessStatus(ApprovalInstance instance) {
        String status     = instance.getStatus();
        Long   businessId = instance.getBusinessId();
        Long   approverId = getCurrentApproverId(instance);
        try {
            if ("LEAVE".equals(instance.getBusinessType())) {
                leaveApplicationRepository.findById(businessId).ifPresent(app -> {
                    app.setApprovalStatus(status);
                    app.setApproverId(approverId);
                    if (INSTANCE_PENDING.equals(status)) { app.setApprovalComment(null); app.setApprovalTime(null); }
                    else { app.setApprovalTime(LocalDateTime.now()); }
                    leaveApplicationRepository.save(app);
                });
            } else if ("AWARD".equals(instance.getBusinessType())) {
                awardApplicationRepository.findById(businessId).ifPresent(app -> {
                    app.setApprovalStatus(status);
                    app.setApproverId(approverId);
                    if (INSTANCE_PENDING.equals(status)) { app.setApprovalComment(null); app.setApprovalTime(null); }
                    else { app.setApprovalTime(LocalDateTime.now()); }
                    awardApplicationRepository.save(app);
                });
            }
        } catch (Exception e) {
            log.error("更新业务状态失败", e);
        }
    }

    private Long getCurrentApproverId(ApprovalInstance instance) {
        return getCurrentTasks(instance).stream()
                .filter(t -> TASK_PROCESSING.equals(t.getStatus()))
                .map(ApprovalTask::getApproverId)
                .findFirst().orElse(null);
    }

    // =======================================================
    // 通知方法
    // =======================================================

    /** 通知审批者：您有新的待审批任务 */
    private void notifyApprover(ApprovalTask task, ApprovalInstance instance) {
        try {
            User approver = userRepository.findById(task.getApproverId()).orElse(null);
            if (approver == null) {
                log.warn("审批者不存在，跳过通知，approverId: {}", task.getApproverId());
                return;
            }
            String businessName = resolveBusinessName(instance.getBusinessType());
            String stepName = task.getStep().getStepName();
            NotificationMessage msg = buildNotificationMessage(
                    approver,
                    instance,
                    instance.getBusinessType(),
                    "您有新的" + businessName + "待审批",
                    String.format("您有一条新的%s（编号 #%d）需要您进行【%s】审批，请及时处理。",
                            businessName, instance.getBusinessId(), stepName),
                    Arrays.asList("SITE", "EMAIL")
            );
            notificationService.sendAwardApprovalNotification(msg);
        } catch (Exception e) {
            log.error("发送审批者通知失败，approverId: {}", task.getApproverId(), e);
        }
    }

    /** 通知申请者：审批最终结果（通过或拒绝） */
    private void notifyApplicant(ApprovalInstance instance, String finalStatus,
                                 Long approverId, String comment) {
        try {
            Long applicantId = instance.getApplicantId();
            if (applicantId == null) {
                log.warn("审批实例未记录申请人ID，跳过申请者通知，instanceId: {}", instance.getId());
                return;
            }
            User applicant = userRepository.findById(applicantId).orElse(null);
            if (applicant == null) {
                log.warn("申请者不存在，跳过通知，applicantId: {}", applicantId);
                return;
            }
            User approver = approverId != null ? userRepository.findById(approverId).orElse(null) : null;
            String businessName = resolveBusinessName(instance.getBusinessType());
            String statusText = INSTANCE_APPROVED.equals(finalStatus) ? "已通过" : "未通过";
            NotificationMessage msg = buildNotificationMessage(
                    applicant,
                    instance,
                    "APPROVAL_RESULT",
                    businessName + "审批结果通知",
                    String.format("您的%s（编号 #%d）审批%s。审批意见：%s",
                            businessName, instance.getBusinessId(), statusText,
                            comment != null ? comment : "无"),
                    Arrays.asList("SITE", "EMAIL")
            );
            msg.setApprovalStatus(finalStatus);
            msg.setApprovalComment(comment);
            msg.setApproverName(approver != null ? approver.getRealName() : "系统");
            notificationService.sendAwardApprovalNotification(msg);
        } catch (Exception e) {
            log.error("发送申请者通知失败，instanceId: {}", instance.getId(), e);
        }
    }

    private NotificationMessage buildNotificationMessage(User receiver, ApprovalInstance instance,
                                                         String type, String title, String content,
                                                         List<String> channels) {
        NotificationMessage message = NotificationMessage.builder()
                .userId(receiver.getId())
                .phone(receiver.getPhone())
                .email(receiver.getEmail())
                .type(type)
                .title(title)
                .content(content)
                .businessId(instance.getBusinessId())
                .businessType(instance.getBusinessType())
                .channels(channels)
                .receiverName(receiver.getRealName())
                .build();
        fillNotificationBusinessInfo(message, instance, receiver);
        return message;
    }

    private void fillNotificationBusinessInfo(NotificationMessage message, ApprovalInstance instance, User receiver) {
        message.setApplicantName(receiver.getRealName());

        if ("AWARD".equals(instance.getBusinessType())) {
            AwardApplication application = awardApplicationRepository.findById(instance.getBusinessId()).orElse(null);
            if (application != null) {
                message.setApplicantName(application.getStudentName() != null ? application.getStudentName() : receiver.getRealName());
                message.setApplicationType(application.getApplicationType());
                message.setAwardName(application.getAwardName());
                message.setAmount(application.getAmount());
            }
            return;
        }

        if ("LEAVE".equals(instance.getBusinessType())) {
            LeaveApplication application = leaveApplicationRepository.findById(instance.getBusinessId()).orElse(null);
            if (application != null) {
                message.setApplicantName(application.getStudentName() != null ? application.getStudentName() : receiver.getRealName());
                message.setApplicationType("LEAVE");
                message.setAwardName(resolveLeaveTypeName(application.getLeaveType()));
                message.setLeaveStartDate(application.getStartDate());
                message.setLeaveEndDate(application.getEndDate());
                message.setLeaveDays(application.getDays());
                message.setLeaveReason(application.getReason());
            }
        }
    }

    private String resolveLeaveTypeName(String leaveType) {
        if (leaveType == null) {
            return null;
        }
        return switch (leaveType) {
            case "SICK" -> "病假";
            case "PERSONAL" -> "事假";
            case "OFFICIAL" -> "公假";
            default -> leaveType;
        };
    }

    private String resolveBusinessName(String businessType) {
        return "LEAVE".equals(businessType) ? "请假申请" : "奖助申请";
    }
}
