package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import com.example.learnworkagent.infrastructure.external.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 请假申请服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TemplateService templateService;
    private final OssService ossService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    /**
     * 提交请假申请
     */
    @Transactional
    public LeaveApplication submitLeaveApplication(Long applicantId, LeaveApplicationRequest request) {
        // 验证日期
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能早于今天");
        }

        // 计算请假天数
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        LeaveApplication application = getLeaveApplication(applicantId, request, (int) days);

        LeaveApplication savedApplication = leaveApplicationRepository.save(application);

        // 创建审批流程
        try {
            // 构建申请人信息
            HashMap<String, Object> applicantInfo = new HashMap<>();
            applicantInfo.put("studentName", request.getStudentName());
            applicantInfo.put("departmentId", request.getDepartmentId());
            applicantInfo.put("grade", request.getGrade());
            applicantInfo.put("className", request.getClassName());

            String applicantInfoJson = objectMapper.writeValueAsString(applicantInfo);
            approvalService.createApprovalInstance("LEAVE", savedApplication.getId(), applicantId, applicantInfoJson);
        } catch (Exception e) {
            log.error("创建审批流程失败", e);
        }

        return savedApplication;
    }

    private static LeaveApplication getLeaveApplication(Long applicantId, LeaveApplicationRequest request, int days) {
        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setLeaveType(request.getLeaveType());
        application.setStartDate(request.getStartDate());
        application.setEndDate(request.getEndDate());
        application.setDays(days);
        application.setReason(request.getReason());
        application.setAttachmentUrl(request.getAttachmentUrl());
        application.setApprovalStatus("PENDING");
        application.setStudentName(request.getStudentName());
        application.setDepartmentId(request.getDepartmentId());
        application.setGrade(request.getGrade());
        application.setClassName(request.getClassName());
        application.setApprovalStatus("PENDING");
        return application;
    }

    /**
     * 审批请假申请
     */
    @Transactional
    public void approveLeaveApplication(Long applicationId, Long approverId,
                                        String approvalStatus, String approvalComment) {
        leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));

        ApprovalInstance approvalInstance = approvalService.getApprovalInstance("LEAVE", applicationId);
        ApprovalTask currentTask = getCurrentTask(approvalInstance, approverId);

        approvalService.processApprovalTask(currentTask.getId(), approverId, approvalStatus, approvalComment);

        LeaveApplication refreshedApplication = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));

        if ("APPROVED".equals(refreshedApplication.getApprovalStatus())) {
            generateLeaveSlip(refreshedApplication);
        }

        if (!"PENDING".equals(refreshedApplication.getApprovalStatus())) {
            sendApprovalNotification(refreshedApplication, approverId);
        }
    }

    private ApprovalTask getCurrentTask(ApprovalInstance approvalInstance, Long approverId) {
        if (approvalInstance == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审批流程不存在");
        }

        return approvalService.getCurrentTasks(approvalInstance).stream()
                .filter(task -> Objects.equals(task.getApproverId(), approverId))
                .filter(task -> "PROCESSING".equals(task.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "当前没有可处理的审批任务"));
    }

    /**
     * 发送请假审批结果通知
     *
     * @param application 请假申请
     * @param approverId  审批人ID
     */
    private void sendApprovalNotification(LeaveApplication application, Long approverId) {
        try {
            // 获取申请人信息
            User applicant = userRepository.findById(application.getApplicantId())
                    .orElse(null);

            // 获取审批人信息
            User approver = userRepository.findById(approverId)
                    .orElse(null);

            if (applicant == null) {
                log.warn("请假申请人不存在，无法发送通知，用户ID: {}", application.getApplicantId());
                return;
            }

            // 构建通知消息
            String statusText = "APPROVED".equals(application.getApprovalStatus()) ? "已通过" : "未通过";
            String title = "请假申请审批结果通知";
            String content = String.format("您的%s（%s至%s，共%d天）申请%s。审批意见：%s",
                    getLeaveTypeName(application.getLeaveType()),
                    application.getStartDate(),
                    application.getEndDate(),
                    application.getDays(),
                    statusText,
                    application.getApprovalComment());

            NotificationMessage message = NotificationMessage.builder()
                    .userId(application.getApplicantId())
                    .email(applicant.getEmail())
                    .type("LEAVE_APPROVAL")
                    .title(title)
                    .content(content)
                    .businessId(application.getId())
                    .businessType("LEAVE_APPLICATION")
                    .channels(Arrays.asList("SITE", "EMAIL"))
                    .applicantName(applicant.getRealName())
                    .applicationType(application.getLeaveType())
                    .approvalStatus(application.getApprovalStatus())
                    .approvalComment(application.getApprovalComment())
                    .approverName(approver != null ? approver.getRealName() : "系统")
                    .build();

            // 发送通知
            notificationService.sendAwardApprovalNotification(message);

            log.info("请假审批结果通知已发送，申请ID: {}, 用户ID: {}",
                    application.getId(), application.getApplicantId());
        } catch (Exception e) {
            log.error("发送请假审批结果通知失败，申请ID: {}", application.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 获取请假类型名称
     *
     * @param leaveType 请假类型代码
     * @return 请假类型名称
     */
    private String getLeaveTypeName(String leaveType) {
        return switch (leaveType) {
            case "SICK" -> "病假";
            case "PERSONAL" -> "事假";
            case "OFFICIAL" -> "公假";
            default -> "请假";
        };
    }

    /**
     * 生成请假条
     */
    @Transactional
    public void generateLeaveSlip(Long applicationId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));
        generateLeaveSlip(application);
    }

    /**
     * 生成请假条
     */
    @Transactional
    public void generateLeaveSlip(LeaveApplication application) {
        if (!"APPROVED".equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有已批准的申请才能生成请假条");
        }

        try {
            // 生成请假条PDF
            byte[] pdfBytes = templateService.generateLeaveSlip(application);

            // 上传到OSS
            // 创建MultipartFile对象
            MultipartFile pdfFile = new MultipartFile() {

                @Override
                @NotNull
                public String getName() {
                    return "leave-slip-" + application.getId() + ".pdf";
                }

                @Override
                public String getOriginalFilename() {
                    return "leave-slip-" + application.getId() + ".pdf";
                }

                @Override
                public String getContentType() {
                    return "application/pdf";
                }

                @Override
                public boolean isEmpty() {
                    return pdfBytes == null || pdfBytes.length == 0;
                }

                @Override
                public long getSize() {
                    return pdfBytes.length;
                }

                @Override
                public byte @NotNull [] getBytes() {
                    return pdfBytes;
                }

                @Override
                public @NotNull InputStream getInputStream() {
                    return new ByteArrayInputStream(pdfBytes);
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.write(dest.toPath(), pdfBytes);
                }
            };

            // 上传到OSS
            String fileUrl = ossService.uploadFile(pdfFile, "leave-slips");

            // 更新请假条状态和URL
            application.setLeaveSlipStatus("GENERATED");
            application.setLeaveSlipUrl(fileUrl);
            leaveApplicationRepository.save(application);

            log.info("请假条生成成功，URL: {}", fileUrl);
        } catch (Exception e) {
            log.error("生成请假条失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成请假条失败: " + e.getMessage());
        }
    }

    /**
     * 销假
     */
    @Transactional
    public void cancelLeave(Long applicationId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));

        if (!"APPROVED".equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有已批准的申请才能销假");
        }

        if (application.getCancelled()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已销假");
        }

        application.setCancelled(true);
        application.setCancelTime(LocalDateTime.now());
        leaveApplicationRepository.save(application);
    }

    /**
     * 获取申请详情
     */
    public LeaveApplication getApplicationById(Long applicationId) {
        return leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));
    }

    /**
     * 分页查询用户的请假申请
     */
    public PageResult<LeaveApplication> getUserApplications(Long userId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<LeaveApplication> page = leaveApplicationRepository
                .findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    public PageResult<LeaveApplication> getPendingApplications(Long approverId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        List<Long> applicationIds = approvalService.getPendingTasks(approverId).stream()
                .map(task -> task.getInstance().getBusinessId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Page<LeaveApplication> page;
        if (applicationIds.isEmpty()) {
            page = Page.empty(pageable);
        } else {
            page = leaveApplicationRepository.findAll(
                    (Specification<LeaveApplication>) (root, query, cb) -> root.get("id").in(applicationIds),
                    pageable
            );
        }

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }
}
