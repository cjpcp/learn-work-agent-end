package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.consultation.dto.ConsultationRequest;
import com.example.learnworkagent.domain.consultation.dto.TransferToHumanRequest;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.service.ConsultationService;
import com.example.learnworkagent.domain.consultation.service.HumanTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * 咨询控制器
 */
@Slf4j
@Tag(name = "智能咨询", description = "智能咨询助手相关接口")
@RestController
@RequestMapping("/api/v1/consultation")
@RequiredArgsConstructor
public class ConsultationController extends BaseController {

    private final ConsultationService consultationService;
    private final HumanTransferService humanTransferService;

    @Operation(summary = "提交咨询问题")
    @PostMapping("/questions")
    public Result<ConsultationQuestion> submitQuestion(@Valid @RequestBody ConsultationRequest request) {
        // TODO: 从JWT中获取用户ID
        Long userId = getCurrentUserId();
        ConsultationQuestion question = consultationService.submitQuestion(
                userId,
                request.getQuestionText(),
                request.getQuestionType(),
                request.getCategory(),
                request.getImageUrl(),
                request.getVoiceUrl()
        );
        return Result.success(question);
    }

    /**
     * 获取问题详情
     *
     * @param id 问题id
     * @return 问题详情：用户ID，问题内容，问题类型，问题分类，图片URL，语音URL，AI回答，回答来源，是否已转人工，转人工原因，状态，满意度评分
     */
    @Operation(summary = "获取问题详情")
    @GetMapping("/questions/{id}")
    public Result<ConsultationQuestion> getQuestion(@PathVariable Long id) {
        ConsultationQuestion question = consultationService.getQuestionById(id);
        return Result.success(question);
    }

    /**
     * 分页查询我的问题
     *
     * @param pageRequest 分页参数：页码，每页数量
     * @return 分页查询结果
     */
    @Operation(summary = "分页查询我的问题")
    @GetMapping("/questions/my")
    public Result<PageResult<ConsultationQuestion>> getMyQuestions(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<ConsultationQuestion> result = consultationService.getUserQuestions(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 分页查询所有问题（管理员）
     *
     * @param pageRequest 分页参数：页码，每页数量
     * @param status      问题状态（PENDING-待回答, ANSWERED-已回答, TRANSFERRED-已转人工）
     * @param category    问题分类（AWARD-奖助勤贷, DORM-宿舍管理, DISCIPLINE-违纪申诉, MENTAL-心理健康, EMPLOYMENT-就业指导）
     * @return 分页查询结果
     */
    @Operation(summary = "分页查询所有问题（管理员）")
    @GetMapping("/questions")
    public Result<PageResult<ConsultationQuestion>> getAllQuestions(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        PageResult<ConsultationQuestion> result = consultationService.getAllQuestions(pageRequest, status, category);
        return Result.success(result);
    }

    /**
     * 评价问题回答
     *
     * @param id                问题id
     * @param satisfactionScore 满意度评分
     * @return 评价结果
     */
    @Operation(summary = "评价问题回答")
    @PostMapping("/questions/{id}/rate")
    public Result<Void> rateQuestion(@PathVariable Long id, @RequestParam Integer satisfactionScore) {
        consultationService.rateQuestion(id, satisfactionScore);
        return Result.success();
    }

    /**
     * 申请转人工
     *
     * @param id      问题id
     * @param request 转人工请求
     * @return 转人工结果
     */
    @Operation(summary = "申请转人工")
    @PostMapping("/questions/{id}/transfer")
    public Result<Void> transferToHuman(@PathVariable Long id, @RequestBody TransferToHumanRequest request) {
        Long userId = getCurrentUserId();
        humanTransferService.createTransfer(id, userId, "MANUAL", request.getReason());
        return Result.success();
    }

    /**
     * 提交咨询问题（流式响应）
     *
     * @param request 咨询请求封装：问题内容，问题类型，问题分类，图片URL，语音URL
     * @return 流式响应
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

        //注册emitter
        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onCompletion(() -> log.info("SSE连接完成，userId: {}", userId));

        emitter.onError((error) -> log.error("SSE连接错误，userId: {}", userId, error));

        //超时处理
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
                request.getImageUrl(),
                request.getVoiceUrl()
        );

        log.info("准备订阅Flux，userId: {}", userId);

        responseFlux.publishOn(Schedulers.boundedElastic()).subscribe(
                chunk -> {
                    log.info("接收到chunk，userId: {}, chunk: {}", userId, chunk);
                    try {
                        log.debug("发送SSE数据，userId: {}, chunk长度: {}, chunk内容: {}", userId, chunk.length(), chunk);
                        emitter.send(SseEmitter.event().data(chunk));
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

    //todo 转人工后的人工操作
}
