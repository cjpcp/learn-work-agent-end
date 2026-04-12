package com.example.learnworkagent.domain.process.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.repository.ApprovalTaskRepository;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.user.entity.Admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private static final int PROCESS_PAGE_SIZE = 100;
    private static final String PROCESS_STATUS_PENDING = "pending";
    private static final String PROCESS_STATUS_COMPLETED = "completed";
    private static final String PROCESS_TYPE_LEAVE = "leave";
    private static final String PROCESS_TYPE_AWARD = "award";
    private static final String PROCESS_TYPE_LEAVE_CANCEL = "leave_cancel";
    private static final String LEAVE_APPLICATION_NAME = "请假申请";
    private static final String AWARD_APPLICATION_NAME = "奖助申请";
    private static final String LEAVE_CANCEL_APPLICATION_NAME = "销假申请";
    private static final String LEAVE_APPROVAL_NAME = "请假审批";
    private static final String AWARD_APPROVAL_NAME = "奖助审批";
    private static final String LEAVE_CANCEL_APPROVAL_NAME = "销假审批";

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final ApprovalTaskRepository approvalTaskRepository;


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

        if (isStudent(admin)) {
            var pendingLeaveApps = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                    admin.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : pendingLeaveApps.getContent()) {
                allPending.add(buildProcessItem(application.getId(), LEAVE_APPLICATION_NAME, PROCESS_TYPE_LEAVE,
                        application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_PENDING, "您的请假申请正在审批中"));
            }

            var pendingAwardApps = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                    admin.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (AwardApplication application : pendingAwardApps.getContent()) {
                allPending.add(buildProcessItem(application.getId(), AWARD_APPLICATION_NAME, PROCESS_TYPE_AWARD,
                        application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_PENDING, "您的奖助申请正在审批中"));
            }
        } else if (isStaffRole(admin)) {
            var pendingTasks = approvalTaskRepository.findByApproverIdAndStatus(admin.getId(), ApprovalStatusEnum.PROCESSING.getCode());
            for (ApprovalTask task : pendingTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                allPending.add(buildProcessItem(
                        task.getInstance().getBusinessId(),
                        leaveBusiness ? LEAVE_APPROVAL_NAME : AWARD_APPROVAL_NAME,
                        task.getInstance().getBusinessType().toLowerCase(),
                        task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_PENDING,
                        leaveBusiness ? "学生的请假申请需要您审批" : "学生的奖助申请需要您审批",
                        true
                ));
            }

            var pendingCancelApps = leaveApplicationRepository.findPendingCancelRequestsByApproverId(admin.getId(), PageRequest.of(0, PROCESS_PAGE_SIZE));
            for (LeaveApplication application : pendingCancelApps.getContent()) {
                allPending.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPROVAL_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_PENDING,
                        application.getStudentName() + "的销假申请需要您审批",
                        true
                ));
            }
        }

        return buildPageResult(allPending, pageRequest);
    }

    public PageResult<ProcessItem> getPendingAward(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingAward = new ArrayList<>();

        if (isStudent(admin)) {
            var pendingApps = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                    admin.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (AwardApplication application : pendingApps.getContent()) {
                pendingAward.add(buildProcessItem(application.getId(), AWARD_APPLICATION_NAME, PROCESS_TYPE_AWARD,
                        application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_PENDING, "您的奖助申请正在审批中"));
            }
        } else if (isStaffRole(admin)) {
            var pendingTasks = approvalTaskRepository.findByApproverIdAndStatus(admin.getId(), ApprovalStatusEnum.PROCESSING.getCode());
            for (ApprovalTask task : pendingTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                if (!leaveBusiness) {
                    pendingAward.add(buildProcessItem(
                            task.getInstance().getBusinessId(),
                            AWARD_APPROVAL_NAME,
                            PROCESS_TYPE_AWARD,
                            task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                            PROCESS_STATUS_PENDING,
                            "学生的奖助申请需要您审批",
                            true
                    ));
                }
            }
        }

        return buildPageResult(pendingAward, pageRequest);
    }

    public PageResult<ProcessItem> getPendingLeave(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingLeave = new ArrayList<>();

        if (isStudent(admin)) {
            var pendingApps = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                    admin.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : pendingApps.getContent()) {
                pendingLeave.add(buildProcessItem(application.getId(), LEAVE_APPLICATION_NAME, PROCESS_TYPE_LEAVE,
                        application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_PENDING, "您的请假申请正在审批中"));
            }
        } else if (isStaffRole(admin)) {
            var pendingTasks = approvalTaskRepository.findByApproverIdAndStatus(admin.getId(), ApprovalStatusEnum.PROCESSING.getCode());
            for (ApprovalTask task : pendingTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                if (leaveBusiness) {
                    pendingLeave.add(buildProcessItem(
                            task.getInstance().getBusinessId(),
                            LEAVE_APPROVAL_NAME,
                            PROCESS_TYPE_LEAVE,
                            task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                            PROCESS_STATUS_PENDING,
                            "学生的请假申请需要您审批",
                            true
                    ));
                }
            }
        }

        return buildPageResult(pendingLeave, pageRequest);
    }

    public PageResult<ProcessItem> getPendingLeaveCancel(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> pendingCancel = new ArrayList<>();

        if (isStudent(admin)) {
            var pendingCancelApps = leaveApplicationRepository.findPendingCancelRequestsByApplicantId(admin.getId(), PageRequest.of(0, PROCESS_PAGE_SIZE));
            for (LeaveApplication application : pendingCancelApps.getContent()) {
                pendingCancel.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_PENDING,
                        "您的销假申请等待审批"
                ));
            }
        } else if (isStaffRole(admin)) {
            var pendingCancelApps = leaveApplicationRepository.findPendingCancelRequestsByApproverId(admin.getId(), PageRequest.of(0, PROCESS_PAGE_SIZE));
            for (LeaveApplication application : pendingCancelApps.getContent()) {
                pendingCancel.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPROVAL_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_PENDING,
                        application.getStudentName() + "的销假申请需要您审批",
                        true
                ));
            }
        }

        return buildPageResult(pendingCancel, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedAll(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> allCompleted = new ArrayList<>();

        if (isStudent(admin)) {
            appendStudentCompletedLeaveProcesses(admin, allCompleted, ApprovalStatusEnum.APPROVED.getCode(), "您的请假申请已批准");
            appendStudentCompletedLeaveProcesses(admin, allCompleted, ApprovalStatusEnum.REJECTED.getCode(), "您的请假申请已拒绝");
            appendStudentCompletedAwardProcesses(admin, allCompleted, ApprovalStatusEnum.APPROVED.getCode(), "您的奖助申请已批准");
            appendStudentCompletedAwardProcesses(admin, allCompleted, ApprovalStatusEnum.REJECTED.getCode(), "您的奖助申请已拒绝");

            var completedCancelApps = leaveApplicationRepository.findByApplicantIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                    admin.getId(),
                    List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode()),
                    PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : completedCancelApps.getContent()) {
                String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus()) ? "已批准" : "已拒绝";
                allCompleted.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_COMPLETED,
                        "您的销假申请" + statusText
                ));
            }
        } else if (isStaffRole(admin)) {
            var completedTasks = approvalTaskRepository.findByApproverIdAndStatusIn(
                    admin.getId(), List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
            );
            for (ApprovalTask task : completedTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(task.getStatus()) ? "已批准" : "已拒绝";
                allCompleted.add(buildProcessItem(
                        task.getInstance().getBusinessId(),
                        leaveBusiness ? LEAVE_APPROVAL_NAME : AWARD_APPROVAL_NAME,
                        task.getInstance().getBusinessType().toLowerCase(),
                        task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_COMPLETED,
                        leaveBusiness ? "学生的请假申请您已" + statusText : "学生的奖助申请您已" + statusText
                ));
            }

            var completedCancelApps = leaveApplicationRepository.findByApproverIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                    admin.getId(),
                    List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode()),
                    PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : completedCancelApps.getContent()) {
                String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus()) ? "已批准" : "已拒绝";
                allCompleted.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPROVAL_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_COMPLETED,
                        application.getStudentName() + "的销假申请" + statusText
                ));
            }
        }

        return buildPageResult(allCompleted, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedAward(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedAward = new ArrayList<>();

        if (isStudent(admin)) {
            appendStudentCompletedAwardProcesses(admin, completedAward, ApprovalStatusEnum.APPROVED.getCode(), "您的奖助申请已批准");
            appendStudentCompletedAwardProcesses(admin, completedAward, ApprovalStatusEnum.REJECTED.getCode(), "您的奖助申请已拒绝");
        } else if (isStaffRole(admin)) {
            var completedTasks = approvalTaskRepository.findByApproverIdAndStatusIn(
                    admin.getId(), List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
            );
            for (ApprovalTask task : completedTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                if (!leaveBusiness) {
                    String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(task.getStatus()) ? "已批准" : "已拒绝";
                    completedAward.add(buildProcessItem(
                            task.getInstance().getBusinessId(),
                            AWARD_APPROVAL_NAME,
                            PROCESS_TYPE_AWARD,
                            task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                            PROCESS_STATUS_COMPLETED,
                            "学生的奖助申请您已" + statusText
                    ));
                }
            }
        }

        return buildPageResult(completedAward, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedLeave(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedLeave = new ArrayList<>();

        if (isStudent(admin)) {
            appendStudentCompletedLeaveProcesses(admin, completedLeave, ApprovalStatusEnum.APPROVED.getCode(), "您的请假申请已批准");
            appendStudentCompletedLeaveProcesses(admin, completedLeave, ApprovalStatusEnum.REJECTED.getCode(), "您的请假申请已拒绝");
        } else if (isStaffRole(admin)) {
            var completedTasks = approvalTaskRepository.findByApproverIdAndStatusIn(
                    admin.getId(), List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
            );
            for (ApprovalTask task : completedTasks) {
                boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
                if (leaveBusiness) {
                    String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(task.getStatus()) ? "已批准" : "已拒绝";
                    completedLeave.add(buildProcessItem(
                            task.getInstance().getBusinessId(),
                            LEAVE_APPROVAL_NAME,
                            PROCESS_TYPE_LEAVE,
                            task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                            PROCESS_STATUS_COMPLETED,
                            "学生的请假申请您已" + statusText
                    ));
                }
            }
        }

        return buildPageResult(completedLeave, pageRequest);
    }

    public PageResult<ProcessItem> getCompletedLeaveCancel(Admin admin, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        List<ProcessItem> completedCancel = new ArrayList<>();

        if (isStudent(admin)) {
            var completedCancelApps = leaveApplicationRepository.findByApplicantIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                    admin.getId(),
                    List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode()),
                    PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : completedCancelApps.getContent()) {
                String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus()) ? "已批准" : "已拒绝";
                completedCancel.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPLICATION_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_COMPLETED,
                        "您的销假申请" + statusText
                ));
            }
        } else if (isStaffRole(admin)) {
            var completedCancelApps = leaveApplicationRepository.findByApproverIdAndCancelRequestedTrueAndCancelApprovalStatusInAndDeletedFalseOrderByCancelTimeDesc(
                    admin.getId(),
                    List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode()),
                    PageRequest.of(0, PROCESS_PAGE_SIZE)
            );
            for (LeaveApplication application : completedCancelApps.getContent()) {
                String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(application.getCancelApprovalStatus()) ? "已批准" : "已拒绝";
                completedCancel.add(buildProcessItem(
                        application.getId(),
                        LEAVE_CANCEL_APPROVAL_NAME,
                        PROCESS_TYPE_LEAVE_CANCEL,
                        application.getCancelTime() != null ? application.getCancelTime().format(DATE_TIME_FORMATTER) : application.getCreateTime().format(DATE_TIME_FORMATTER),
                        PROCESS_STATUS_COMPLETED,
                        application.getStudentName() + "的销假申请" + statusText
                ));
            }
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
        return buildProcessItem(
                application.getId(), "奖助学金审批", PROCESS_TYPE_AWARD,
                application.getCreateTime().format(DATE_TIME_FORMATTER),
                application.getApprovalStatus(), "用户的奖助学金申请"
        );
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


    private void appendStudentCompletedLeaveProcesses(Admin admin, List<ProcessItem> completed, String status, String description) {
        var applications = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                admin.getId(), status, PageRequest.of(0, PROCESS_PAGE_SIZE)
        );
        for (LeaveApplication application : applications.getContent()) {
            completed.add(buildProcessItem(application.getId(), LEAVE_APPLICATION_NAME, PROCESS_TYPE_LEAVE,
                    application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_COMPLETED, description));
        }
    }

    private void appendStudentCompletedAwardProcesses(Admin admin, List<ProcessItem> completed, String status, String description) {
        var applications = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                admin.getId(), status, PageRequest.of(0, PROCESS_PAGE_SIZE)
        );
        for (AwardApplication application : applications.getContent()) {
            completed.add(buildProcessItem(application.getId(), AWARD_APPLICATION_NAME, PROCESS_TYPE_AWARD,
                    application.getCreateTime().format(DATE_TIME_FORMATTER), PROCESS_STATUS_COMPLETED, description));
        }
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
