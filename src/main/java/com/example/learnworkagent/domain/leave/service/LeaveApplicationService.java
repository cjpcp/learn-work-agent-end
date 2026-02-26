package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
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

        // TODO: 发送审批结果通知（通过消息队列）
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
