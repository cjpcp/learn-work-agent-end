package com.example.learnworkagent.domain.award.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.infrastructure.external.ai.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 奖助申请服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardApplicationService {

    private final AwardApplicationRepository awardApplicationRepository;
    private final OcrService ocrService;

    /**
     * 提交奖助申请
     */
    @Transactional
    public AwardApplication submitAwardApplication(Long applicantId, String applicationType,
                                                   String awardName, java.math.BigDecimal amount,
                                                   String reason, List<String> attachmentUrls) {
        log.info("提交申请，用户ID: {}, 申请类型: {}, 奖项名称: {}, 金额: {}, 理由: {}",
                applicantId, applicationType, awardName, amount, reason);

        // 创建申请并保存
        AwardApplication application = new AwardApplication();
        application.setApplicantId(applicantId);
        application.setApplicationType(applicationType);
        application.setAwardName(awardName);
        application.setAmount(amount);
        application.setReason(reason);
        application.setAttachmentUrls(attachmentUrls != null ? String.join(",", attachmentUrls) : null);
        application.setMaterialStatus("PENDING");
        application.setApprovalStatus("PENDING");

        AwardApplication saved = awardApplicationRepository.save(application);

        // 异步进行材料预审
        preCheckMaterialsAsync(saved.getId());

        return saved;
    }

    /**
     * 材料预审意见
     *
     * @param applicationId 奖助申请id
     */
    @Async
    public void preCheckMaterialsAsync(Long applicationId) {
        try {
            AwardApplication application = awardApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND));

            // 检查材料完整性
            boolean materialsComplete = checkMaterialsComplete(application);

            //根据检查结果更新申请预审状态，预审意见和预审时间
            application.setMaterialStatus(materialsComplete ? "PASSED" : "FAILED");
            application.setMaterialComment(materialsComplete ? "材料完整，通过预审" : "材料不完整，请补充相关材料");
            application.setMaterialReviewTime(LocalDateTime.now());

            awardApplicationRepository.save(application);
        } catch (Exception e) {
            log.error("材料预审失败，申请ID: {}", applicationId, e);
        }
    }

    /**
     * 检查材料完整性
     *
     * @param application 具体的奖助学金申请
     * @return 材料是否完整
     */
    private boolean checkMaterialsComplete(AwardApplication application) {
        // 基础检查：必须有申请理由和附件
        if (application.getReason() == null || application.getReason().trim().isEmpty()) {
            return false;
        }

        if (application.getAttachmentUrls() == null || application.getAttachmentUrls().trim().isEmpty()) {
            return false;
        }

        // 根据申请类型检查必需材料
        String applicationType = application.getApplicationType();
        if ("SCHOLARSHIP".equals(applicationType)) {
            // 奖学金需要成绩单、推荐信等
            return checkScholarshipMaterials(application.getAttachmentUrls()); // todo简化处理，实际应该检查具体附件
        } else if ("GRANT".equals(applicationType) || "SUBSIDY".equals(applicationType)) {
            // todo助学金和困难补助需要家庭情况证明等
            return checkGrantMaterials(application.getAttachmentUrls()); // 简化处理
        }

        return true;
    }

    /**
     * 检查奖学金材料是否提供
     *
     * @param attachmentUrls 奖学金url
     * @return 是否提供
     */
    private boolean checkScholarshipMaterials(String attachmentUrls) {
        String[] urls = attachmentUrls.split(",");
        boolean hasTranscript = false;
        boolean hasRecommendation = false;

        for (String url : urls) {
            String trimmedUrl = url.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }

            try {
                // 每个 URL 只调用一次 OCR，根据识别结果判断是否满足条件
                String documentType = ocrService.identifyDocumentType(trimmedUrl)
                        .block(Duration.ofSeconds(15)); // 减少阻塞时间

                if ("成绩单".equals(documentType)) {
                    hasTranscript = true;
                    log.info("检测到成绩单: {}", trimmedUrl);
                } else if ("推荐信".equals(documentType)) {
                    hasRecommendation = true;
                    log.info("检测到推荐信: {}", trimmedUrl);
                }

                // 如果两种材料都找到了，提前结束循环
                if (hasTranscript && hasRecommendation) {
                    break;
                }
            } catch (Exception e) {
                log.error("OCR 识别失败: {}", trimmedUrl, e);
            }
        }

        return hasTranscript && hasRecommendation;
    }

    /**
     * 检查助学金材料是否提供
     *
     * @param attachmentUrls 助学金材料url
     * @return 是否提供
     */
    private boolean checkGrantMaterials(String attachmentUrls) {
        String[] urls = attachmentUrls.split(",");
        boolean hasFamilyProof = false;
        boolean hasIncomeProof = false;

        for (String url : urls) {
            String trimmedUrl = url.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }

            try {
                // 每个 URL 只调用一次 OCR，根据识别结果判断是否满足条件
                String documentType = ocrService.identifyDocumentType(trimmedUrl)
                        .block(Duration.ofSeconds(15)); // 减少阻塞时间

                if ("家庭情况证明".equals(documentType)) {
                    hasFamilyProof = true;
                    log.info("检测到家庭情况证明: {}", trimmedUrl);
                } else if ("收入证明".equals(documentType)) {
                    hasIncomeProof = true;
                    log.info("检测到收入证明: {}", trimmedUrl);
                }

                // 如果两种材料都找到了，提前结束循环
                if (hasFamilyProof && hasIncomeProof) {
                    break;
                }
            } catch (Exception e) {
                log.error("OCR 识别失败: {}", trimmedUrl, e);
            }
        }

        return hasFamilyProof && hasIncomeProof;
    }

    /**
     * 审批奖助申请
     *
     * @param applicationId   奖助申请id
     * @param approverId      审批人id
     * @param approvalStatus  审批状态
     * @param approvalComment 审批意见
     */
    @Transactional
    public void approveAwardApplication(Long applicationId, Long approverId,
                                        String approvalStatus, String approvalComment) {
        AwardApplication application = awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND));

        if (!"PENDING".equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已处理，无法重复审批");
        }

        if (!"PASSED".equals(application.getMaterialStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "材料预审未通过，无法审批");
        }

        application.setApproverId(approverId);
        application.setApprovalStatus(approvalStatus);
        application.setApprovalComment(approvalComment);
        application.setApprovalTime(LocalDateTime.now());

        awardApplicationRepository.save(application);

        // TODO: 发送审批结果通知（通过消息队列）
    }

    /**
     * 获取申请详情
     */
    public AwardApplication getApplicationById(Long applicationId) {
        return awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.AWARD_APPLICATION_NOT_FOUND));
    }

    /**
     * 分页查询用户的奖助申请
     *
     * @param userId      用户id
     * @param pageRequest 分页查询参数
     * @return 奖助申请分页查询结果
     */
    public PageResult<AwardApplication> getUserApplications(Long userId, PageRequest pageRequest) {
        //通过传入的分页参数构建Pageable对象
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        //根据userId和pageable进行分页查询
        Page<AwardApplication> page = awardApplicationRepository
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
    public PageResult<AwardApplication> getPendingApplications(Long approverId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        //根据审批状态和审批人进行分页查询
        Page<AwardApplication> page = awardApplicationRepository
                .findByApprovalStatusAndApproverIdAndDeletedFalseOrderByCreateTimeDesc("PENDING", approverId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }
}
