package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.infrastructure.external.ai.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 测试控制器 - 用于测试各种服务
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "测试接口", description = "用于测试各种服务的接口")
public class TestController {

    private final OcrService ocrService;
    private final LeaveApplicationService leaveApplicationService;
    private final NotificationService notificationService;

    /**
     * 测试 OCR 服务 - 识别文档类型
     *
     * @param fileUrl 文件 URL
     * @return 文档类型
     */
    @Operation(summary = "测试 OCR 服务", description = "识别文档类型，支持成绩单、推荐信、家庭情况证明、收入证明等")
    @GetMapping("/ocr/identify-document")
    public Mono<Result<String>> testOcrService(@RequestParam String fileUrl) {
        log.info("测试 OCR 服务，文件 URL: {}", fileUrl);

        return ocrService.identifyDocumentType(fileUrl)
                .map(documentType -> {
                    log.info("OCR 识别结果: {}", documentType);
                    return Result.success("OCR 识别成功", documentType);
                })
                .onErrorResume(error -> {
                    log.error("OCR 测试失败", error);
                    return Mono.just(Result.fail("OCR 测试失败: " + error.getMessage()));
                });
    }

    /**
     * 测试 OCR 服务 - 检查文档类型
     *
     * @param fileUrl      文件 URL
     * @param expectedType 期望的文档类型
     * @return 是否为期望的文档类型
     */
    @Operation(summary = "测试 OCR 服务 - 检查文档类型", description = "检查文档是否为指定类型")
    @GetMapping("/ocr/check-document-type")
    public Mono<Result<Boolean>> testOcrCheckDocumentType(
            @RequestParam String fileUrl,
            @RequestParam String expectedType) {
        log.info("测试 OCR 服务 - 检查文档类型，文件 URL: {}, 期望类型: {}", fileUrl, expectedType);

        return ocrService.checkDocumentType(fileUrl, expectedType)
                .map(result -> {
                    log.info("OCR 检查结果: {}", result);
                    return Result.success("OCR 检查成功", result);
                })
                .onErrorResume(error -> {
                    log.error("OCR 检查失败", error);
                    return Mono.just(Result.fail("OCR 检查失败: " + error.getMessage()));
                });
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return Result.success("OK", "服务运行正常");
    }

    /**
     * 测试生成请假条
     *
     * @param applicationId 请假申请ID
     * @return 生成结果
     */
    @Operation(summary = "测试生成请假条", description = "生成请假条PDF并上传到OSS")
    @GetMapping("/leave/generate-slip/{applicationId}")
    public Result<String> testGenerateLeaveSlip(@PathVariable Long applicationId) {
        log.info("测试生成请假条，申请ID: {}", applicationId);
        
        try {
            leaveApplicationService.generateLeaveSlip(applicationId);
            return Result.success("请假条生成成功", "请假条已生成并上传到OSS");
        } catch (Exception e) {
            log.error("生成请假条失败", e);
            return Result.fail("生成请假条失败: " + e.getMessage());
        }
    }

