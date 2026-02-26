package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
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
}
