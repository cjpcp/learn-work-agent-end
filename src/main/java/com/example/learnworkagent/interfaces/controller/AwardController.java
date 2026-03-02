package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.award.dto.AwardApplicationRequest;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.service.AwardApplicationService;
import com.example.learnworkagent.domain.leave.dto.ApprovalRequest;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖助管理控制器
 */
@Slf4j
@Tag(name = "奖助管理", description = "奖助申请、审批等相关接口")
@RestController
@RequestMapping("/api/v1/award")
@RequiredArgsConstructor
public class AwardController extends BaseController {

    private final AwardApplicationService awardApplicationService;
    private final OssService ossService;

    /**
     * 提交奖助申请
     *
     * @param request 申请类型，奖助名称，金额，申请理由，附件URL列表
     * @return 奖助申请实体：申请人ID，申请类型，申请名称，申请金额，材料预审状态，材料预审意见等
     */
    @Operation(summary = "提交奖助申请")
    @PostMapping("/applications")
    public Result<AwardApplication> submitApplication(@Valid @RequestBody AwardApplicationRequest request) {
        Long userId = getCurrentUserId();
        AwardApplication application = awardApplicationService.submitAwardApplication(
                userId,
                request.getApplicationType(),
                request.getAwardName(),
                request.getAmount(),
                request.getReason(),
                request.getAttachmentUrls()
        );
        return Result.success(application);
    }

    /**
     * 获取申请详情
     *
     * @param id 奖助申请id
     * @return 奖助申请详情
     */
    @Operation(summary = "获取申请详情")
    @GetMapping("/applications/{id}")
    public Result<AwardApplication> getApplication(@PathVariable Long id) {
        AwardApplication application = awardApplicationService.getApplicationById(id);
        return Result.success(application);
    }

    /**
     * 分页查询我的奖助申请
     *
     * @param pageRequest 分页参数：页码，每页大小，排序字段，排序方向
     * @return 分页后的申请列表
     */
    @Operation(summary = "分页查询我的奖助申请")
    @GetMapping("/applications/my")
    public Result<PageResult<AwardApplication>> getMyApplications(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        log.info("用户id:{}",userId);
        PageResult<AwardApplication> result = awardApplicationService.getUserApplications(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 分页查询待审批的申请（审批人）
     *
     * @param pageRequest 分页参数：页码，每页大小，排序字段，排序方向
     * @return 分页后的申请列表
     */
    @Operation(summary = "分页查询待审批的申请（审批人）")
    @GetMapping("/applications/pending")
    public Result<PageResult<AwardApplication>> getPendingApplications(@Valid PageRequest pageRequest) {
        Long approverId = getCurrentUserId();
        PageResult<AwardApplication> result = awardApplicationService.getPendingApplications(approverId, pageRequest);
        return Result.success(result);
    }

    /**
     * 审批奖助申请
     *
     * @param id      奖助申请id
     * @param request 审批状态，审批意见
     * @return 响应结果
     */
    @Operation(summary = "审批奖助申请")
    @PostMapping("/applications/{id}/approve")
    public Result<Void> approveApplication(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        awardApplicationService.approveAwardApplication(id, approverId, request.getApprovalStatus(), request.getApprovalComment());
        return Result.success();
    }

    /**
     * 上传奖助申请附件（支持单个或多个文件）
     *
     * @param files 上传的文件列表
     * @return 文件URL列表
     */
    @Operation(summary = "上传奖助申请附件", description = "支持单个或多个文件上传")
    @PostMapping("/applications/upload")
    public Result<List<String>> uploadAttachments(@RequestParam("files") MultipartFile[] files) {
        Long userId = getCurrentUserId();
        List<String> fileUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileUrl = ossService.uploadAwardFile(file, userId);
                fileUrls.add(fileUrl);
            }
        }
        
        return Result.success("文件上传成功", fileUrls);
    }
} 
