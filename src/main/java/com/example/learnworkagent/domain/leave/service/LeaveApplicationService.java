package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveSlipStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import com.example.learnworkagent.infrastructure.external.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 请假申请服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private static final String BUSINESS_TYPE_LEAVE = "LEAVE";
    private static final String SORT_FIELD_CREATE_TIME = "createTime";
    private static final String LEAVE_SLIP_DIRECTORY = "leave-slips";
    private static final String LEAVE_SLIP_FILE_PREFIX = "leave-slip-";
    private static final String LEAVE_SLIP_FILE_SUFFIX = ".docx";
    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TemplateService templateService;
    private final OssService ossService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    /**
     * 提交请假申请。
     *
     * @param applicantId 申请人ID
     * @param request 请假申请参数
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
     * 审批请假申请。
     *
     * @param applicationId 请假申请ID
     * @param approverId 审批人ID
     * @param approvalStatus 审批状态
     * @param approvalComment 审批意见
     */
    @Transactional
    public void approveLeaveApplication(Long applicationId, Long approverId,
                                        String approvalStatus, String approvalComment) {
        getApplicationById(applicationId);

        ApprovalInstance approvalInstance = approvalService.getApprovalInstance(BUSINESS_TYPE_LEAVE, applicationId);
        ApprovalTask currentTask = getCurrentTask(approvalInstance, approverId);
        approvalService.processApprovalTask(currentTask.getId(), approverId, approvalStatus, approvalComment);

        LeaveApplication refreshedApplication = getApplicationById(applicationId);
        if (refreshedApplication.isApproved()) {
            generateLeaveSlip(refreshedApplication);
        }
    }

    /**
     * 根据申请ID生成请假条。
     *
     * @param applicationId 请假申请ID
     */
    @Transactional
    public void generateLeaveSlip(Long applicationId) {
        generateLeaveSlip(getApplicationById(applicationId));
    }

    /**
     * 生成请假条。
     *
     * @param application 请假申请
     */
    @Transactional
    public void generateLeaveSlip(LeaveApplication application) {
        try {
            byte[] docBytes = templateService.generateLeaveSlip(application);
            MultipartFile docxFile = buildLeaveSlipFile(application, docBytes);
            String fileUrl = ossService.uploadFile(docxFile, LEAVE_SLIP_DIRECTORY);
            application.markLeaveSlipGenerated(fileUrl);
            leaveApplicationRepository.save(application);
            log.info("请假条生成成功，申请ID: {}, URL: {}", application.getId(), fileUrl);
        } catch (Exception exception) {
            log.error("生成请假条失败，申请ID: {}", application.getId(), exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成请假条失败: " + exception.getMessage());
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

        if (!application.isApproved()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有已批准的申请才能申请销假");
        }
        if (Boolean.TRUE.equals(application.getCancelled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已销假");
        }
        if (Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已提交销假申请");
        }

        application.setCancelRequested(true);
        application.setCancelApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        leaveApplicationRepository.save(application);
    }

    /**
     * 审批销假申请。
     *
     * @param applicationId 请假申请ID
     * @param approved 是否批准
     * @param comment 审批意见
     */
    @Transactional
    public void approveCancelRequest(Long applicationId, boolean approved, String comment) {
        LeaveApplication application = getApplicationById(applicationId);

        if (!Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请未提交销假申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getCancelApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "销假申请已审批");
        }

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
     * @param userId 用户ID
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
     * 分页查询待审批的请假申请。
     *
     * @param approverId 审批人ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getPendingApplications(Long approverId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        List<Long> applicationIds = approvalService.getPendingTasks(approverId).stream()
                .map(ApprovalTaskDTO::getBusinessId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        Page<LeaveApplication> page;
        if (applicationIds.isEmpty()) {
            page = Page.empty(pageable);
        } else {
            page = leaveApplicationRepository.findAll(buildPendingApplicationsSpecification(applicationIds), pageable);
        }

        return buildPageResult(page, pageRequest);
    }

    /**
     * 分页查询待审批销假申请。
     *
     * @param approverId 审批人ID（原请假审批人）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getPendingCancelRequests(Long approverId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<LeaveApplication> page = leaveApplicationRepository.findPendingCancelRequestsByApproverId(approverId, pageable);
        return buildPageResult(page, pageRequest);
    }

    private void validateLeaveDates(LeaveApplicationRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能早于今天");
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

    private ApprovalTask getCurrentTask(ApprovalInstance approvalInstance, Long approverId) {
        if (approvalInstance == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审批流程不存在");
        }

        return approvalService.getCurrentTasks(approvalInstance).stream()
                .filter(task -> Objects.equals(task.getApproverId(), approverId))
                .filter(task -> ApprovalStatusEnum.PROCESSING.getCode().equals(task.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "当前没有可处理的审批任务"));
    }

    private MultipartFile buildLeaveSlipFile(LeaveApplication application, byte[] docBytes) {
        final String fileName = LEAVE_SLIP_FILE_PREFIX + application.getId() + LEAVE_SLIP_FILE_SUFFIX;
        return new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return DOCX_CONTENT_TYPE;
            }

            @Override
            public boolean isEmpty() {
                return docBytes.length == 0;
            }

            @Override
            public long getSize() {
                return docBytes.length;
            }

            @Override
            public byte[] getBytes() {
                return docBytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(docBytes);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                Files.write(dest.toPath(), docBytes);
            }
        };
    }

    private Pageable buildPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATE_TIME)
        );
    }

    private PageResult<LeaveApplication> buildPageResult(Page<LeaveApplication> page, PageRequest pageRequest) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    private Specification<LeaveApplication> buildPendingApplicationsSpecification(List<Long> applicationIds) {
        return (root, query, criteriaBuilder) -> root.get("id").in(applicationIds);
    }
}
