package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.repository.ConsultationQuestionRepository;
import com.example.learnworkagent.infrastructure.external.ai.QianwenApiClient;
import com.example.learnworkagent.infrastructure.service.CacheService;
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

import java.util.concurrent.CompletableFuture;

/**
 * 咨询Agent服务（用于高频问题解答和多模态交互）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAgentService {

    private final ConsultationQuestionRepository consultationQuestionRepository;
    private final QianwenApiClient qianwenApiClient;
    private final CacheService cacheService;

    // 注入自身的代理对象，用于解决事务自调用问题
    @Resource
    @Lazy
    private ConsultationAgentService self;

    /**
     * 异步处理问题
     */
    @Async
    public void processQuestionAsync(Long questionId) {
        try {
            //获取问题，如果问题不存在，则抛出异常
            ConsultationQuestion question = consultationQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "问题不存在"));
            log.info("开始处理问题，问题ID: {}", questionId);
            log.info("开始处理问题，问题内容: {}", question);

            // 检查是否需要转人工，如需要转人工，则将问题状态设置为转人工
            if (shouldTransferToHuman(question)) {
                self.transferToHuman(question);
                CompletableFuture.completedFuture(null);
                return;
            }

            // 尝试从缓存获取答案
            String cachedAnswer = getCachedAnswer(question);
            if (cachedAnswer != null) {
                self.updateQuestionAnswer(question, cachedAnswer, "AI");
                CompletableFuture.completedFuture(null);
                return;
            }

            // 根据问题类型调用相应的AI服务
            String prompt = buildPrompt(question);
            if ("IMAGE".equals(question.getQuestionType()) && question.getImageUrl() != null) {
                // 处理图片类型问题
                qianwenApiClient.generateMultimodal(prompt, question.getImageUrl())
                        .subscribe(
                                answer -> {
                                    // 缓存答案
                                    cacheAnswer(question, answer);
                                    // 更新问题答案
                                    self.updateQuestionAnswer(question, answer, "AI");
                                },
                                error -> {
                                    log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                    // AI服务失败，转人工
                                    self.transferToHuman(question);
                                }
                        );
            } else {
                // 处理文本或语音类型问题
                qianwenApiClient.generateText(prompt)
                        .subscribe(
                                answer -> {
                                    // 缓存答案
                                    cacheAnswer(question, answer);
                                    // 更新问题答案
                                    self.updateQuestionAnswer(question, answer, "AI");
                                },
                                error -> {
                                    log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                    // AI服务失败，转人工
                                    self.transferToHuman(question);
                                }
                        );
            }

            CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("处理问题失败，问题ID: {}", questionId, e);
            CompletableFuture.completedFuture(null);
        }
    }

    /**
     * todo需要对判断是否人工处理逻辑进行修改，不应该仅因为问题包含敏感关键词就转人工
     * 判断是否需要转人工
     */
    private boolean shouldTransferToHuman(ConsultationQuestion question) {
        // 检查是否包含敏感关键词（如：申诉、特殊情况等）
        String questionText = question.getQuestionText();
        if (questionText != null) {
            String lowerText = questionText.toLowerCase();
            return lowerText.contains("申诉") || lowerText.contains("特殊情况")
                    || lowerText.contains("投诉") || lowerText.contains("紧急");
        }
        return false;
    }

    /**
     * 转人工
     */
    @Transactional
    public void transferToHuman(ConsultationQuestion question) {
        question.setTransferredToHuman(true);
        question.setStatus("TRANSFERRED");
        question.setTransferReason("问题复杂或包含敏感关键词，需要人工处理");
        question.setAnswerSource("HUMAN");
        consultationQuestionRepository.save(question);
        log.info("问题已转人工，问题ID: {}", question.getId());
    }

    /**
     * 构建AI提示词
     */
    private String buildPrompt(ConsultationQuestion question) {

        return "你是一个学工智能助手，专门回答学生关于奖助勤贷、宿舍管理、违纪申诉、心理健康、就业指导等方面的问题。" +
                "\n\n问题分类：" + question.getCategory() +
                "\n问题内容：" + question.getQuestionText() +
                "\n\n请提供准确、详细的回答，并给出相关的流程指引。";
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
        cacheService.set(cacheKey, answer, 30, java.util.concurrent.TimeUnit.MINUTES);
    }

    /**
     * 更新问题答案
     */
    @Transactional
    public void updateQuestionAnswer(ConsultationQuestion question, String answer, String source) {
        question.setAiAnswer(answer);
        question.setAnswerSource(source);
        question.setStatus("ANSWERED");
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

        // 将阻塞的数据库查询包装在异步操作中
        return Mono.fromCallable(() -> consultationQuestionRepository.findById(questionId)
                        .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "问题不存在")))
                .subscribeOn(Schedulers.boundedElastic())  // 使用专门的阻塞线程池
                .flatMapMany(question -> {

                    //检查问题是否转人工
                    if (shouldTransferToHuman(question)) {
                        log.info("问题需要转人工，问题ID: {}", questionId);

                        //使用专门的线程池来处理阻塞任务
                        Mono.fromRunnable(() -> self.transferToHuman(question))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnError(error -> log.error("问题需要转人工失败，问题ID: {}", questionId, error))
                                .subscribe();

                        return Flux.just("抱歉，这个问题需要人工处理，已为您转接到人工客服。");
                    }

                    //检查缓存中是否有答案或需要更新答案
                    String cachedAnswer = getCachedAnswer(question);
                    if (cachedAnswer != null) {
                        log.info("使用缓存答案，问题ID: {}", questionId);

                        //使用专门的线程池来处理阻塞任务
                        Mono.fromRunnable(() -> self.updateQuestionAnswer(question, cachedAnswer, "AI"))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnError(error -> log.error("更新缓存答案失败，问题ID: {}", questionId, error))
                                .subscribe();

                        return Flux.just(cachedAnswer);
                    }

                    //咨询ai返回结果
                    String prompt = buildPrompt(question);
                    log.info("调用千问AI，问题ID: {}, prompt: {}", questionId, prompt);
                    
                    if ("IMAGE".equals(question.getQuestionType()) && question.getImageUrl() != null) {
                        // 处理图片类型问题
                        return qianwenApiClient.generateMultimodalStream(prompt, question.getImageUrl())
                                //所有数据发送完成时的回调函数
                                .doOnComplete(() -> {
                                    log.info("千问多模态API调用完成，问题ID: {}", questionId);
                                    //使用专门的线程池来处理阻塞任务
                                    Mono.fromRunnable(() -> {
                                                question.setStatus("ANSWERED");
                                                question.setAnswerSource("AI");
                                                //保存到数据库可能陷入阻塞
                                                consultationQuestionRepository.save(question);
                                            })
                                            .subscribeOn(Schedulers.boundedElastic())  // 使用专门的阻塞线程池
                                            .doOnError(error -> log.error("保存AI回答失败，问题ID: {}", questionId, error))
                                            .subscribe();
                                })
                                //当发送过程中发生错误时的回调函数
                                .doOnError(error -> {
                                    log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                    // 保存到数据库可能陷入阻塞
                                    Mono.fromRunnable(() -> self.transferToHuman(question))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .doOnError(transferError -> log.error("转人工处理失败，问题ID: {}", questionId, transferError))
                                            .subscribe();
                                });
                    } else {
                        // 处理文本或语音类型问题
                        return qianwenApiClient.generateTextStream(prompt)
                                //所有数据发送完成时的回调函数
                                .doOnComplete(() -> {
                                    log.info("千问API调用完成，问题ID: {}", questionId);
                                    //使用专门的线程池来处理阻塞任务
                                    Mono.fromRunnable(() -> {
                                                question.setStatus("ANSWERED");
                                                question.setAnswerSource("AI");
                                                //保存到数据库可能陷入阻塞
                                                consultationQuestionRepository.save(question);
                                            })
                                            .subscribeOn(Schedulers.boundedElastic())  // 使用专门的阻塞线程池
                                            .doOnError(error -> log.error("保存AI回答失败，问题ID: {}", questionId, error))
                                            .subscribe();
                                })
                                //当发送过程中发生错误时的回调函数
                                .doOnError(error -> {
                                    log.error("AI服务调用失败，问题ID: {}", questionId, error);
                                    // 保存到数据库可能陷入阻塞
                                    Mono.fromRunnable(() -> self.transferToHuman(question))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .doOnError(transferError -> log.error("转人工处理失败，问题ID: {}", questionId, transferError))
                                            .subscribe();
                                });
                    }
                });
    }

