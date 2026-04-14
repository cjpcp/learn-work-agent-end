package com.example.learnworkagent.domain.award.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.dto.ApprovalTaskDTO;
import com.example.learnworkagent.domain.approval.entity.ApprovalInstance;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.award.dto.AwardApplicationRequest;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.infrastructure.external.dify.DifyWorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 奖助申请服务.
 * <p>提供奖助学金的申请、审批、查询等业务逻辑.</p>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardApplicationService {

    private static final String BUSINESS_TYPE_AWARD = NotificationBusinessTypeEnum.AWARD.getCode();
    private static final String SORT_FIELD_CREATE_TIME = "createTime";
    private static final String MATERIAL_STATUS_PENDING = "PENDING";
    private static final String MATERIAL_STATUS_PASSED = "PASSED";
    private static final String MATERIAL_STATUS_FAILED = "FAILED";
    private static final String MATERIAL_COMMENT_PASSED = "材料完整，通过预审";
    private static final String MATERIAL_COMMENT_FAILED = "材料不完整，请补充相关材料";
    private static final Duration MATERIAL_CHECK_TIMEOUT = Duration.ofSeconds(30);

    private final AwardApplicationRepository awardApplicationRepository;
    private final DifyWorkflowService difyWorkflowService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AwardApplication submitAwardApplication(Long applicantId, AwardApplicationRequest request) {
        log.info("提交申请，用户ID: {}, 申请类型: {}, 奖项名称: {}, 金额: {}, 理由: {}",
                applicantId, request.getApplicationType(), request.getAwardName(), request.getAmount(), request.getReason());

        AwardApplication application = buildAwardApplication(applicantId, request);
        AwardApplication savedApplication = awardApplicationRepository.save(application);
        preCheckMaterialsAsync(savedApplication.getId());
        createApprovalFlow(savedApplication, applicantId, request);
        return savedApplication;
    }

    @Async
    public void preCheckMaterialsAsync(Long applicationId) {
        try {
            AwardApplication application = getApplicationById(applicationId);
            boolean materialsComplete = checkMaterialsComplete(application);
            applyMaterialReviewResult(application, materialsComplete);
            awardApplicationRepository.save(application);
        } catch (Exception exception) {
            log.error("材料预审失败，申请ID: {}", applicationId, exception);
        }
    }

    @Transactional
    public void approveAwardApplication(Long applicationId, Long approverId, String approvalStatus, String approvalComment) {
        AwardApplication application = getApplicationById(applicationId);
        if (!MATERIAL_STATUS_PASSED.equals(application.getMaterialStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "材料预审未通过，无法审批");
        }

        ApprovalInstance approvalInstance = approvalService.getApprovalInstance(BUSINESS_TYPE_AWARD, applicationId);
        ApprovalTask currentTask = getCurrentTask(approvalInstance, approverId);
        approvalService.processApprovalTask(currentTask.getId(), approverId, approvalStatus, approvalComment);
    }

    public AwardApplication getApplicationById(Long applicationId) {
        return awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND));
    }

    public PageResult<AwardApplication> getUserApplications(Long userId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<AwardApplication> page = awardApplicationRepository.findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);
        return buildPageResult(page, pageRequest);
    }

    public PageResult<AwardApplication> getPendingApplications(Long approverId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        List<Long> applicationIds = approvalService.getPendingTasks(approverId).stream()
                .map(ApprovalTaskDTO::getBusinessId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        Page<AwardApplication> page = applicationIds.isEmpty()
                ? Page.empty(pageable)
                : awardApplicationRepository.findAll(buildPendingApplicationsSpecification(applicationIds), pageable);

        return buildPageResult(page, pageRequest);
    }

    private boolean checkMaterialsComplete(AwardApplication application) {
        if (isBlank(application.getReason()) || isBlank(application.getAttachmentUrls())) {
            return false;
        }

        List<String> fileUrls = Stream.of(application.getAttachmentUrls().split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        if (fileUrls.isEmpty()) {
            return false;
        }

        try {
            Map<String, Object> result = difyWorkflowService.identifyDocuments(fileUrls).block(MATERIAL_CHECK_TIMEOUT);
            log.info("Dify工作流识别结果: {}", result);
            return switch (application.getApplicationType()) {
                case "SCHOLARSHIP" -> checkScholarshipMaterials(result);
                case "GRANT", "SUBSIDY" -> checkGrantMaterials(result);
                default -> true;
            };
        } catch (Exception exception) {
            log.error("Dify工作流识别失败", exception);
            return false;
        }
    }

    private boolean checkScholarshipMaterials(Map<String, Object> result) {
        String output = readWorkflowOutput(result);
        boolean hasTranscript = output.contains("成绩单");
        boolean hasRecommendation = output.contains("推荐信");
        log.info("奖学金材料检查结果 - 成绩单: {}, 推荐信: {}", hasTranscript, hasRecommendation);
        return hasTranscript && hasRecommendation;
    }

    private boolean checkGrantMaterials(Map<String, Object> result) {
        String output = readWorkflowOutput(result);
        boolean hasFamilyProof = output.contains("家庭情况证明");
        boolean hasIncomeProof = output.contains("收入证明");
        log.info("助学金材料检查结果 - 家庭情况证明: {}, 收入证明: {}", hasFamilyProof, hasIncomeProof);
        return hasFamilyProof && hasIncomeProof;
    }

    private AwardApplication buildAwardApplication(Long applicantId, AwardApplicationRequest request) {
        AwardApplication application = new AwardApplication();
        application.setApplicantId(applicantId);
        application.setApplicationType(request.getApplicationType());
        application.setAwardName(request.getAwardName());
        application.setAmount(request.getAmount());
        application.setReason(request.getReason());
        application.setAttachmentUrls(request.getAttachmentUrls());
        application.setMaterialStatus(MATERIAL_STATUS_PENDING);
        application.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        application.setStudentName(request.getStudentName());
        application.setGrade(request.getGrade());
        application.setClassName(request.getClassName());
        return application;
    }

    private void createApprovalFlow(AwardApplication application, Long applicantId, AwardApplicationRequest request) {
        try {
            String applicantInfoJson = objectMapper.writeValueAsString(buildApplicantInfo(request));
            approvalService.createApprovalInstance(BUSINESS_TYPE_AWARD, application.getId(), applicantId, applicantInfoJson);
        } catch (Exception exception) {
            log.error("创建奖助审批流程失败，申请ID: {}", application.getId(), exception);
        }
    }

    private Map<String, Object> buildApplicantInfo(AwardApplicationRequest request) {
        Map<String, Object> applicantInfo = new HashMap<>(2);
        applicantInfo.put("studentName", request.getStudentName());
        applicantInfo.put("applicationType", request.getApplicationType());
        return applicantInfo;
    }

    private void applyMaterialReviewResult(AwardApplication application, boolean materialsComplete) {
        application.setMaterialStatus(materialsComplete ? MATERIAL_STATUS_PASSED : MATERIAL_STATUS_FAILED);
        application.setMaterialComment(materialsComplete ? MATERIAL_COMMENT_PASSED : MATERIAL_COMMENT_FAILED);
        application.setMaterialReviewTime(LocalDateTime.now());
    }

    private String readWorkflowOutput(Map<String, Object> result) {
        if (result == null) {
            return "";
        }
        Object output = result.get("output");
        return output == null ? "" : output.toString().toLowerCase();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Pageable buildPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATE_TIME)
        );
    }

    private PageResult<AwardApplication> buildPageResult(Page<AwardApplication> page, PageRequest pageRequest) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    private Specification<AwardApplication> buildPendingApplicationsSpecification(List<Long> applicationIds) {
        return (root, query, criteriaBuilder) -> root.get("id").in(applicationIds);
    }

    @Transactional
    public void cancelAwardApplication(Long applicationId, Long userId) {
        AwardApplication application = getApplicationById(applicationId);
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤销待审批的申请");
        }
        approvalService.cancelApprovalInstance(BUSINESS_TYPE_AWARD, applicationId);
    }
}
