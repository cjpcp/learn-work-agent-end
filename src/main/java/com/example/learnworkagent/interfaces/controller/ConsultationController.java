package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.consultation.dto.ConsultationRequest;
import com.example.learnworkagent.domain.consultation.dto.ConversationMessageDTO;
import com.example.learnworkagent.domain.consultation.dto.TransferToHumanRequest;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.service.ConsultationService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferService;
import com.example.learnworkagent.infrastructure.external.oss.OssService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咨询控制器.
 * <p>提供智能咨询的问答、文件上传、转人工等接口.</p>
 *
 * @author system
 * @see ConsultationService
 */
@Slf4j
@Tag(name = "智能咨询", description = "智能咨询助手相关接口")
@RestController
@RequestMapping("/api/v1/consultation")
@RequiredArgsConstructor
public class ConsultationController extends BaseController {

    private final ConsultationService consultationService;
    private final HumanTransferService humanTransferService;
    private final OssService ossService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 提交咨询问题.
     *
     * @param request 咨询请求参数
     * @return 创建的咨询问题
     */
    @Operation(summary = "提交咨询问题")
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

    /**
     * 获取咨询问题详情.
     *
     * @param id 问题ID
     * @return 咨询问题详情
     */
    @Operation(summary = "获取问题详情")
    @GetMapping("/questions/{id}")
    public Result<ConsultationQuestion> getQuestion(@PathVariable Long id) {
        ConsultationQuestion question = consultationService.getQuestionById(id);
        return Result.success(question);
    }

    /**
     * 获取咨询问题的对话历史.
     *
     * @param id 问题ID
     * @return 对话消息列表
     */
    @Operation(summary = "获取问题对话历史")
    @GetMapping("/questions/{id}/history")
    public Result<List<ConversationMessageDTO>> getQuestionHistory(@PathVariable Long id) {
        List<ConversationMessageDTO> history = consultationService.getConversationHistory(id);
        return Result.success(history);
    }

    /**
     * 分页查询当前用户的咨询问题.
     *
     * @param pageRequest 分页请求参数
     * @return 分页后的咨询问题列表
     */
    @Operation(summary = "分页查询我的问题")
    @GetMapping("/questions/my")
    public Result<PageResult<ConsultationQuestion>> getMyQuestions(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<ConsultationQuestion> result = consultationService.getUserQuestions(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 申请转人工服务.
     *
     * @param id      问题ID
     * @param request 转人工请求参数
     * @return 操作结果
     */
    @Operation(summary = "申请转人工")
    @PostMapping("/questions/{id}/transfer")
    public Result<Void> transferToHuman(@PathVariable Long id, @RequestBody TransferToHumanRequest request) throws JsonProcessingException {
        Long userId = getCurrentUserId();
        humanTransferService.createTransfer(id, userId, "MANUAL", request.getReason(),
                request.getQuestionType(), request.getQuestionText(), request.getFiles());
        return Result.success();
    }

    /**
     * 提交咨询问题并以流式方式返回响应.
     * <p>使用SSE实现流式响应，超时时间120秒.</p>
     *
     * @param request 咨询请求参数
     * @return SSEEmitter用于推送流式响应
     */
    @Operation(summary = "提交咨询问题（流式响应）")
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
                        emitter.send(SseEmitter.event().data("{\"error\": \"" + error.getMessage() + "\"}"));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                    }
                    emitter.complete();
                },
                () -> {
                    log.info("SSE流处理完成，userId: {}", userId);
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 上传咨询语音文件.
     *
     * @param file 语音文件
     * @return 语音文件的OSS访问URL
     */
    @Operation(summary = "上传咨询语音")
    @PostMapping(value = "/upload/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadVoice(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String voiceUrl = ossService.uploadConsultationFile(file, userId, "voice");
        return Result.success(voiceUrl);
    }

    /**
     * 上传咨询附件文件.
     *
     * @param file 附件文件
     * @return 文件的OSS访问URL
     */
    @Operation(summary = "上传咨询文件")
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String fileUrl = ossService.uploadConsultationFile(file, userId, "file");
        return Result.success(fileUrl);
    }

    /**
     * 提交咨询问题（流式响应+附件同步上传）.
     * <p>支持同时上传语音和附件，使用SSE实现流式响应，超时时间120秒.</p>
     *
     * @param questionText 问题文本
     * @param sessionId    会话ID
     * @param files        附件列表（语音或文件）
     * @return SSEEmitter用于推送流式响应
     */
    @Operation(summary = "提交咨询问题（流式响应+附件同步上传）")
    @PostMapping(value = "/questions/stream/multipart", produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter submitQuestionStreamWithFiles(
            @RequestParam(value = "questionText", required = false, defaultValue = "") String questionText,
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
            log.warn("multipart SSE 超时, userId: {}", userId);
            emitter.complete();
        });

        String finalUploadedVoiceUrl = uploadedVoiceUrl;
        consultationService.createQuestion(userId, questionText, "TEXT", null, uploadedVoiceUrl, sessionId,
                fileInputs.isEmpty() ? null : fileInputs)
                .flatMapMany(saved -> {
                    final List<ConsultationRequest.FileInput> fileInputList = new java.util.ArrayList<>(fileInputs);
                    try {
                        Map<String, Object> initMessage = new HashMap<>();
                        initMessage.put("questionId", saved.getId());
                        initMessage.put("messageType", "init");
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(initMessage)));
                    } catch (IOException e) {
                        log.error("发送初始化消息失败", e);
                    }

                    Map<String, Object> userMessage = new HashMap<>();
                    userMessage.put("questionText", questionText);
                    List<Map<String, Object>> fileMaps = new java.util.ArrayList<>();
                    for (ConsultationRequest.FileInput fi : fileInputList) {
                        Map<String, Object> fm = new HashMap<>();
                        fm.put("url", fi.getUrl());
                        fm.put("type", fi.getType());
                        String fileName = extractFileNameFromUrl(fi.getUrl());
                        fm.put("name", fileName != null ? fileName : "附件");
                        fileMaps.add(fm);
                    }
                    if (finalUploadedVoiceUrl != null && !finalUploadedVoiceUrl.isBlank()) {
                        Map<String, Object> voiceFile = new HashMap<>();
                        voiceFile.put("url", finalUploadedVoiceUrl);
                        voiceFile.put("type", "audio");
                        voiceFile.put("name", "语音");
                        fileMaps.add(voiceFile);
                    }
                    userMessage.put("files", fileMaps.isEmpty() ? null : fileMaps);
                    userMessage.put("messageType", "user");
                    try {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(userMessage)));
                    } catch (IOException e) {
                        log.error("发送用户消息SSE失败", e);
                    }

                    return consultationService.processQuestionStreamById(saved.getId());
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
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
                    log.error("SSE流处理错误，userId: {}", userId, error);
                    try {
                        emitter.send(SseEmitter.event().data("{\"error\": \"" + error.getMessage() + "\"}"));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                    }
                    emitter.complete();
                },
                () -> {
                    log.info("SSE流处理完成，userId: {}", userId);
                    emitter.complete();
                }
        );

        return emitter;
    }

    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            String name = url.substring(lastSlash + 1);
            int questionIdx = name.indexOf('?');
            if (questionIdx > 0) {
                name = name.substring(0, questionIdx);
            }
            try {
                name = java.net.URLDecoder.decode(name, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            return name;
        }
        return "附件";
    }
}
