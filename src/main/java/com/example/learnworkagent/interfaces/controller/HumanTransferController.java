package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferRepository;
import com.example.learnworkagent.domain.consultation.service.ConsultationService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferConfigService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferService;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人工转接控制器.
 * <p>提供人工转接的创建、查询、分配、处理等接口.</p>
 *
 * @author system
 * @see HumanTransferService
 */
@Slf4j
@Tag(name = "人工转接", description = "人工转接相关接口")
@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
public class HumanTransferController extends BaseController {

    private final HumanTransferService humanTransferService;
    private final HumanTransferConfigService humanTransferConfigService;
    private final HumanTransferRepository humanTransferRepository;
    private final ConsultationService consultationService;
    private final AdminRepository adminRepository;

    @Operation(summary = "根据问题申请转人工")
    @PostMapping("/questions/{questionId}")
    public Result<Void> transferToHuman(@PathVariable Long questionId, @RequestBody com.example.learnworkagent.domain.consultation.dto.TransferToHumanRequest request) {
        if (questionId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "问题ID不能为空");
        }
        Long userId = getCurrentUserId();
        try {
            humanTransferService.createTransfer(
                    questionId,
                    userId,
                    "MANUAL",
                    request.getReason(),
                    request.getQuestionType(),
                    request.getQuestionText(),
                    request.getFiles()
            );
            return Result.success();
        } catch (Exception e) {
            log.error("创建转人工记录失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建转人工记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "直接申请转人工（无关联问题）")
    @PostMapping
    public Result<Void> directTransferToHuman(@RequestBody com.example.learnworkagent.domain.consultation.dto.TransferToHumanRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转人工请求不能为空");
        }
        Long userId = getCurrentUserId();
        try {
            humanTransferService.createTransfer(
                    null,
                    userId,
                    "MANUAL",
                    request.getReason(),
                    request.getQuestionType(),
                    request.getQuestionText(),
                    request.getFiles()
            );
            return Result.success();
        } catch (Exception e) {
            log.error("创建转人工记录失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建转人工记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分页查询用户的转人工记录")
    @GetMapping("/my")
    public Result<PageResult<HumanTransfer>> getUserTransfers(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getUserTransfers(userId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "获取转人工记录详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTransferDetail(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转接记录ID不能为空");
        }
        HumanTransfer transfer = humanTransferRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "转人工记录不存在"));

        List<Map<String, Object>> historyWithFiles = new java.util.ArrayList<>();
        if (transfer.getQuestionId() != null) {
            List<ConsultationQuestion> history = consultationService.getHistoryByUserIdUpToQuestion(
                    transfer.getUserId(), transfer.getQuestionId());
            historyWithFiles = history.stream().map(q -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", q.getId());
                map.put("questionText", q.getQuestionText());
                map.put("answer", q.getAiAnswer());
                map.put("humanReply", q.getHumanReply());
                map.put("answerSource", q.getAnswerSource());
                map.put("createTime", q.getCreateTime());
                map.put("files", consultationService.parseFileUrls(q.getFileUrls()));
                return map;
            }).toList();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("transfer", transfer);
        result.put("history", historyWithFiles);
        return Result.success(result);
    }

    @Operation(summary = "分配工作人员")
    @PostMapping("/{id}/assign")
    public Result<Void> assignStaff(@PathVariable Long id, @RequestParam Long staffId) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转接记录ID不能为空");
        }
        humanTransferService.assignStaff(id, staffId);
        return Result.success();
    }

    @Operation(summary = "工作人员回复")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestParam String reply) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转接记录ID不能为空");
        }
        Long staffId = getCurrentUserId();
        humanTransferService.reply(id, staffId, reply);
        return Result.success();
    }

    @Operation(summary = "直接处理转接记录")
    @PostMapping("/{id}/process")
    public Result<Void> process(@PathVariable Long id, @RequestParam String reply) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转接记录ID不能为空");
        }
        Long staffId = getCurrentUserId();
        humanTransferService.process(id, staffId, reply);
        return Result.success();
    }

    @Operation(summary = "分页查询工作人员的转接记录")
    @GetMapping("/staff/pending")
    public Result<PageResult<HumanTransfer>> getStaffTransfers(@Valid PageRequest pageRequest) {
        Long staffId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getStaffTransfers(staffId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "分页查询工作人员已完成的转接记录")
    @GetMapping("/staff/completed")
    public Result<PageResult<HumanTransfer>> getCompletedTransfers(@Valid PageRequest pageRequest) {
        Long staffId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getCompletedTransfers(staffId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "检查当前用户是否有权限查看人工处理中心")
    @GetMapping("/permission")
    public Result<Boolean> checkTransferConfigPermission() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.success(false);
        }
        Admin admin = adminRepository.findById(userId).orElse(null);
        if (admin == null) {
            return Result.success(false);
        }
        boolean hasPermission = humanTransferConfigService.isCurrentUserInTransferConfig(userId, admin.getRoleId());
        return Result.success(hasPermission);
    }
}