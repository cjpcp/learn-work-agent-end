package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import com.example.learnworkagent.domain.consultation.repository.ConsultationQuestionRepository;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferRepository;
import com.example.learnworkagent.infrastructure.external.dify.DifyChatService;
import com.example.learnworkagent.infrastructure.external.dify.SpeechToTextService;
import com.example.learnworkagent.infrastructure.service.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 咨询Agent服务.
 * <p>用于高频问题解答和多模态交互，提供异步处理问题和流式响应等功能.</p>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAgentService {

    private final ConsultationQuestionRepository consultationQuestionRepository;
    private final DifyChatService difyChatService;
    private final SpeechToTextService speechToTextService;
    private final CacheService cacheService;
    private final HumanTransferRepository humanTransferRepository;
    private final HumanTransferConfigService humanTransferConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注入自身的代理对象，用于解决事务自调用问题
    @Resource
    @Lazy
    private ConsultationAgentService self;

    /**
     * 从实体的 fileUrls JSON 字段中解析出 URL 列表
     */
    private List<String> resolveFileUrls(ConsultationQuestion question) {
        List<String> urls = new ArrayList<>();
        // 加附件文件 URL（图片、文档等均存于此）
        String fileUrlsJson = question.getFileUrls();
        if (fileUrlsJson != null && !fileUrlsJson.isBlank()) {
            try {
                List<String> parsed = objectMapper.readValue(fileUrlsJson, new TypeReference<>() {
                });
                if (parsed != null) urls.addAll(parsed);
            } catch (Exception e) {
                log.warn("解析 fileUrls JSON 失败，questionId: {}, json: {}", question.getId(), fileUrlsJson, e);
            }
        }
        return urls.isEmpty() ? null : urls;
    }

    /**
     * 异步处理问题
     */
    @Async
    public void processQuestionAsync(Long questionId) {
        try {
            ConsultationQuestion question = consultationQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "问题不存在"));
            log.info("开始处理问题，问题ID: {}", questionId);

            String cachedAnswer = getCachedAnswer(question);
            if (cachedAnswer != null) {
                self.updateQuestionAnswer(question, cachedAnswer, "AI");
                return;
            }

            List<String> fileUrls = resolveFileUrls(question);
            String query = buildQuery(question);
            String prompt = buildPrompt(question, query);
            log.info("调用Dify AI，问题ID: {}, 文件数: {}", questionId,
                    fileUrls != null ? fileUrls.size() : 0);

            question.setStatus("ANSWERING");
            question.setIsAnswering(true);
            consultationQuestionRepository.save(question);

            StringBuilder partialAnswer = new StringBuilder();
            difyChatService.chatStream(prompt, fileUrls, null, String.valueOf(question.getUserId()),
                    convId -> {
                        log.info("捕获到conversation_id: {}，保存到问题: {}", convId, questionId);
                        question.setConversationId(convId);
                        consultationQuestionRepository.save(question);
                    })
                    .doOnNext(partial -> {
                        partialAnswer.append(partial);
                    })
                    .collectList()
                    .subscribe(
                            answerList -> {
                                String answer = String.join("", answerList);
                                cacheAnswer(question, answer);
                                self.updateQuestionAnswer(question, answer, "AI");
                                question.setIsAnswering(false);
                                consultationQuestionRepository.save(question);
                            },
                            error -> {
                                log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                if (partialAnswer.length() > 0) {
                                    question.setAiAnswer(partialAnswer.toString());
                                }
                                question.setIsAnswering(false);
                                consultationQuestionRepository.save(question);
                                self.transferToHuman(question);
                            }
                    );

        } catch (Exception e) {
            log.error("处理问题失败，问题ID: {}", questionId, e);
        }
    }

    /**
     * 转人工（同时创建 HumanTransfer 记录）
     */
    @Transactional
    public void transferToHuman(ConsultationQuestion question) {
        question.setTransferredToHuman(true);
        question.setStatus("TRANSFERRED");
        question.setTransferReason("问题复杂或包含敏感关键词，需要人工处理");
        question.setAnswerSource("HUMAN");
        consultationQuestionRepository.save(question);

        HumanTransfer transfer = new HumanTransfer();
        transfer.setQuestionId(question.getId());
        transfer.setUserId(question.getUserId());
        transfer.setTransferType("AUTO");
        transfer.setTransferReason("问题复杂或包含敏感关键词，需要人工处理");

        Long matchedStaffId = humanTransferConfigService.resolveStaffId(question.getCategory());
        if (matchedStaffId != null) {
            transfer.setStaffId(matchedStaffId);
            transfer.setStatus("PROCESSING");
        } else {
            transfer.setStatus("PENDING");
        }
        humanTransferRepository.save(transfer);

        log.info("问题已转人工，问题ID: {}, HumanTransfer已创建", question.getId());
    }

    /**
     * 构建AI提示词
     *
     * @param question 咨询问题实体（用于获取分类等元数据）
     * @param query    经过语音转文字处理后的最终用户问题内容
     */
    private String buildPrompt(ConsultationQuestion question, String query) {
        return "你是一个学工智能助手，专门回答学生关于奖助勤贷、宿舍管理、违纪申诉、心理健康、就业指导等方面的问题。" +
                "\n\n问题分类：" + question.getCategory() +
                "\n问题内容：" + query +
                "\n\n请提供准确、详细的回答，并给出相关的流程指引。";
    }

    /**
     * 动态构造发送给Dify智能咨询服务的 query。
     * <ul>
     *   <li>仅有语音（无文本）：调用语音转文字，将结果直接作为 query。</li>
     *   <li>既有文本又有语音：将语音转文字结果拼接到用户文本之后，共同作为 query。</li>
     *   <li>仅有文本（无语音）：直接使用 questionText 作为 query，不调用语音服务。</li>
     * </ul>
     *
     * @param question 咨询问题实体
     * @return 最终 query 字符串
     */
    private String buildQuery(ConsultationQuestion question) {
        String textPart = question.getQuestionText();
        String voiceUrl = question.getVoiceUrl();
        String userId   = String.valueOf(question.getUserId());

        boolean hasText  = textPart != null && !textPart.isBlank();
        boolean hasVoice = voiceUrl  != null && !voiceUrl.isBlank();

        if (!hasText && !hasVoice) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "问题内容不能为空（文本和语音均为空）");
        }

        if (!hasVoice) {
            // 仅有文本，直接返回
            return textPart;
        }

        // 存在语音：调用语音转文字服务（同步），并做异常保护
        log.info("检测到语音URL，开始语音转文字，questionId: {}, voiceUrl: {}", question.getId(), voiceUrl);
        try {
            String speechText = speechToTextService.convertVoiceUrlToText(voiceUrl, userId);
            if (speechText == null || speechText.isBlank()) {
                log.warn("语音转文字结果为空，questionId: {}", question.getId());
                // 转文字结果为空时，退化为纯文本（若有）
                return hasText ? textPart : "";
            }
            if (hasText) {
                // 用户文本 + 语音转文字内容拼接
                String combined = textPart + "\n[语音补充内容]：" + speechText;
                log.info("文本与语音转文字内容已拼接，questionId: {}", question.getId());
                return combined;
            }
            // 仅有语音，直接使用转文字结果
            log.info("仅语音输入，使用语音转文字结果作为query，questionId: {}", question.getId());
            return speechText;
        } catch (BusinessException e) {
            // 语音转文字失败，降级处理：若有文本则继续用文本，否则向上抛出
            log.error("语音转文字失败，questionId: {}，尝试降级处理", question.getId(), e);
            if (hasText) {
                log.warn("语音转文字失败，降级使用用户文本，questionId: {}", question.getId());
                return textPart;
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "语音转文字失败且无文本内容，无法处理该问题");
        }
    }
    /**
     * 从缓存获取答案
     */
    private String getCachedAnswer(ConsultationQuestion question) {
        String cacheKey = "consultation:answer:" + question.getQuestionText().hashCode();
        Object cached = cacheService.get(cacheKey);
        return cached != null ? cached.toString() : null;
    }

    /**
     * 缓存答案
     */
    private void cacheAnswer(ConsultationQuestion question, String answer) {
        String cacheKey = "consultation:answer:" + question.getQuestionText().hashCode();
        cacheService.set(cacheKey, answer, 30, TimeUnit.MINUTES);
    }

    /**
     * 更新问题答案
     */
    @Transactional
    public void updateQuestionAnswer(ConsultationQuestion question, String answer, String source) {
        question.setAiAnswer(answer);
        question.setAnswerSource(source);
        question.setStatus("ANSWERED");
        question.setIsAnswering(false);
        consultationQuestionRepository.save(question);
    }


    /**
     * 流式处理问题
     *
     * @param questionId 问题id
     * @return 流式响应ai返回结果
     */
    public Flux<String> processQuestionStream(Long questionId) {
        log.info("开始流式处理问题，问题ID: {}", questionId);

        return Mono.fromCallable(() -> consultationQuestionRepository.findById(questionId)
                        .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "问题不存在")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(question -> {

                    String cachedAnswer = getCachedAnswer(question);
                    if (cachedAnswer != null) {
                        log.info("使用缓存答案，问题ID: {}", questionId);

                        Mono.fromRunnable(() -> self.updateQuestionAnswer(question, cachedAnswer, "AI"))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnError(error -> log.error("更新缓存答案失败，问题ID: {}", questionId, error))
                                .subscribe();

                        return Flux.just(cachedAnswer);
                    }

                    List<String> fileUrls = resolveFileUrls(question);
                    String query = buildQuery(question);
                    String prompt = buildPrompt(question, query);
                    log.info("调用Dify AI，问题ID: {}, 文件数: {}", questionId,
                            fileUrls != null ? fileUrls.size() : 0);

                    question.setStatus("ANSWERING");
                    question.setIsAnswering(true);
                    consultationQuestionRepository.save(question);

                    StringBuilder partialAnswer = new StringBuilder();

                    return difyChatService.chatStream(prompt, fileUrls, null,
                                    String.valueOf(question.getUserId()),
                                    convId -> {
                                        log.info("流式处理捕获到conversation_id: {}，保存到问题: {}", convId, questionId);
                                        question.setConversationId(convId);
                                        consultationQuestionRepository.save(question);
                                    })
                            .doOnNext(partial -> {
                                partialAnswer.append(partial);
                            })
                            .doOnComplete(() -> {
                                log.info("Dify API调用完成，问题ID: {}", questionId);
                                Mono.fromRunnable(() -> {
                                            question.setAiAnswer(partialAnswer.toString());
                                            question.setStatus("ANSWERED");
                                            question.setAnswerSource("AI");
                                            question.setIsAnswering(false);
                                            consultationQuestionRepository.save(question);
                                        })
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnError(error -> log.error("保存AI回答失败，问题ID: {}", questionId, error))
                                        .subscribe();
                            })
                            .doOnError(error -> {
                                log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                Mono.fromRunnable(() -> {
                                            if (partialAnswer.length() > 0) {
                                                question.setAiAnswer(partialAnswer.toString());
                                            }
                                            question.setIsAnswering(false);
                                            consultationQuestionRepository.save(question);
                                            self.transferToHuman(question);
                                        })
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnError(transferError -> log.error("转人工处理失败，问题ID: {}", questionId, transferError))
                                        .subscribe();
                            });
                });
    }

}
