package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.award.dto.AwardApplicationRequest;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.service.AwardApplicationService;
import com.example.learnworkagent.domain.leave.dto.ApprovalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 奖助管理控制器.
 * <p>提供奖助学金的申请、查询和审批接口.</p>
 *
 * @author system
 * @see AwardApplicationService
 */
@Slf4j
@Tag(name = "奖助管理", description = "奖助申请、审批等相关接口")
@RestController
@RequestMapping("/api/v1/award")
@RequiredArgsConstructor
public class AwardController extends BaseController {

    private final AwardApplicationService awardApplicationService;

    /**
     * 提交奖助申请.
     *
     * @param request 奖助申请请求参数，包含申请类型、奖助名称、金额、申请理由、附件URL列表等
     * @return 创建的奖助申请记录
     */
    @Operation(summary = "提交奖助申请")
    @PostMapping("/applications")
    public Result<AwardApplication> submitApplication(@Valid @RequestBody AwardApplicationRequest request) {
        Long userId = getCurrentUserId();
        AwardApplication application = awardApplicationService.submitAwardApplication(userId, request);
        return Result.success(application);
    }

    /**
     * 获取奖助申请详情.
     *
     * @param id 奖助申请记录ID
     * @return 奖助申请详情
     */
    @Operation(summary = "获取申请详情")
    @GetMapping("/applications/{id}")
    public Result<AwardApplication> getApplication(@PathVariable Long id) {
        AwardApplication application = awardApplicationService.getApplicationById(id);
        return Result.success(application);
    }

    /**
     * 分页查询当前用户的奖助申请记录.
     *
     * @param pageRequest 分页参数（页码、每页大小、排序字段、排序方向）
     * @return 分页后的申请列表
     */
    @Operation(summary = "分页查询我的奖助申请")
    @GetMapping("/applications/my")
    public Result<PageResult<AwardApplication>> getMyApplications(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        log.info("用户id:{}", userId);
        PageResult<AwardApplication> result = awardApplicationService.getUserApplications(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 分页查询待审批的奖助申请（审批人视图）.
     *
     * @param pageRequest 分页参数（页码、每页大小、排序字段、排序方向）
     * @return 分页后的待审批申请列表
     */
    @Operation(summary = "分页查询待审批的申请（审批人）")
    @GetMapping("/applications/pending")
    public Result<PageResult<AwardApplication>> getPendingApplications(@Valid PageRequest pageRequest) {
        Long approverId = getCurrentUserId();
        PageResult<AwardApplication> result = awardApplicationService.getPendingApplications(approverId, pageRequest);
        return Result.success(result);
    }

    /**
     * 审批指定的奖助申请.
     *
     * @param id      奖助申请记录ID
     * @param request 审批请求参数，包含审批状态（通过/拒绝）和审批意见
     * @return 操作结果
     */
    @Operation(summary = "审批奖助申请")
    @PostMapping("/applications/{id}/approve")
    public Result<Void> approveApplication(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        awardApplicationService.approveAwardApplication(id, approverId, request.getApprovalStatus(), request.getApprovalComment());
        return Result.success();
    }

    @Operation(summary = "撤销奖助申请")
    @DeleteMapping("/applications/{id}")
    public Result<Void> cancelApplication(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        awardApplicationService.cancelAwardApplication(id, userId);
        return Result.success();
    }
} 
