package com.example.learnworkagent.domain.process.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.NotificationBusinessTypeEnum;
import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import com.example.learnworkagent.domain.approval.repository.ApprovalTaskRepository;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.dto.ProcessListResponse;
import com.example.learnworkagent.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PROCESS_PAGE_SIZE = 100;
    private static final String PROCESS_STATUS_PENDING = "pending";
    private static final String PROCESS_STATUS_COMPLETED = "completed";
    private static final String PROCESS_TYPE_LEAVE = "leave";
    private static final String PROCESS_TYPE_AWARD = "award";
    private static final String LEAVE_APPLICATION_NAME = "请假申请";
    private static final String AWARD_APPLICATION_NAME = "奖助申请";
    private static final String LEAVE_APPROVAL_NAME = "请假审批";
    private static final String AWARD_APPROVAL_NAME = "奖助审批";

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final ApprovalTaskRepository approvalTaskRepository;

    public ProcessListResponse getProcessList(User user) {
        ProcessListResponse response = new ProcessListResponse();
        List<ProcessItem> pending = new ArrayList<>();
        List<ProcessItem> completed = new ArrayList<>();

        if (RoleEnum.STUDENT.getCode().equals(user.getRole())) {
            fillStudentPendingProcesses(user, pending);
        } else if (isStaffRole(user)) {
            fillStaffPendingTasks(user, pending);
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "出现未知的审批角色: " + user.getRole());
        }

        response.setPending(pending);
        response.setCompleted(completed);
        return response;
    }

    /**
     * 获取流程详情
     *
     * @param id 流程id
     * @param type 流程类型（请假或奖助）
     * @return 流程详情
     */
    public ProcessItem getProcessDetail(String id, String type) {
        if (PROCESS_TYPE_LEAVE.equals(type)) {
            return buildLeaveProcessDetail(id);
        }
        if (PROCESS_TYPE_AWARD.equals(type)) {
            return buildAwardProcessDetail(id);
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "未知的流程类型: " + type);
    }

    public List<ProcessItem> getCompletedProcesses(User user) {
        List<ProcessItem> completed = new ArrayList<>();

        if (RoleEnum.STUDENT.getCode().equals(user.getRole())) {
            fillStudentCompletedProcesses(user, completed);
        } else if (isStaffRole(user)) {
            fillStaffCompletedTasks(user, completed);
        }

        return completed;
    }

    private void fillStudentPendingProcesses(User user, List<ProcessItem> pending) {
        var pendingLeaveApps = leaveApplicationRepository
                .findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                        user.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
                );
        for (LeaveApplication application : pendingLeaveApps.getContent()) {
            pending.add(buildStudentApplicationItem(
                    application.getId(),
                    LEAVE_APPLICATION_NAME,
                    PROCESS_TYPE_LEAVE,
                    application.getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_PENDING,
                    "您的请假申请正在审批中"
            ));
        }

        var pendingAwardApps = awardApplicationRepository
                .findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                        user.getId(), ApprovalStatusEnum.PENDING.getCode(), PageRequest.of(0, PROCESS_PAGE_SIZE)
                );
        for (AwardApplication application : pendingAwardApps.getContent()) {
            pending.add(buildStudentApplicationItem(
                    application.getId(),
                    AWARD_APPLICATION_NAME,
                    PROCESS_TYPE_AWARD,
                    application.getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_PENDING,
                    "您的奖助申请正在审批中"
            ));
        }
    }

    private void fillStaffPendingTasks(User user, List<ProcessItem> pending) {
        List<ApprovalTask> pendingTasks = approvalTaskRepository.findByApproverIdAndStatus(
                user.getId(), ApprovalStatusEnum.PROCESSING.getCode()
        );

        for (ApprovalTask task : pendingTasks) {
            boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
            pending.add(buildStudentApplicationItem(
                    task.getInstance().getBusinessId(),
                    leaveBusiness ? LEAVE_APPROVAL_NAME : AWARD_APPROVAL_NAME,
                    task.getInstance().getBusinessType().toLowerCase(),
                    task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_PENDING,
                    leaveBusiness ? "学生的请假申请需要您审批" : "学生的奖助申请需要您审批"
            ));
        }
    }

    private ProcessItem buildLeaveProcessDetail(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(Long.parseLong(id)).orElse(null);
        if (application == null) {
            return new ProcessItem();
        }

        return buildStudentApplicationItem(
                application.getId(),
                LEAVE_APPROVAL_NAME,
                PROCESS_TYPE_LEAVE,
                application.getCreateTime().format(DATE_TIME_FORMATTER),
                ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus())
                        ? PROCESS_STATUS_PENDING
                        : PROCESS_STATUS_COMPLETED,
                "用户的请假申请"
        );
    }

    private ProcessItem buildAwardProcessDetail(String id) {
        AwardApplication application = awardApplicationRepository.findById(Long.parseLong(id)).orElse(null);
        if (application == null) {
            return new ProcessItem();
        }

        return buildStudentApplicationItem(
                application.getId(),
                "奖助学金审批",
                PROCESS_TYPE_AWARD,
                application.getCreateTime().format(DATE_TIME_FORMATTER),
                application.getApprovalStatus(),
                "用户的奖助学金申请"
        );
    }

    private void fillStudentCompletedProcesses(User user, List<ProcessItem> completed) {
        appendStudentCompletedLeaveProcesses(user, completed, ApprovalStatusEnum.APPROVED.getCode(), "您的请假申请已批准");
        appendStudentCompletedLeaveProcesses(user, completed, ApprovalStatusEnum.REJECTED.getCode(), "您的请假申请已拒绝");
        appendStudentCompletedAwardProcesses(user, completed, ApprovalStatusEnum.APPROVED.getCode(), "您的奖助申请已批准");
        appendStudentCompletedAwardProcesses(user, completed, ApprovalStatusEnum.REJECTED.getCode(), "您的奖助申请已拒绝");
    }

    private void appendStudentCompletedLeaveProcesses(User user, List<ProcessItem> completed, String status, String description) {
        var applications = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                user.getId(), status, PageRequest.of(0, PROCESS_PAGE_SIZE)
        );
        for (LeaveApplication application : applications.getContent()) {
            completed.add(buildStudentApplicationItem(
                    application.getId(),
                    LEAVE_APPLICATION_NAME,
                    PROCESS_TYPE_LEAVE,
                    application.getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_COMPLETED,
                    description
            ));
        }
    }

    private void appendStudentCompletedAwardProcesses(User user, List<ProcessItem> completed, String status, String description) {
        var applications = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(
                user.getId(), status, PageRequest.of(0, PROCESS_PAGE_SIZE)
        );
        for (AwardApplication application : applications.getContent()) {
            completed.add(buildStudentApplicationItem(
                    application.getId(),
                    AWARD_APPLICATION_NAME,
                    PROCESS_TYPE_AWARD,
                    application.getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_COMPLETED,
                    description
            ));
        }
    }

    private void fillStaffCompletedTasks(User user, List<ProcessItem> completed) {
        List<ApprovalTask> completedTasks = approvalTaskRepository.findByApproverIdAndStatusIn(
                user.getId(),
                List.of(ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
        );
        for (ApprovalTask task : completedTasks) {
            boolean leaveBusiness = NotificationBusinessTypeEnum.LEAVE.getCode().equals(task.getInstance().getBusinessType());
            String statusText = ApprovalStatusEnum.APPROVED.getCode().equals(task.getStatus()) ? "已批准" : "已拒绝";
            completed.add(buildStudentApplicationItem(
                    task.getInstance().getBusinessId(),
                    leaveBusiness ? LEAVE_APPROVAL_NAME : AWARD_APPROVAL_NAME,
                    task.getInstance().getBusinessType().toLowerCase(),
                    task.getInstance().getCreateTime().format(DATE_TIME_FORMATTER),
                    PROCESS_STATUS_COMPLETED,
                    leaveBusiness ? "学生的请假申请您已" + statusText : "学生的奖助申请您已" + statusText
            ));
        }
    }

    private ProcessItem buildStudentApplicationItem(Long id, String name, String type, String createTime,
                                                    String status, String description) {
        ProcessItem item = new ProcessItem();
        item.setId(String.valueOf(id));
        item.setName(name);
        item.setType(type);
        item.setCreateTime(createTime);
        item.setStatus(status);
        item.setDescription(description);
        return item;
    }

    private boolean isStaffRole(User user) {
        return RoleEnum.COUNSELOR.getCode().equals(user.getRole())
                || RoleEnum.COLLEGE_LEADER.getCode().equals(user.getRole())
                || RoleEnum.DEPARTMENT_LEADER.getCode().equals(user.getRole());
    }
}
