package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import com.example.learnworkagent.infrastructure.external.template.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * 提交请假申请
     */
    @Transactional
    public LeaveApplication submitLeaveApplication(Long applicantId, String leaveType,
                                                   LocalDate startDate, LocalDate endDate,
                                                   String reason, String attachmentUrl) {
        // 验证日期
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能早于今天");
        }

        // 计算请假天数
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setLeaveType(leaveType);
        application.setStartDate(startDate);
        application.setEndDate(endDate);
        application.setDays((int) days);
        application.setReason(reason);
        application.setAttachmentUrl(attachmentUrl);
        application.setApprovalStatus("PENDING");

        return leaveApplicationRepository.save(application);
    }

    /**
     * 审批请假申请
     */
    @Transactional
    public void approveLeaveApplication(Long applicationId, Long approverId,
                                        String approvalStatus, String approvalComment) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));

        if (!"PENDING".equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已处理，无法重复审批");
        }

        application.setApproverId(approverId);
        application.setApprovalStatus(approvalStatus);
        application.setApprovalComment(approvalComment);
        application.setApprovalTime(LocalDateTime.now());

        leaveApplicationRepository.save(application);

        // 当审批通过时，自动生成请假条
        if ("APPROVED".equals(approvalStatus)) {
            generateLeaveSlip(applicationId);
        }

        // 发送审批结果通知（多渠道推送）
        sendApprovalNotification(application, approverId);
    }

    /**
     * 发送审批结果通知
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
                log.warn("申请人不存在，无法发送通知，用户ID: {}", application.getApplicantId());
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
                public String getName() {
                    return "leave-slip-" + applicationId + ".pdf";
                }

                @Override
                public String getOriginalFilename() {
                    return "leave-slip-" + applicationId + ".pdf";
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
                public byte[] getBytes() throws IOException {
                    return pdfBytes;
                }

                @Override
                public InputStream getInputStream() throws IOException {
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

    /**
     * 分页查询待审批的申请（审批人）
     */
    public PageResult<LeaveApplication> getPendingApplications(Long approverId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<LeaveApplication> page = leaveApplicationRepository
                .findByApprovalStatusAndApproverIdAndDeletedFalseOrderByCreateTimeDesc("PENDING", approverId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }
}
