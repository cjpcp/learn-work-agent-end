package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.repository.ConsultationQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

/**
 * 咨询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationQuestionRepository consultationQuestionRepository;
    private final ConsultationAgentService consultationAgentService;

    /**
     * 提交咨询问题
     */
    @Transactional
    public ConsultationQuestion submitQuestion(Long userId, String questionText, String questionType,
                                               String category, String imageUrl, String voiceUrl) {
        ConsultationQuestion question = new ConsultationQuestion();
        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setCategory(category);
        question.setImageUrl(imageUrl);
        question.setVoiceUrl(voiceUrl);
        question.setStatus("PENDING");

        ConsultationQuestion saved = consultationQuestionRepository.save(question);

        // 异步调用Agent服务进行智能回答
        consultationAgentService.processQuestionAsync(saved.getId());

        return saved;
    }

    /**
     * 获取问题详情
     */
    public ConsultationQuestion getQuestionById(Long questionId) {
        return consultationQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "咨询问题不存在"));
    }

    /**
     * 分页查询用户的问题
     */
    public PageResult<ConsultationQuestion> getUserQuestions(Long userId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<ConsultationQuestion> page = consultationQuestionRepository
                .findByUserIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    /**
     * 分页查询所有问题（管理员）
     */
    public PageResult<ConsultationQuestion> getAllQuestions(PageRequest pageRequest, String status, String category) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<ConsultationQuestion> page;
        if (status != null && !status.isEmpty()) {
            page = consultationQuestionRepository.findByStatusAndDeletedFalseOrderByCreateTimeDesc(status, pageable);
        } else if (category != null && !category.isEmpty()) {
            page = consultationQuestionRepository.findByCategoryAndDeletedFalseOrderByCreateTimeDesc(category, pageable);
        } else {
            page = consultationQuestionRepository.findAll(pageable);
        }

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    /**
     * 评价问题回答
     */
    @Transactional
    public void rateQuestion(Long questionId, Integer satisfactionScore) {
        ConsultationQuestion question = getQuestionById(questionId);
        question.setSatisfactionScore(satisfactionScore);
        consultationQuestionRepository.save(question);
    }

    /**
     * 提交咨询问题（流式响应）
     */
    public Flux<String> submitQuestionStream(Long userId, String questionText, String questionType,
                                             String category, String imageUrl, String voiceUrl) {
        //保存问题
        ConsultationQuestion question = new ConsultationQuestion();
        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setCategory(category);
        question.setImageUrl(imageUrl);
        question.setVoiceUrl(voiceUrl);
        question.setStatus("PENDING");

        ConsultationQuestion saved = consultationQuestionRepository.save(question);

        Flux<String> res = consultationAgentService.processQuestionStream(saved.getId());
        log.info("res1:{}", res);
        return res;
    }
}
