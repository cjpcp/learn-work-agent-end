package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveSlipStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.utils.ApplicationServiceUtils;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.dto.LeaveSlipPreviewRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.infrastructure.external.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 请假申请服务.
 * <p>提供请假申请、审批、销假等业务逻辑.</p>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private static final String BUSINESS_TYPE_LEAVE = "LEAVE";
    private static final String PROCESS_TYPE_LEAVE = "leave";
    private static final String PROCESS_TYPE_LEAVE_CANCEL = "leave_cancel";
    private static final String PROCESS_STATUS_PENDING = "pending";
    private static final String PROCESS_STATUS_COMPLETED = "completed";
    private static final String LEAVE_APPLICATION_NAME = "请假申请";
    private static final String LEAVE_APPROVAL_NAME = "请假审批";
    private static final String LEAVE_CANCEL_APPLICATION_NAME = "销假申请";
    private static final String LEAVE_CANCEL_APPROVAL_NAME = "销假审批";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGE_SIZE = 100;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TemplateService templateService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    /**
     * 提交请假申请。
     *
     * @param applicantId 申请人ID
     * @param request     请假申请参数
     * @return 保存后的请假申请
     */
    @Transactional
    public LeaveApplication submitLeaveApplication(Long applicantId, LeaveApplicationRequest request) {
        validateLeaveDates(request);

        int leaveDays = calculateLeaveDays(request);
        LeaveApplication application = buildLeaveApplication(applicantId, request, leaveDays);
        LeaveApplication savedApplication = leaveApplicationRepository.save(application);
        createApprovalFlow(savedApplication, applicantId, request);
        return savedApplication;
    }

    /**
     * 生成请假条预览。
     *
     * @param request 请假条预览参数
     * @return 请假条PDF字节数组
     */
    public byte[] generateLeaveSlipPreview(LeaveSlipPreviewRequest request) {
        int days = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        try {
            return templateService.generateLeaveSlipPreview(
                    request.getStudentName(),
                    request.getCardNumber(),
                    request.getGrade(),
                    request.getClassName(),
                    request.getPhone(),
                    request.getLeaveType(),
                    request.getStartDate(),
                    request.getEndDate(),
                    days,
                    request.getReason()
            );
        } catch (Exception exception) {
            log.error("生成请假条预览失败", exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成请假条预览失败: " + exception.getMessage());
        }
    }

    /**
     * 申请销假。
     *
     * @param applicationId 请假申请ID
     */
    @Transactional
    public void requestCancelLeave(Long applicationId) {
        LeaveApplication application = getApplicationById(applicationId);
        validateCancelLeaveRequest(application);
        application.setCancelRequested(true);
        application.setCancelApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        leaveApplicationRepository.save(application);
    }

    /**
     * 审批销假申请。
     *
     * @param applicationId 请假申请ID
     * @param approved      是否批准
     * @param comment       审批意见
     */
    @Transactional
    public void approveCancelRequest(Long applicationId, boolean approved, String comment) {
        LeaveApplication application = getApplicationById(applicationId);
        validateCancelApprovalRequest(application);

        if (approved) {
            application.markCancelled();
        }
        application.setCancelApprovalStatus(approved ? ApprovalStatusEnum.APPROVED.getCode() : ApprovalStatusEnum.REJECTED.getCode());
        application.setCancelApprovalComment(comment);
        application.setCancelApprovalTime(LocalDateTime.now());
        leaveApplicationRepository.save(application);
    }

    /**
     * 获取申请详情。
     *
     * @param applicationId 请假申请ID
     * @return 请假申请
     */
    public LeaveApplication getApplicationById(Long applicationId) {
        return leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));
    }

    /**
     * 分页查询用户的请假申请。
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getUserApplications(Long userId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<LeaveApplication> page = leaveApplicationRepository
                .findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);
        return buildPageResult(page, pageRequest);
    }

    /**
     * 分页查询待审批销假申请。
     *
     * @param approverId  审批人ID（原请假审批人）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getPendingCancelRequests(Long approverId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<LeaveApplication> page = leaveApplicationRepository.findPendingCancelRequestsByApproverId(approverId, pageable);
        return buildPageResult(page, pageRequest);
    }

    /**
     * 撤销请假申请。
     *
     * @param applicationId 请假申请ID
     * @param userId        用户ID
     */
    @Transactional
    public void withdrawApplication(Long applicationId, Long userId) {
        LeaveApplication application = getApplicationById(applicationId);
        validateWithdrawPermission(application, userId);
        approvalService.cancelApprovalInstance(BUSINESS_TYPE_LEAVE, applicationId);
    }

    /**
     * 获取待办流程项。
     *
     * @param userId  用户ID
     * @param isStaff 是否为教职工
     * @return 待办流程项列表
     */
    public List<ProcessItem> getPendingProcessItems(Long userId, boolean isStaff) {
        return isStaff ? getStaffPendingItems(userId) : getUserPendingItems(userId);
    }

    /**
     * 获取已完成流程项。
     *
     * @param userId  用户ID
     * @param isStaff 是否为教职工
     * @return 已完成流程项列表
     */
    public List<ProcessItem> getCompletedProcessItems(Long userId, boolean isStaff) {
        return isStaff ? getStaffCompletedItems(userId) : getUserCompletedItems(userId);
    }

    /**
     * 获取待办销假流程项。
     *
     * @param userId  用户ID
     * @param isStaff 是否为教职工
     * @return 待办销假流程项列表
     */
    public List<ProcessItem> getPendingCancelProcessItems(Long userId, boolean isStaff) {
        return isStaff ? getStaffPendingCancelItems(userId) : getUserPendingCancelItems(userId);
    }

    /**
     * 获取已完成销假流程项。
     *
     * @param userId  用户ID
     * @param isStaff 是否为教职工
     * @return 已完成销假流程项列表
     */
    public List<ProcessItem> getCompletedCancelProcessItems(Long userId, boolean isStaff) {
        return isStaff ? getStaffCompletedCancelItems(userId) : getUserCompletedCancelItems(userId);
    }

    private List<ProcessItem> getStaffPendingItems(Long userId) {
        return approvalService.getPendingTasks(userId).stream()
                .filter(task -> BUSINESS_TYPE_LEAVE.equals(task.getBusinessType()))
                .map(task -> approvalService.getTaskById(task.getId()))
                .filter(Objects::nonNull)
                .map(this::buildLeaveApprovalProcessItem)
                .toList();
    }

    private List<ProcessItem> getUserPendingItems(Long userId) {
        return findApplicationsByStatus(userId, ApprovalStatusEnum.PENDING).stream()
                .map(app -> buildProcessItem(
                        app.getId(),
                        LEAVE_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE,
                        formatDateTime(app.getCreateTime()),
                        PROCESS_STATUS_PENDING,
                        "您的请假申请正在审批中"
                ))
                .toList();
    }

    private List<ProcessItem> getStaffCompletedItems(Long userId) {
        return approvalService.getCompletedTasksByApprover(userId).stream()
                .filter(task -> BUSINESS_TYPE_LEAVE.equals(task.getBusinessType()))
                .map(task -> {
                    String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(task.getStatus()) ? "已批准" : "已拒绝";
                    return buildProcessItem(
                            task.getBusinessId(),
                            LEAVE_APPROVAL_NAME,
                            PROCESS_TYPE_LEAVE,
                            formatDateTime(task.getCreateTime()),
                            PROCESS_STATUS_COMPLETED,
                            "学生的请假申请您" + statusText,
                            true
                    );
                })
                .toList();
    }

    private List<ProcessItem> getUserCompletedItems(Long userId) {
        return Stream.of(ApprovalStatusEnum.APPROVED, ApprovalStatusEnum.REJECTED)
                .flatMap(status -> findApplicationsByStatus(userId, status).stream())
                .map(app -> {
                    String description = ApprovalStatusEnum.APPROVED.getCode().equals(app.getApprovalStatus())
                            ? "您的请假申请已批准"
                            : "您的请假申请已拒绝";
                    return buildProcessItem(
                            app.getId(),
                            LEAVE_APPLICATION_NAME,
                            PROCESS_TYPE_LEAVE,
                            formatDateTime(app.getCreateTime()),
                            PROCESS_STATUS_COMPLETED,
                            description
                    );
                })
                .toList();
    }

    private List<ProcessItem> getStaffPendingCancelItems(Long userId) {
        return leaveApplicationRepository.findPendingCancelRequestsByApproverId(userId, createMaxPageRequest()).stream()
                .map(app -> buildProcessItem(
                        app.getId(),
                        LEAVE_CANCEL_APPROVAL_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        formatDateTime(getCancelOrCreateTime(app)),
                        PROCESS_STATUS_PENDING,
                        app.getStudentName() + "的销假申请需要您审批",
                        true
                ))
                .toList();
    }

    private List<ProcessItem> getUserPendingCancelItems(Long userId) {
        return leaveApplicationRepository.findPendingCancelRequestsByApplicantId(userId, createMaxPageRequest()).stream()
                .map(app -> buildProcessItem(
                        app.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        formatDateTime(getCancelOrCreateTime(app)),
                        PROCESS_STATUS_PENDING,
                        "您的销假申请等待审批"
                ))
                .toList();
    }

    private List<ProcessItem> getStaffCompletedCancelItems(Long userId) {
        return findCompletedCancelApplications(userId, true).stream()
                .map(app -> buildProcessItem(
                        app.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        formatDateTime(getCancelOrCreateTime(app)),
                        PROCESS_STATUS_COMPLETED,
                        app.getStudentName() + "的销假申请" + getCancelStatusText(app)
                ))
                .toList();
    }

    private List<ProcessItem> getUserCompletedCancelItems(Long userId) {
        return findCompletedCancelApplications(userId, false).stream()
                .map(app -> buildProcessItem(
                        app.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        formatDateTime(getCancelOrCreateTime(app)),
                        PROCESS_STATUS_COMPLETED,
                        "您的销假申请" + getCancelStatusText(app)
                ))
                .toList();
    }

    private void validateLeaveDates(LeaveApplicationRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能早于今天");
        }
    }

    private void validateCancelLeaveRequest(LeaveApplication application) {
        if (!application.isApproved()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有已批准的申请才能申请销假");
        }
        if (Boolean.TRUE.equals(application.getCancelled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已销假");
        }
        if (Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已提交销假申请");
        }
    }

    private void validateCancelApprovalRequest(LeaveApplication application) {
        if (!Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请未提交销假申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getCancelApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "销假申请已审批");
        }
    }

    private void validateWithdrawPermission(LeaveApplication application, Long userId) {
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤销待审批的申请");
        }
    }

    private int calculateLeaveDays(LeaveApplicationRequest request) {
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        return Math.toIntExact(days);
    }

    private LeaveApplication buildLeaveApplication(Long applicantId, LeaveApplicationRequest request, int days) {
        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setLeaveType(request.getLeaveType());
        application.setStartDate(request.getStartDate());
        application.setEndDate(request.getEndDate());
        application.setDays(days);
        application.setReason(request.getReason());
        application.setAttachmentUrl(request.getAttachmentUrl());
        application.setStudentName(request.getStudentName());
        application.setDepartmentName(request.getDepartmentName());
        application.setGrade(request.getGrade());
        application.setClassName(request.getClassName());
        application.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        application.setLeaveSlipStatus(LeaveSlipStatusEnum.NOT_GENERATED.getCode());
        return application;
    }

    private void createApprovalFlow(LeaveApplication application, Long applicantId, LeaveApplicationRequest request) {
        try {
            String applicantInfoJson = objectMapper.writeValueAsString(buildApplicantInfo(request));
            approvalService.createApprovalInstance(BUSINESS_TYPE_LEAVE, application.getId(), applicantId, applicantInfoJson);
        } catch (Exception exception) {
            log.error("创建请假审批流程失败，申请ID: {}", application.getId(), exception);
        }
    }

    private Map<String, Object> buildApplicantInfo(LeaveApplicationRequest request) {
        Map<String, Object> applicantInfo = new HashMap<>(4);
        applicantInfo.put("studentName", request.getStudentName());
        applicantInfo.put("departmentName", request.getDepartmentName());
        applicantInfo.put("grade", request.getGrade());
        applicantInfo.put("className", request.getClassName());
        return applicantInfo;
    }

    private Pageable buildPageable(PageRequest pageRequest) {
        return ApplicationServiceUtils.buildPageable(pageRequest);
    }

    private PageResult<LeaveApplication> buildPageResult(Page<LeaveApplication> page, PageRequest pageRequest) {
        return ApplicationServiceUtils.buildPageResult(page, pageRequest);
    }

    private Page<LeaveApplication> findApplicationsByStatus(Long userId, ApprovalStatusEnum status) {
        return leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                userId, status.getCode(), createMaxPageRequest());
    }

    private List<LeaveApplication> findCompletedCancelApplications(Long userId, boolean isStaff) {
        List<String> completedStatuses = List.of(
                ApprovalStatusEnum.APPROVED.getCode(),
                ApprovalStatusEnum.REJECTED.getCode()
        );
        Page<LeaveApplication> page = isStaff
                ? leaveApplicationRepository.findByApproverIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                userId, completedStatuses, createMaxPageRequest())
                : leaveApplicationRepository.findByApplicantIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                userId, completedStatuses, createMaxPageRequest());
        return page.getContent();
    }

    private ProcessItem buildProcessItem(Long id, String name, String type, String createTime, String status, String description) {
        return buildProcessItem(id, name, type, createTime, status, description, false);
    }

    private ProcessItem buildProcessItem(Long id, String name, String type, String createTime, String status, String description, boolean allowAction) {
        ProcessItem item = new ProcessItem();
        item.setId(String.valueOf(id));
        item.setName(name);
        item.setType(type);
        item.setCreateTime(createTime);
        item.setStatus(status);
        item.setDescription(description);
        item.setAllowAction(allowAction);
        return item;
    }

    private ProcessItem buildLeaveApprovalProcessItem(ApprovalTask task) {
        return buildProcessItem(
                task.getInstance().getBusinessId(),
                LEAVE_APPROVAL_NAME,
                PROCESS_TYPE_LEAVE,
                formatDateTime(task.getInstance().getCreateTime()),
                PROCESS_STATUS_PENDING,
                "学生的请假申请需要您审批",
                true
        );
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }

    private LocalDateTime getCancelOrCreateTime(LeaveApplication application) {
        return application.getCancelTime() != null ? application.getCancelTime() : application.getCreateTime();
    }

    private String getCancelStatusText(LeaveApplication application) {
        return ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus()) ? "已批准" : "已拒绝";
    }

    private org.springframework.data.domain.PageRequest createMaxPageRequest() {
        return org.springframework.data.domain.PageRequest.of(0, MAX_PAGE_SIZE);
    }
}
