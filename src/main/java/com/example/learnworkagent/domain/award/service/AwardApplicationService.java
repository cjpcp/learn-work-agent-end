package com.example.learnworkagent.domain.award.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.MaterialStatusEnum;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private static final String MATERIAL_COMMENT_PASSED = "材料完整，通过预审";
    private static final Duration MATERIAL_CHECK_TIMEOUT = Duration.ofMinutes(2);

    private final AwardApplicationRepository awardApplicationRepository;
    private final DifyWorkflowService difyWorkflowService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;
    @Lazy
    @Autowired
    private AwardApplicationService self;


    @Transactional
    public AwardApplication submitAwardApplication(Long applicantId, AwardApplicationRequest request) {
        log.info("提交申请，用户ID: {}, 申请类型: {}, 奖项名称: {}, 金额: {}, 理由: {}",
                applicantId, request.getApplicationType(), request.getAwardName(), request.getAmount(), request.getReason());

        AwardApplication application = buildAwardApplication(applicantId, request);
        AwardApplication savedApplication = awardApplicationRepository.save(application);
        Long applicationId = savedApplication.getId();
        createApprovalFlow(savedApplication, applicantId, request);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.preCheckMaterialsAsync(applicationId);
            }
        });

        return savedApplication;
    }

    @Async
    @Transactional
    public void preCheckMaterialsAsync(Long applicationId) {
        log.info("异步材料预审开始，申请ID: {}", applicationId);
        try {
            AwardApplication application = getApplicationById(applicationId);
            if (application == null) {
                log.error("异步材料预审失败：申请不存在，申请ID: {}", applicationId);
                return;
            }
            log.info("异步材料预审，获取到申请: {}, 材料状态: {}", application.getId(), application.getMaterialStatus());
            Map<String, Object> result = checkMaterialsComplete(application);
            boolean materialsComplete = (boolean) result.get("pass");
            String text = result.get("reason").toString();
            log.info("异步材料预审，Dify返回结果: pass={}, reason={}", materialsComplete, text);
            applyMaterialReviewResult(application, materialsComplete, text);
            awardApplicationRepository.save(application);
            if (!materialsComplete) {
                approvalService.rejectPendingTasks(BUSINESS_TYPE_AWARD, applicationId, text);
            }
            log.info("异步材料预审完成，申请ID: {}, 新状态: {}", applicationId, application.getMaterialStatus());
        } catch (Exception exception) {
            log.error("材料预审失败，申请ID: {}", applicationId, exception);
        }
    }

    @Transactional
    public void approveAwardApplication(Long applicationId, Long approverId, String approvalStatus, String approvalComment) {
        AwardApplication application = getApplicationById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND);
        }
        if (!MaterialStatusEnum.PASSED.equals(application.getMaterialStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "材料预审未通过，无法审批");
        }

        ApprovalInstance approvalInstance = approvalService.getApprovalInstance(BUSINESS_TYPE_AWARD, applicationId);
        ApprovalTask currentTask = getCurrentTask(approvalInstance, approverId);
        approvalService.processApprovalTask(currentTask.getId(), approverId, approvalStatus, approvalComment);
    }

    public AwardApplication getApplicationById(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        return awardApplicationRepository.findById(applicationId)
                .orElse(null);
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

    private Map<String, Object> checkMaterialsComplete(AwardApplication application) {
        Map<String, Object> result = new HashMap<>();
        result.put("pass", false);
        result.put("reason", "");
        if (isBlank(application.getReason()) || isBlank(application.getAttachmentUrls())) {
            result.put("pass", false);
            result.put("reason", "请补充申请理由和附件");
            return result;
        }

        List<String> fileUrls = Stream.of(application.getAttachmentUrls().split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());
        if (fileUrls.isEmpty()) {
            result.put("pass", false);
            result.put("reason", "请补充附件");
            return result;
        }

        try {
            result = difyWorkflowService.identifyDocuments(fileUrls, application.getApplicationType()).block(MATERIAL_CHECK_TIMEOUT);
            log.info("Dify工作流识别结果: {}", result);
            return result;
        } catch (Exception exception) {
            log.error("Dify工作流识别失败", exception);
        }
        return result;
    }


    private AwardApplication buildAwardApplication(Long applicantId, AwardApplicationRequest request) {
        AwardApplication application = new AwardApplication();
        application.setApplicantId(applicantId);
        application.setApplicationType(request.getApplicationType());
        application.setAwardName(request.getAwardName());
        application.setAmount(request.getAmount());
        application.setReason(request.getReason());
        application.setAttachmentUrls(request.getAttachmentUrls());
        application.setMaterialStatus(MaterialStatusEnum.PENDING);
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

    private void applyMaterialReviewResult(AwardApplication application, boolean materialsComplete, String text) {
        application.setMaterialStatus(materialsComplete ? MaterialStatusEnum.PASSED : MaterialStatusEnum.FAILED);
        application.setMaterialComment(materialsComplete ? MATERIAL_COMMENT_PASSED : text);
        application.setMaterialReviewTime(LocalDateTime.now());
        if (!materialsComplete) {
            application.setApprovalStatus(ApprovalStatusEnum.REJECTED.getCode());
            application.setApprovalComment("材料预审不通过：" + text);
        }
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
        if (application == null) {
            throw new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND);
        }
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤销待审批的申请");
        }
        approvalService.cancelApprovalInstance(BUSINESS_TYPE_AWARD, applicationId);
    }
}
