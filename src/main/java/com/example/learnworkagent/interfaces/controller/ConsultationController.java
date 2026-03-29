package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.consultation.dto.ConsultationRequest;
import com.example.learnworkagent.domain.consultation.dto.TransferToHumanRequest;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferRepository;
import com.example.learnworkagent.domain.consultation.service.ConsultationService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferService;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咋询控制器
 */
@Slf4j
@Tag(name = "智能咋询", description = "智能咋询助手相关接口")
@RestController
@RequestMapping("/api/v1/consultation")
@RequiredArgsConstructor
public class ConsultationController extends BaseController {

    private final ConsultationService consultationService;
    private final HumanTransferService humanTransferService;
    private final HumanTransferRepository humanTransferRepository;
    private final OssService ossService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "提交咋询问题")
    @PostMapping("/questions")
    public Result<ConsultationQuestion> submitQuestion(@Valid @RequestBody ConsultationRequest request) {
        Long userId = getCurrentUserId();
        ConsultationQuestion question = consultationService.submitQuestion(
                userId,
                request.getQuestionText(),
                request.getQuestionType(),
                request.getCategory(),
                request.getVoiceUrl(),
                request.getSessionId(),
                request.getFiles()
        );
        return Result.success(question);
    }

    @Operation(summary = "获取问题详情")
    @GetMapping("/questions/{id}")
    public Result<ConsultationQuestion> getQuestion(@PathVariable Long id) {
        ConsultationQuestion question = consultationService.getQuestionById(id);
        return Result.success(question);
    }

    @Operation(summary = "分页查询我的问题")
    @GetMapping("/questions/my")
    public Result<PageResult<ConsultationQuestion>> getMyQuestions(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<ConsultationQuestion> result = consultationService.getUserQuestions(userId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "分页查询所有问题（管理员）")
    @GetMapping("/questions")
    public Result<PageResult<ConsultationQuestion>> getAllQuestions(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        PageResult<ConsultationQuestion> result = consultationService.getAllQuestions(pageRequest, status, category);
        return Result.success(result);
    }

    @Operation(summary = "评价问题回答")
    @PostMapping("/questions/{id}/rate")
    public Result<Void> rateQuestion(@PathVariable Long id, @RequestParam Integer satisfactionScore) {
        consultationService.rateQuestion(id, satisfactionScore);
        return Result.success();
    }

    @Operation(summary = "申请转人工")
    @PostMapping("/questions/{id}/transfer")
    public Result<Void> transferToHuman(@PathVariable Long id, @RequestBody TransferToHumanRequest request) {
        Long userId = getCurrentUserId();
        humanTransferService.createTransfer(id, userId, "MANUAL", request.getReason());
        return Result.success();
    }

    @Operation(summary = "提交咋询问题（流式响应）")
    @PostMapping(value = "/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitQuestionStream(@Valid @RequestBody ConsultationRequest request) {
        final Long userId = getCurrentUserId();

        if (userId == null) {
            log.warn("SSE请求认证失败：用户未登录");
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event().data("错误: 用户未登录或认证失败"));
                errorEmitter.complete();
            } catch (IOException e) {
                log.error("发送认证错误失败", e);
                errorEmitter.completeWithError(e);
            }
            return errorEmitter;
        }

        log.info("收到流式请求，userId: {}, questionText: {}", userId, request.getQuestionText());

        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onCompletion(() -> log.info("SSE连接完成，userId: {}", userId));
        emitter.onError((error) -> log.error("SSE连接错误，userId: {}", userId, error));
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，userId: {}", userId);
            try {
                emitter.send(SseEmitter.event().data("错误: 请求超时"));
            } catch (IOException e) {
                log.error("发送超时错误失败", e);
            }
            emitter.complete();
        });

        Flux<String> responseFlux = consultationService.submitQuestionStream(
                userId,
                request.getQuestionText(),
                request.getQuestionType(),
                request.getCategory(),
                request.getVoiceUrl(),
                request.getSessionId(),
                request.getFiles()
        );

        log.info("准备订阅Flux，userId: {}", userId);

        responseFlux.publishOn(Schedulers.boundedElastic()).subscribe(
                chunk -> {
                    log.info("接收到chunk，userId: {}, chunk: {}", userId, chunk);
                    try {
                        Map<String, String> data = new HashMap<>();
                        data.put("answer", chunk);
                        String jsonData = objectMapper.writeValueAsString(data);
                        log.debug("发送SSE数据，userId: {}, chunk长度: {}", userId, chunk.length());
                        emitter.send(SseEmitter.event().data(jsonData));
                        log.info("成功发送SSE数据，userId: {}, chunk: {}", userId, chunk);
                    } catch (IOException e) {
                        log.error("发送SSE数据失败，userId: {}", userId, e);
                    }
                },
                error -> {
                    log.error("SSE流处理错误，userId: {}", userId, error);
                    try {
                        emitter.send(SseEmitter.event().data("错误: " + error.getMessage()));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("SSE流处理完成，userId: {}", userId);
                    emitter.complete();
                }
        );

        return emitter;
    }

    @Operation(summary = "上传咋询语音")
    @PostMapping(value = "/upload/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadVoice(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String voiceUrl = ossService.uploadConsultationFile(file, userId, "voice");
        return Result.success(voiceUrl);
    }

    @Operation(summary = "上传咋询文件")
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String fileUrl = ossService.uploadConsultationFile(file, userId, "file");
        return Result.success(fileUrl);
    }

    /**
     * 提交咋询问题（流式响应＋附件同步上传）
     */
    @Operation(summary = "提交咨询问题（流式响应+附件同步上传）")
    @PostMapping(value = "/questions/stream/multipart", produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter submitQuestionStreamWithFiles(
            @RequestParam("questionText") String questionText,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        final Long userId = getCurrentUserId();
        if (userId == null) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event().data("错误: 用户未登录"));
                errorEmitter.complete();
            } catch (IOException e) {
                errorEmitter.completeWithError(e);
            }
            return errorEmitter;
        }

        log.info("收到multipart流式请求, userId: {}, questionText: {}, 文件数: {}",
                userId, questionText, files != null ? files.length : 0);

        // 按 content-type 将语音单独提取为 voiceUrl，图片/文档放入 fileInputs

        String uploadedVoiceUrl = null;

        List<ConsultationRequest.FileInput> fileInputs = new java.util.ArrayList<>();

        if (files != null) {

            for (MultipartFile file : files) {

                if (file != null && !file.isEmpty()) {

                    try {

                        String ct = file.getContentType() != null ? file.getContentType() : "";

                        if (ct.startsWith("audio/")) {

                            uploadedVoiceUrl = ossService.uploadConsultationFile(file, userId, "voice");

                            log.info("语音上传成功: {}", uploadedVoiceUrl);

                        } else {

                            String url = ossService.uploadConsultationFile(file, userId, "file");

                            ConsultationRequest.FileInput fi = new ConsultationRequest.FileInput();

                            fi.setUrl(url);

                            fi.setTransferMethod("remote_url");

                            String lower = url.toLowerCase();

                            fi.setType(lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$") ? "image" : "document");

                            fileInputs.add(fi);

                            log.info("附件上传成功: {}", url);

                        }

                    } catch (Exception e) {

                        log.error("文件上传失败: {}", file.getOriginalFilename(), e);

                    }

                }

            }

        }


        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onCompletion(() -> log.info("multipart SSE 完成, userId: {}", userId));

        emitter.onError(e -> log.error("multipart SSE 错误, userId: {}", userId, e));

        emitter.onTimeout(() -> {

            try {

                emitter.send(SseEmitter.event().data("错误: 请求超时"));

            } catch (IOException e) {

                log.error("", e);

            }

            emitter.complete();

        });


        Flux<String> responseFlux = consultationService.submitQuestionStream(

                userId, questionText, "TEXT", null, uploadedVoiceUrl, sessionId,

                fileInputs.isEmpty() ? null : fileInputs);


        responseFlux.publishOn(Schedulers.boundedElastic()).subscribe(
                chunk -> {
                    try {
                        Map<String, String> data = new HashMap<>();
                        data.put("answer", chunk);
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
                    } catch (IOException e) {
                        log.error("发送SSE失败", e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event().data("错误: " + error.getMessage()));
                    } catch (IOException e) {
                        log.error("", e);
                    }
                    emitter.completeWithError(error);
                },
                emitter::complete
        );
        return emitter;
    }

    @Operation(summary = "分页查询用户的转人工记录")
    @GetMapping("/transfers")
    public Result<PageResult<HumanTransfer>> getUserTransfers(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getUserTransfers(userId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "获取转人工记录详情")
    @GetMapping("/transfers/{id}")
    public Result<Map<String, Object>> getTransferDetail(@PathVariable Long id) {
        HumanTransfer transfer = humanTransferRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "转人工记录不存在"));
        List<ConsultationQuestion> history = consultationService.getHistoryByUserIdUpToQuestion(
                transfer.getUserId(), transfer.getQuestionId());
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("transfer", transfer);
        result.put("history", history);
        return Result.success(result);
    }

    @Operation(summary = "分配工作人员")
    @PostMapping("/transfers/{id}/assign")
    public Result<Void> assignStaff(@PathVariable Long id, @RequestParam Long staffId) {
        humanTransferService.assignStaff(id, staffId);
        return Result.success();
    }

    @Operation(summary = "工作人员回复")
    @PostMapping("/transfers/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestParam String reply) {
        Long staffId = getCurrentUserId();
        humanTransferService.reply(id, staffId, reply);
        return Result.success();
    }

    @Operation(summary = "直接处理转接记录")
    @PostMapping("/transfers/{id}/process")
    public Result<Void> process(@PathVariable Long id, @RequestParam String reply) {
        Long staffId = getCurrentUserId();
        humanTransferService.process(id, staffId, reply);
        return Result.success();
    }

    @Operation(summary = "分页查询工作人员的转接记录")
    @GetMapping("/transfers/staff")
    public Result<PageResult<HumanTransfer>> getStaffTransfers(@Valid PageRequest pageRequest) {
        Long staffId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getStaffTransfers(staffId, pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "分页查询工作人员已完成的转接记录")
    @GetMapping("/transfers/completed")
    public Result<PageResult<HumanTransfer>> getCompletedTransfers(@Valid PageRequest pageRequest) {
        Long staffId = getCurrentUserId();
        PageResult<HumanTransfer> result = humanTransferService.getCompletedTransfers(staffId, pageRequest);
        return Result.success(result);
    }
}
