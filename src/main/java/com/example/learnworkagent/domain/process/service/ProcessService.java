package com.example.learnworkagent.domain.process.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.award.service.AwardApplicationService;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.user.entity.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程记录服务.
 * <p>提供待办和已完成流程的查询等业务逻辑.</p>
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
public class ProcessService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROCESS_STATUS_PENDING = "pending";
    private static final String PROCESS_STATUS_COMPLETED = "completed";
    private static final String PROCESS_TYPE_LEAVE = "leave";
    private static final String PROCESS_TYPE_AWARD = "award";
    private static final String PROCESS_TYPE_LEAVE_CANCEL = "leave_cancel";
    private static final String LEAVE_CANCEL_APPLICATION_NAME = "销假申请";
    private static final String LEAVE_APPROVAL_NAME = "请假审批";

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final LeaveApplicationService leaveApplicationService;
    private final AwardApplicationService awardApplicationService;


    public ProcessItem getProcessDetail(String id, String type) {
        if (PROCESS_TYPE_LEAVE.equals(type)) {
            return buildLeaveProcessDetail(id);
        }
        if (PROCESS_TYPE_AWARD.equals(type)) {
            return buildAwardProcessDetail(id);
        }
        if (PROCESS_TYPE_LEAVE_CANCEL.equals(type)) {
            return buildLeaveCancelProcessDetail(id);
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "未知的流程类型: " + type);
    }


    public PageResult<ProcessItem> getPendingAll(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> allPending = new ArrayList<>();

        boolean isStaff = isStaffRole(admin);

        if (isStudent(admin)) {
            allPending.addAll(leaveApplicationService.getPendingProcessItems(admin.getId(), false));
            allPending.addAll(awardApplicationService.getPendingProcessItems(admin.getId(), false));
        } else if (isStaff) {
            allPending.addAll(leaveApplicationService.getPendingProcessItems(admin.getId(), true));
            allPending.addAll(awardApplicationService.getPendingProcessItems(admin.getId(), true));
            allPending.addAll(leaveApplicationService.getPendingCancelProcessItems(admin.getId(), true));
        }

        return buildPageResult(allPending, pageRequest);
    }

    public PageResult<ProcessItem> getPendingAward(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingAward = new ArrayList<>();

        if (isStudent(admin)) {
            pendingAward.addAll(awardApplicationService.getPendingProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            pendingAward.addAll(awardApplicationService.getPendingProcessItems(admin.getId(), true));
        }

        return buildPageResult(pendingAward, pageRequest);
    }

    public PageResult<ProcessItem> getPendingLeave(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingLeave = new ArrayList<>();

        if (isStudent(admin)) {
            pendingLeave.addAll(leaveApplicationService.getPendingProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            pendingLeave.addAll(leaveApplicationService.getPendingProcessItems(admin.getId(), true));
        }

        return buildPageResult(pendingLeave, pageRequest);
    }

    public PageResult<ProcessItem> getPendingLeaveCancel(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingCancel = new ArrayList<>();

        if (isStudent(admin)) {
            pendingCancel.addAll(leaveApplicationService.getPendingCancelProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            pendingCancel.addAll(leaveApplicationService.getPendingCancelProcessItems(admin.getId(), true));
        }

        return buildPageResult(pendingCancel, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedAll(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> allCompleted = new ArrayList<>();

        boolean isStaff = isStaffRole(admin);

        if (isStudent(admin)) {
            allCompleted.addAll(leaveApplicationService.getCompletedProcessItems(admin.getId(), false));
            allCompleted.addAll(awardApplicationService.getCompletedProcessItems(admin.getId(), false));
            allCompleted.addAll(leaveApplicationService.getCompletedCancelProcessItems(admin.getId(), false));
        } else if (isStaff) {
            allCompleted.addAll(leaveApplicationService.getCompletedProcessItems(admin.getId(), true));
            allCompleted.addAll(awardApplicationService.getCompletedProcessItems(admin.getId(), true));
            allCompleted.addAll(leaveApplicationService.getCompletedCancelProcessItems(admin.getId(), true));
        }

        return buildPageResult(allCompleted, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedAward(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedAward = new ArrayList<>();

        if (isStudent(admin)) {
            completedAward.addAll(awardApplicationService.getCompletedProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            completedAward.addAll(awardApplicationService.getCompletedProcessItems(admin.getId(), true));
        }

        return buildPageResult(completedAward, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedLeave(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedLeave = new ArrayList<>();

        if (isStudent(admin)) {
            completedLeave.addAll(leaveApplicationService.getCompletedProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            completedLeave.addAll(leaveApplicationService.getCompletedProcessItems(admin.getId(), true));
        }

        return buildPageResult(completedLeave, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedLeaveCancel(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedCancel = new ArrayList<>();

        if (isStudent(admin)) {
            completedCancel.addAll(leaveApplicationService.getCompletedCancelProcessItems(admin.getId(), false));
        } else if (isStaffRole(admin)) {
            completedCancel.addAll(leaveApplicationService.getCompletedCancelProcessItems(admin.getId(), true));
        }

        return buildPageResult(completedCancel, pageRequest);
    }

    private PageResult<ProcessItem> buildPageResult(List<ProcessItem> allItems, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        int total = allItems.size();
        int start = (pageRequest.getPageNum() - 1) * pageRequest.getPageSize();
        int end = Math.min(start + pageRequest.getPageSize(), total);

        List<ProcessItem> pageItems = start < total ? allItems.subList(start, end) : new ArrayList<>();
        return new PageResult<>(pageItems, (long) total, pageRequest.getPageNum(), pageRequest.getPageSize());
    }


    private ProcessItem buildLeaveProcessDetail(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(Long.parseLong(id)).orElse(null);
        if (application == null) {
            return new ProcessItem();
        }
        return buildProcessItem(
                application.getId(), LEAVE_APPROVAL_NAME, PROCESS_TYPE_LEAVE,
                application.getCreateTime().format(DATE_TIME_FORMATTER),
                ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus()) ? PROCESS_STATUS_PENDING : PROCESS_STATUS_COMPLETED,
                "用户的请假申请"
        );
    }

    private ProcessItem buildAwardProcessDetail(String id) {
        AwardApplication application = awardApplicationRepository.findById(Long.parseLong(id)).orElse(null);
        if (application == null) {
            return new ProcessItem();
        }
        ProcessItem item = buildProcessItem(
                application.getId(), "奖助学金审批", PROCESS_TYPE_AWARD,
                application.getCreateTime().format(DATE_TIME_FORMATTER),
                application.getApprovalStatus(), "用户的奖助学金申请"
        );
        item.setComment(application.getApprovalComment());
        item.setMaterialStatus(application.getMaterialStatus() != null ? application.getMaterialStatus().getCode() : null);
        item.setMaterialComment(application.getMaterialComment());
        return item;
    }

    private ProcessItem buildLeaveCancelProcessDetail(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(Long.parseLong(id)).orElse(null);
        if (application == null) {
            return new ProcessItem();
        }
        String status = PROCESS_STATUS_PENDING;
        String description = "销假申请等待审批";
        if (application.getCancelApprovalStatus() != null) {
            if (ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus())) {
                status = PROCESS_STATUS_COMPLETED;
                description = "销假申请已批准";
            } else if (ApprovalStatusEnum.REJECTED.getCode().equals(application.getCancelApprovalStatus())) {
                status = PROCESS_STATUS_COMPLETED;
                description = "销假申请已拒绝";
            }
        }
        return buildProcessItem(
                application.getId(), LEAVE_CANCEL_APPLICATION_NAME, PROCESS_TYPE_LEAVE_CANCEL,
                application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                status,
                description
        );
    }

    private ProcessItem buildProcessItem(Long id, String name, String type, String createTime, String status, String description) {
        return buildProcessItem(id, name, type, createTime, status, description, null);
    }

    private ProcessItem buildProcessItem(Long id, String name, String type, String createTime, String status, String description, Boolean allowAction) {
        ProcessItem item = new ProcessItem();
        item.setId(String.valueOf(id));
        item.setName(name);
        item.setType(type);
        item.setCreateTime(createTime);
        item.setStatus(status);
        item.setDescription(description);
        item.setAllowAction(allowAction);
        return item;
    }


    private boolean isStudent(Admin admin) {
        return admin != null && admin.getTeacherId() != null && admin.getTeacherId() == 0;
    }

    private boolean isStaffRole(Admin admin) {
        return admin != null && admin.getTeacherId() != null && admin.getTeacherId() != 0;
    }

}
