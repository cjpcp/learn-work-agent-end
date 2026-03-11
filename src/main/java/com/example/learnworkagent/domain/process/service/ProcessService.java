package com.example.learnworkagent.domain.process.service;

import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.dto.ProcessListResponse;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.approval.repository.ApprovalTaskRepository;
import com.example.learnworkagent.domain.approval.entity.ApprovalTask;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final ApprovalTaskRepository approvalTaskRepository;

    public ProcessListResponse getProcessList(User user) {
        ProcessListResponse response = new ProcessListResponse();
        List<ProcessItem> pending = new ArrayList<>();
        List<ProcessItem> completed = new ArrayList<>();

        if ("STUDENT".equals(user.getRole())) {
            // 学生角色：待办流程是自己申请的未审批，已办理流程是自己申请的已审批
            // 获取学生待处理的请假申请
            var pendingLeaveApps = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "PENDING", PageRequest.of(0, 100));
            for (LeaveApplication app : pendingLeaveApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("请假申请");
                item.setType("leave");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("pending");
                item.setDescription("您的请假申请正在审批中");
                pending.add(item);
            }

            // 获取学生已处理的请假申请
            var approvedLeaveApps = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "APPROVED", PageRequest.of(0, 100));
            var rejectedLeaveApps = leaveApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "REJECTED", PageRequest.of(0, 100));
            for (LeaveApplication app : approvedLeaveApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("请假申请");
                item.setType("leave");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("completed");
                item.setDescription("您的请假申请已批准");
                completed.add(item);
            }
            for (LeaveApplication app : rejectedLeaveApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("请假申请");
                item.setType("leave");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("completed");
                item.setDescription("您的请假申请已拒绝");
                completed.add(item);
            }

            // 获取学生待处理的奖助申请
            var pendingAwardApps = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "PENDING", PageRequest.of(0, 100));
            for (AwardApplication app : pendingAwardApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("奖助申请");
                item.setType("award");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("pending");
                item.setDescription("您的奖助申请正在审批中");
                pending.add(item);
            }

            // 获取学生已处理的奖助申请
            var approvedAwardApps = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "APPROVED", PageRequest.of(0, 100));
            var rejectedAwardApps = awardApplicationRepository.findByApplicantIdAndApprovalStatusAndDeletedFalseOrderByCreateTimeDesc(user.getId(), "REJECTED", PageRequest.of(0, 100));
            for (AwardApplication app : approvedAwardApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("奖助申请");
                item.setType("award");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("completed");
                item.setDescription("您的奖助申请已批准");
                completed.add(item);
            }
            for (AwardApplication app : rejectedAwardApps.getContent()) {
                ProcessItem item = new ProcessItem();
                item.setId(app.getId().toString());
                item.setName("奖助申请");
                item.setType("award");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("completed");
                item.setDescription("您的奖助申请已拒绝");
                completed.add(item);
            }
        } else if ("COUNSELOR".equals(user.getRole())) {
            // 辅导员角色：待办流程是分配给自己的未审批，已办理流程是自己已审批的
            // 获取辅导员待处理的审批任务
            List<ApprovalTask> pendingTasks = approvalTaskRepository.findByApproverIdAndStatus(user.getId(), "PENDING");
            for (ApprovalTask task : pendingTasks) {
                ProcessItem item = new ProcessItem();
                item.setId(task.getInstance().getBusinessId().toString());
                item.setName(task.getInstance().getBusinessType().equals("LEAVE") ? "请假审批" : "奖助审批");
                item.setType(task.getInstance().getBusinessType().toLowerCase());
                item.setCreateTime(task.getInstance().getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus("pending");
                item.setDescription(task.getInstance().getBusinessType().equals("LEAVE") ? "学生的请假申请需要您审批" : "学生的奖助申请需要您审批");
                pending.add(item);
            }

            // 暂时不处理已完成的审批任务
            // TODO: 实现已完成审批任务的查询
        }

        response.setPending(pending);
        response.setCompleted(completed);
        return response;
    }

    public ProcessItem getProcessDetail(String id, String type) {
        ProcessItem item = new ProcessItem();
        // 根据类型和ID获取详细信息
        if ("leave".equals(type)) {
            LeaveApplication app = leaveApplicationRepository.findById(Long.parseLong(id)).orElse(null);
            if (app != null) {
                item.setId(app.getId().toString());
                item.setName("请假审批");
                item.setType("leave");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus(app.getApprovalStatus().equals("PENDING") ? "pending" : "completed");
                item.setDescription("用户的请假申请");
            }
        } else if ("award".equals(type)) {
            AwardApplication app = awardApplicationRepository.findById(Long.parseLong(id)).orElse(null);
            if (app != null) {
                item.setId(app.getId().toString());
                item.setName("奖助学金审批");
                item.setType("award");
                item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                item.setStatus(app.getApprovalStatus().equals("PENDING") ? "pending" : "completed");
                item.setDescription("用户的奖助学金申请");
            }
        }
        return item;
    }
}