//        原版本存在“在非阻塞上下文中使用阻塞调用可能会导致线程匮乏”问题
//        public Flux<String> processQuestionStream(Long questionId) {
//        log.info("开始流式处理问题，问题ID: {}", questionId);
//        //查找问题
//        ConsultationQuestion question = consultationQuestionRepository.findById(questionId)
//                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "问题不存在"));
//
//
//
//        if (shouldTransferToHuman(question)) {
//            log.info("问题需要转人工，问题ID: {}", questionId);
//
//            //使用专门的线程池来处理阻塞任务
//            Mono.fromRunnable(() -> self.transferToHuman(question))
//                    .subscribeOn(Schedulers.boundedElastic())
//                    .doOnError(error -> log.error("问题需要转人工失败，问题ID: {}", questionId, error))
//                    .subscribe();
//
//            return Flux.just("抱歉，这个问题需要人工处理，已为您转接到人工客服。");
//        }
//
//        String cachedAnswer = getCachedAnswer(question);
//        if (cachedAnswer != null) {
//            log.info("使用缓存答案，问题ID: {}", questionId);
//
//            //使用专门的线程池来处理阻塞任务
//            Mono.fromRunnable(() -> self.updateQuestionAnswer(question, cachedAnswer, "AI"))
//                    .subscribeOn(Schedulers.boundedElastic())
//                    .doOnError(error -> log.error("更新缓存答案失败，问题ID: {}", questionId, error))
//                    .subscribe();
//
//
//            return Flux.just(cachedAnswer);
//        }
//
//        String prompt = buildPrompt(question);
//        log.info("调用千问AI，问题ID: {}, prompt: {}", questionId, prompt);
//        return qianwenApiClient.generateTextStream(prompt)
//                //所有数据发送完成时的回调函数
//                .doOnComplete(() -> {
//                    log.info("千问API调用完成，问题ID: {}", questionId);
////                  //使用专门的线程池来处理阻塞任务
//                    Mono.fromRunnable(() -> {
//                                question.setStatus("ANSWERED");
//                                question.setAnswerSource("AI");
//                                //保存到数据库可能陷入阻塞
//                                consultationQuestionRepository.save(question);
//                            })
//                            .subscribeOn(Schedulers.boundedElastic())  // 使用专门的阻塞线程池
//                            .doOnError(error -> log.error("保存AI回答失败，问题ID: {}", questionId, error))
//                            .subscribe();
//                })
//                //当发送过程中发生错误时的回调函数
//                .doOnError(error -> {
//                    log.error("AI服务调用失败，问题ID: {}", questionId, error);
//                    // 保存到数据库可能陷入阻塞
//                    Mono.fromRunnable(() -> self.transferToHuman(question))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .doOnError(transferError -> log.error("转人工处理失败，问题ID: {}", questionId, transferError))
//                            .subscribe();
//                });
//    }
}