    /**
     * 测试多渠道通知发送
     *
     * @param userId   用户ID
     * @param email    邮箱
     * @param channels 渠道列表（逗号分隔，如：SITE,EMAIL）
     * @return 发送结果
     */
    @Operation(summary = "测试多渠道通知发送", description = "测试站内信、邮件多渠道推送")
    @GetMapping("/notification/send")
    public Result<String> testNotificationSend(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "test@example.com") String email,
            @RequestParam(defaultValue = "SITE,EMAIL") String channels) {

        log.info("测试多渠道通知发送，用户ID: {}, 邮箱: {}, 渠道: {}",
                userId, email, channels);

        List<String> channelList = Arrays.asList(channels.split(","));

        NotificationMessage message = NotificationMessage.builder()
                .userId(userId)
                .email(email)
                .type("AWARD_APPROVAL")
                .title("奖助申请审批结果通知")
                .content("您的奖学金（国家励志奖学金）申请已通过。审批意见：材料齐全，符合申请条件。")
                .businessId(1L)
                .businessType("AWARD_APPLICATION")
                .channels(channelList)
                .applicantName("测试用户")
                .applicationType("SCHOLARSHIP")
                .awardName("国家励志奖学金")
                .amount(new BigDecimal("5000.00"))
                .approvalStatus("APPROVED")
                .approvalComment("材料齐全，符合申请条件")
                .approverName("审批员张三")
                .build();

        try {
            notificationService.sendAwardApprovalNotification(message);
            return Result.success("通知已发送到消息队列", 
                    String.format("渠道: %s，请查看日志确认发送结果", channels));
        } catch (Exception e) {
            log.error("通知发送失败", e);
            return Result.fail("通知发送失败: " + e.getMessage());
        }
    }

    /**
     * 测试审批通过通知
     *
     * @param userId 用户ID
     * @param email  邮箱
     * @return 发送结果
     */
    @Operation(summary = "测试审批通过通知", description = "模拟奖助申请审批通过的通知")
    @GetMapping("/notification/approved")
    public Result<String> testApprovedNotification(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "test@example.com") String email) {

        log.info("测试审批通过通知，用户ID: {}", userId);

        NotificationMessage message = NotificationMessage.builder()
                .userId(userId)
                .email(email)
                .type("AWARD_APPROVAL")
                .title("奖助申请审批结果通知")
                .content("您的奖学金（国家励志奖学金）申请已通过。审批意见：材料齐全，符合申请条件。")
                .businessId(1L)
                .businessType("AWARD_APPLICATION")
                .channels(Arrays.asList("SITE", "EMAIL"))
                .applicantName("测试用户")
                .applicationType("SCHOLARSHIP")
                .awardName("国家励志奖学金")
                .amount(new BigDecimal("5000.00"))
                .approvalStatus("APPROVED")
                .approvalComment("材料齐全，符合申请条件，同意批准")
                .approverName("学工处李老师")
                .build();

        try {
            notificationService.sendAwardApprovalNotification(message);
            return Result.success("审批通过通知已发送", "请查看日志确认发送结果");
        } catch (Exception e) {
            log.error("通知发送失败", e);
            return Result.fail("通知发送失败: " + e.getMessage());
        }
    }

    /**
     * 测试审批拒绝通知
     *
     * @param userId 用户ID
     * @param email  邮箱
     * @return 发送结果
     */
    @Operation(summary = "测试审批拒绝通知", description = "模拟奖助申请审批拒绝的通知")
    @GetMapping("/notification/rejected")
    public Result<String> testRejectedNotification(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "test@example.com") String email) {

        log.info("测试审批拒绝通知，用户ID: {}", userId);

        NotificationMessage message = NotificationMessage.builder()
                .userId(userId)
                .email(email)
                .type("AWARD_APPROVAL")
                .title("奖助申请审批结果通知")
                .content("您的助学金（国家助学金）申请未通过。审批意见：家庭经济情况证明材料不完整，请补充相关材料。")
                .businessId(2L)
                .businessType("AWARD_APPLICATION")
                .channels(Arrays.asList("SITE", "EMAIL"))
                .applicantName("测试用户")
                .applicationType("GRANT")
                .awardName("国家助学金")
                .amount(new BigDecimal("3000.00"))
                .approvalStatus("REJECTED")
                .approvalComment("家庭经济情况证明材料不完整，请补充相关材料后重新申请")
                .approverName("学工处王老师")
                .build();

        try {
            notificationService.sendAwardApprovalNotification(message);
            return Result.success("审批拒绝通知已发送", "请查看日志确认发送结果");
        } catch (Exception e) {
            log.error("通知发送失败", e);
            return Result.fail("通知发送失败: " + e.getMessage());
        }
    }
}
