package com.example.learnworkagent.domain.approval.service.impl;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveTypeEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.common.enums.NotificationChannelEnum;
import com.example.learnworkagent.common.enums.NotificationTypeEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
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
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Role;
import com.example.learnworkagent.domain.user.entity.Teacher;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.repository.RoleRepository;
import com.example.learnworkagent.domain.user.repository.TeacherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


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

    private static final String BUSINESS_TYPE_LEAVE = NotificationBusinessTypeEnum.LEAVE.getCode();
    private static final String BUSINESS_TYPE_AWARD = NotificationBusinessTypeEnum.AWARD.getCode();
    private static final List<String> DEFAULT_NOTIFICATION_CHANNELS = List.of(
            NotificationChannelEnum.SITE.getCode(),
            NotificationChannelEnum.EMAIL.getCode()
    );

    private static final String INSTANCE_PENDING  = ApprovalStatusEnum.PENDING.getCode();
    private static final String INSTANCE_APPROVED = ApprovalStatusEnum.APPROVED.getCode();
    private static final String INSTANCE_REJECTED = ApprovalStatusEnum.REJECTED.getCode();
    private static final String TASK_PROCESSING   = ApprovalStatusEnum.PROCESSING.getCode();
    private static final String TASK_APPROVED     = ApprovalStatusEnum.APPROVED.getCode();
    private static final String TASK_REJECTED     = ApprovalStatusEnum.REJECTED.getCode();
    private static final String STATUS_TEXT_APPROVED = "已通过";
    private static final String STATUS_TEXT_REJECTED = "未通过";

    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalTaskRepository     taskRepository;
    private final ApprovalProcessRepository  processRepository;
    private final ApprovalStepRepository     stepRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AdminRepository            adminRepository;
    private final TeacherRepository          teacherRepository;
    private final RoleRepository             roleRepository;
    private final NotificationService        notificationService;
    private final ObjectMapper               objectMapper;

    // =======================================================
    // 公开接口
    // =======================================================

    @Override
    public void createApprovalInstance(String businessType, Long businessId,
                                       Long applicantId, String applicantInfo) {
        ApprovalProcess process = processRepository.findByProcessTypeAndEnabledTrue(businessType)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "未找到启用的审批流程: " + businessType));
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        if (steps.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审批流程未配置审批步骤: " + businessType);
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setApplicantId(applicantId);
        instance.setProcess(process);
        instance.markPending(steps.get(0).getStepOrder());
        instance = instanceRepository.save(instance);

        for (ApprovalStep step : steps) {
            createApprovalTasks(instance, step, applicantInfo);
        }
        activateStep(instance, instance.getCurrentStep());
        updateBusinessStatus(instance, null, null);
    }

    @Override
    public ApprovalTask processApprovalTask(Long taskId, Long approverId, String status, String comment) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "审批任务不存在: " + taskId));
        if (!task.isProcessing()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前任务未到审批阶段或已处理");
        }
        if (!task.getApproverId().equals(approverId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权审批此任务");
        }
        if (!TASK_APPROVED.equals(status) && !TASK_REJECTED.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的审批状态: " + status);
        }

        task.markApproved(comment);
        if (TASK_REJECTED.equals(status)) {
            task.markRejected(comment);
        }
        task = taskRepository.save(task);

        ApprovalInstance instance = task.getInstance();
        handleStepProgress(instance, task.getStep().getStepOrder(), status, approverId, comment);
        updateBusinessStatus(instance, approverId, comment);
        return task;
    }

    @Override
    public ApprovalInstance getApprovalInstance(String businessType, Long businessId) {
        return instanceRepository.findByBusinessTypeAndBusinessId(businessType, businessId).orElse(null);
    }

    @Override
    public List<ApprovalTaskDTO> getPendingTasks(Long approverId) {
        List<ApprovalTask> tasks = taskRepository.findByApproverIdAndStatus(approverId, TASK_PROCESSING);
        return tasks.stream().map(task -> {
            ApprovalTaskDTO dto = new ApprovalTaskDTO();
            dto.setId(task.getId());
            dto.setBusinessId(task.getInstance().getBusinessId());
            dto.setBusinessType(task.getInstance().getBusinessType());
            dto.setStatus(task.getStatus());
            dto.setCreateTime(task.getCreateTime());
            dto.setApproverId(task.getApproverId());
            dto.setStepName(task.getStep().getStepName());
            return dto;
        }).toList();
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
            if (applicantInfo != null && !applicantInfo.isBlank()) {
                objectMapper.readTree(applicantInfo);
            }
            List<Long> approvers = findApprovers(step).stream().distinct().toList();
            if (approvers.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "审批步骤未找到审批人: " + step.getStepName());
            }
            for (int i = 0; i < approvers.size(); i++) {
                ApprovalTask task = new ApprovalTask();
                task.setInstance(instance);
                task.setStep(step);
                task.setApproverId(approvers.get(i));
                task.markPending();
                task.setTaskOrder(i + 1);
                taskRepository.save(task);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("创建审批任务失败", exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建审批任务失败");
        }
    }

    private List<Long> findApprovers(ApprovalStep step) {
        List<Long> approvers = new ArrayList<>();
        if (step.hasAssignedApprover()) {
            approvers.add(step.getApproverUserId());
            return approvers;
        }

        Role role = roleRepository.findByRoleName(step.getApproverRole())
                .orElseThrow(() -> new BusinessException("审批角色不存在: " + step.getApproverRole()));
        adminRepository.findByRoleId(role.getId())
                .stream()
                .filter(Admin::isEnabled)
                .forEach(admin -> approvers.add(admin.getId()));
        return approvers;
    }

    private void handleStepProgress(ApprovalInstance instance, Integer stepOrder,
                                    String taskStatus, Long approverId, String comment) {
        List<ApprovalTask> stepTasks = taskRepository
                .findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder);
        if (stepTasks.isEmpty()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "审批步骤任务不存在 stepOrder=" + stepOrder);
        }
        ApprovalStep step = stepTasks.get(0).getStep();

        if (TASK_REJECTED.equals(taskStatus) && !canContinueAfterReject(step)) {
            instance.markRejected();
            instanceRepository.save(instance);
            notifyApplicant(instance, INSTANCE_REJECTED, approverId, comment);
            return;
        }

        if (!isStepApproved(stepTasks)) {
            instance.markPending(stepOrder);
            instanceRepository.save(instance);
            return;
        }

        ApprovalStep nextStep = findNextStep(instance.getProcess(), stepOrder);
        if (nextStep != null) {
            instance.markPending(nextStep.getStepOrder());
            instanceRepository.save(instance);
            activateStep(instance, nextStep.getStepOrder());
        } else {
            instance.markApproved();
            instance.setCurrentStep(stepOrder);
            instanceRepository.save(instance);
            notifyApplicant(instance, INSTANCE_APPROVED, approverId, comment);
        }
    }

    private boolean isStepApproved(List<ApprovalTask> stepTasks) {
        ApprovalStep step = stepTasks.get(0).getStep();
        if (step.isMultipleApproval() && step.requiresAllPass()) {
            return stepTasks.stream().allMatch(ApprovalTask::isApproved);
        }
        return stepTasks.stream().anyMatch(ApprovalTask::isApproved);
    }

    private boolean canContinueAfterReject(ApprovalStep step) {
        return step.allowsRejectContinue();
    }

    private void activateStep(ApprovalInstance instance, Integer stepOrder) {
        taskRepository.findByInstanceAndStepStepOrderOrderByTaskOrderAsc(instance, stepOrder)
                .forEach(task -> {
                    if (task.isPending()) {
                        task.markProcessing();
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

    private void updateBusinessStatus(ApprovalInstance instance, Long fallbackApproverId, String approvalComment) {
        String status = instance.getStatus();
        Long businessId = instance.getBusinessId();
        Long approverId = resolveApproverId(instance, fallbackApproverId);
        try {
            if (BUSINESS_TYPE_LEAVE.equals(instance.getBusinessType())) {
                leaveApplicationRepository.findById(businessId).ifPresent(application -> {
                    applyApprovalFields(application, status, approverId, approvalComment);
                    leaveApplicationRepository.save(application);
                });
            } else if (BUSINESS_TYPE_AWARD.equals(instance.getBusinessType())) {
                awardApplicationRepository.findById(businessId).ifPresent(application -> {
                    applyApprovalFields(application, status, approverId, approvalComment);
                    awardApplicationRepository.save(application);
                });
            }
        } catch (Exception exception) {
            log.error("更新业务状态失败，businessType: {}, businessId: {}", instance.getBusinessType(), businessId, exception);
        }
    }

    private void applyApprovalFields(LeaveApplication application, String status, Long approverId, String approvalComment) {
        application.setApprovalStatus(status);
        application.setApproverId(approverId);
        if (INSTANCE_PENDING.equals(status)) {
            application.setApprovalComment(null);
            application.setApprovalTime(null);
            return;
        }
        application.setApprovalComment(approvalComment);
        application.setApprovalTime(LocalDateTime.now());
    }

    private void applyApprovalFields(AwardApplication application, String status, Long approverId, String approvalComment) {
        application.setApprovalStatus(status);
        application.setApproverId(approverId);
        if (INSTANCE_PENDING.equals(status)) {
            application.setApprovalComment(null);
            application.setApprovalTime(null);
            return;
        }
        application.setApprovalComment(approvalComment);
        application.setApprovalTime(LocalDateTime.now());
    }

    private Long resolveApproverId(ApprovalInstance instance, Long fallbackApproverId) {
        Long currentApproverId = getCurrentApproverId(instance);
        return currentApproverId != null ? currentApproverId : fallbackApproverId;
    }

    private Long getCurrentApproverId(ApprovalInstance instance) {
        return getCurrentTasks(instance).stream()
                .filter(ApprovalTask::isProcessing)
                .map(ApprovalTask::getApproverId)
                .findFirst().orElse(null);
    }

    // =======================================================
    // 通知方法
    // =======================================================

    /** 通知审批者：您有新的待审批任务 */
    private void notifyApprover(ApprovalTask task, ApprovalInstance instance) {
        try {
            Admin approver = adminRepository.findById(task.getApproverId()).orElse(null);
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
                            businessName, instance.getBusinessId(), stepName)
            );
            notificationService.sendAwardApprovalNotification(msg);
        } catch (Exception exception) {
            log.error("发送审批者通知失败，approverId: {}", task.getApproverId(), exception);
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
            Admin applicant = adminRepository.findById(applicantId).orElse(null);
            if (applicant == null) {
                log.warn("申请者不存在，跳过通知，applicantId: {}", applicantId);
                return;
            }
            Admin approver = approverId != null ? adminRepository.findById(approverId).orElse(null) : null;
            String businessName = resolveBusinessName(instance.getBusinessType());
            String statusText = resolveApplicantStatusText(finalStatus);
            NotificationMessage msg = buildNotificationMessage(
                    applicant,
                    instance,
                    NotificationTypeEnum.APPROVAL_RESULT.getCode(),
                    businessName + "审批结果通知",
                    String.format("您的%s（编号 #%d）审批%s。审批意见：%s",
                            businessName, instance.getBusinessId(), statusText,
                            comment != null ? comment : "无")
            );
            msg.setApprovalStatus(finalStatus);
            msg.setApprovalComment(comment);
            msg.setApproverName(resolveDisplayName(approver));
            notificationService.sendAwardApprovalNotification(msg);
        } catch (Exception exception) {
            log.error("发送申请者通知失败，instanceId: {}", instance.getId(), exception);
        }
    }

    private NotificationMessage buildNotificationMessage(Admin receiver, ApprovalInstance instance,
                                                         String type, String title, String content) {
        Teacher teacher = teacherRepository.findById(receiver.getTeacherId()).orElse(null);
        NotificationMessage message = NotificationMessage.builder()
                .userId(receiver.getId())
                .phone(teacher != null ? teacher.getPhone() : null)
                .email(null)
                .wechatOpenId(null)
                .weworkUserId(null)
                .type(type)
                .title(title)
                .content(content)
                .businessId(instance.getBusinessId())
                .businessType(instance.getBusinessType())
                .channels(ApprovalServiceImpl.DEFAULT_NOTIFICATION_CHANNELS)
                .receiverName(resolveDisplayName(receiver))
                .build();
        fillNotificationBusinessInfo(message, instance, receiver);
        return message;
    }

    private void fillNotificationBusinessInfo(NotificationMessage message, ApprovalInstance instance, Admin receiver) {
        message.setApplicantName(resolveDisplayName(receiver));

        if (BUSINESS_TYPE_AWARD.equals(instance.getBusinessType())) {
            AwardApplication application = awardApplicationRepository.findById(instance.getBusinessId()).orElse(null);
            if (application != null) {
                message.setApplicantName(application.getStudentName() != null ? application.getStudentName() : resolveDisplayName(receiver));
                message.setApplicationType(application.getApplicationType());
                message.setAwardName(application.getAwardName());
                message.setAmount(application.getAmount());
            }
            return;
        }

        if (BUSINESS_TYPE_LEAVE.equals(instance.getBusinessType())) {
            LeaveApplication application = leaveApplicationRepository.findById(instance.getBusinessId()).orElse(null);
            if (application != null) {
                message.setApplicantName(application.getStudentName() != null ? application.getStudentName() : resolveDisplayName(receiver));
                message.setApplicationType(BUSINESS_TYPE_LEAVE);
                message.setAwardName(LeaveTypeEnum.getDescriptionByCode(application.getLeaveType()));
                message.setLeaveStartDate(application.getStartDate());
                message.setLeaveEndDate(application.getEndDate());
                message.setLeaveDays(application.getDays());
                message.setLeaveReason(application.getReason());
            }
        }
    }

    private String resolveDisplayName(Admin admin) {
        if (admin == null) {
            return "系统";
        }
        return teacherRepository.findById(admin.getTeacherId())
                .map(Teacher::getName)
                .filter(name -> !name.isBlank())
                .orElse(admin.getNick());
    }

    private String resolveBusinessName(String businessType) {
        return BUSINESS_TYPE_LEAVE.equals(businessType) ? "请假申请" : "奖助申请";
    }

    private String resolveApplicantStatusText(String finalStatus) {
        return INSTANCE_APPROVED.equals(finalStatus) ? STATUS_TEXT_APPROVED : STATUS_TEXT_REJECTED;
    }
}
