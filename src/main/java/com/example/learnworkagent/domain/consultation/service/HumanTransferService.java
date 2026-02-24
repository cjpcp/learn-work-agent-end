package com.example.learnworkagent.domain.consultation.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.consultation.entity.ConsultationQuestion;
import com.example.learnworkagent.domain.consultation.entity.HumanTransfer;
import com.example.learnworkagent.domain.consultation.repository.ConsultationQuestionRepository;
import com.example.learnworkagent.domain.consultation.repository.HumanTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 人工转接服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HumanTransferService {

    private final HumanTransferRepository humanTransferRepository;
    private final ConsultationQuestionRepository consultationQuestionRepository;

    /**
     * 创建转接记录
     */
    @Transactional
    public void createTransfer(Long questionId, Long userId, String transferType, String reason) {
        ConsultationQuestion question = consultationQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "咨询问题不存在"));

        //新建转接记录
        HumanTransfer transfer = new HumanTransfer();
        transfer.setQuestionId(questionId);
        transfer.setUserId(userId);
        transfer.setTransferType(transferType);
        transfer.setTransferReason(reason);
        transfer.setStatus("PENDING");

        // 更新问题状态
        question.setTransferredToHuman(true);
        question.setStatus("TRANSFERRED");
        question.setTransferReason(reason);
        consultationQuestionRepository.save(question);

        humanTransferRepository.save(transfer);
    }

    /**
     * 分配工作人员
     */
    @Transactional
    public void assignStaff(Long transferId, Long staffId) {
        HumanTransfer transfer = humanTransferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "转接记录不存在"));

        transfer.setStaffId(staffId);
        transfer.setStatus("PROCESSING");
        humanTransferRepository.save(transfer);
    }

    /**
     * 工作人员回复
     */
    @Transactional
    public void reply(Long transferId, Long staffId, String reply) {
        HumanTransfer transfer = humanTransferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "转接记录不存在"));

        if (!transfer.getStaffId().equals(staffId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理此转接记录");
        }

        transfer.setStaffReply(reply);
        transfer.setStatus("COMPLETED");
        transfer.setProcessTime(LocalDateTime.now());
        humanTransferRepository.save(transfer);

        // 更新问题答案
        ConsultationQuestion question = consultationQuestionRepository.findById(transfer.getQuestionId())
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "咨询问题不存在"));
        question.setAiAnswer(reply);
        question.setAnswerSource("HUMAN");
        question.setStatus("ANSWERED");
        consultationQuestionRepository.save(question);
    }

    /**
     * 分页查询用户的转接记录
     */
    public PageResult<HumanTransfer> getUserTransfers(Long userId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<HumanTransfer> page = humanTransferRepository
                .findByUserIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    /**
     * 分页查询工作人员的转接记录
     */
    public PageResult<HumanTransfer> getStaffTransfers(Long staffId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<HumanTransfer> page = humanTransferRepository
                .findByStaffIdAndDeletedFalseOrderByCreateTimeDesc(staffId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }
}
