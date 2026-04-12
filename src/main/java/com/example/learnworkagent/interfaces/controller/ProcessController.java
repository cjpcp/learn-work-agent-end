package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.service.ProcessService;
import com.example.learnworkagent.domain.user.entity.Admin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程记录控制器.
 * <p>提供流程待办和已完成的查询接口.</p>
 *
 * @author system
 * @see ProcessService
 */
@Tag(name = "流程记录", description = "流程待办和历史记录查询接口")
@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController extends BaseController {

    private final ProcessService processService;

    /**
     * 获取所有类型的待审批流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的待审批流程列表
     */
    @Operation(summary = "获取所有待审批流程")
    @GetMapping("/pending/all")
    public Result<PageResult<ProcessItem>> getPendingAll(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingAll(admin, pageRequest));
    }

    /**
     * 获取奖助类型的待审批流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的奖助待审批流程列表
     */
    @Operation(summary = "获取奖助待审批流程")
    @GetMapping("/pending/award")
    public Result<PageResult<ProcessItem>> getPendingAward(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingAward(admin, pageRequest));
    }

    /**
     * 获取请假类型的待审批流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的请假待审批流程列表
     */
    @Operation(summary = "获取请假待审批流程")
    @GetMapping("/pending/leave")
    public Result<PageResult<ProcessItem>> getPendingLeave(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingLeave(admin, pageRequest));
    }

    /**
     * 获取销假类型的待审批流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的销假待审批流程列表
     */
    @Operation(summary = "获取销假待审批流程")
    @GetMapping("/pending/leave-cancel")
    public Result<PageResult<ProcessItem>> getPendingLeaveCancel(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedLeaveCancel(admin, pageRequest));
    }

    /**
     * 获取所有类型的已完成流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的已完成流程列表
     */
    @Operation(summary = "获取所有已完成流程")
    @GetMapping("/completed/all")
    public Result<PageResult<ProcessItem>> getCompletedAll(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedAll(admin, pageRequest));
    }

    /**
     * 获取奖助类型的已完成流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的奖助已完成流程列表
     */
    @Operation(summary = "获取奖助已完成流程")
    @GetMapping("/completed/award")
    public Result<PageResult<ProcessItem>> getCompletedAward(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedAward(admin, pageRequest));
    }

    /**
     * 获取请假类型的已完成流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的请假已完成流程列表
     */
    @Operation(summary = "获取请假已完成流程")
    @GetMapping("/completed/leave")
    public Result<PageResult<ProcessItem>> getCompletedLeave(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedLeave(admin, pageRequest));
    }

    /**
     * 获取销假类型的已完成流程.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的销假已完成流程列表
     */
    @Operation(summary = "获取销假已完成流程")
    @GetMapping("/completed/leave-cancel")
    public Result<PageResult<ProcessItem>> getCompletedLeaveCancel(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingLeaveCancel(admin, pageRequest));
    }

    /**
     * 获取指定流程的详细信息.
     *
     * @param id   流程记录ID
     * @param type 流程类型（如leave、award等）
     * @return 流程详细信息
     */
    @Operation(summary = "获取流程详情")
    @GetMapping("/{id}")
    public Result<ProcessItem> getProcessDetail(@PathVariable String id, @RequestParam String type) {
        return Result.success(processService.getProcessDetail(id, type));
    }
}
