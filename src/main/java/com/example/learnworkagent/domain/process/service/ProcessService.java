package com.example.learnworkagent.domain.process.service;

import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.dto.ProcessListResponse;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.domain.award.entity.AwardApplication;
import com.example.learnworkagent.domain.award.repository.AwardApplicationRepository;
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

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AwardApplicationRepository awardApplicationRepository;

    public ProcessListResponse getProcessList(User user) {
        ProcessListResponse response = new ProcessListResponse();
        List<ProcessItem> pending = new ArrayList<>();
        List<ProcessItem> completed = new ArrayList<>();

        // 获取待处理的请假申请
        var pendingLeaveApps = leaveApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("PENDING", PageRequest.of(0, 100));
        for (LeaveApplication app : pendingLeaveApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("请假审批");
            item.setType("leave");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("pending");
            item.setDescription("用户的请假申请需要审批");
            pending.add(item);
        }

        // 获取已处理的请假申请
        var approvedLeaveApps = leaveApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("APPROVED", PageRequest.of(0, 100));
        var rejectedLeaveApps = leaveApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("REJECTED", PageRequest.of(0, 100));
        for (LeaveApplication app : approvedLeaveApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("请假审批");
            item.setType("leave");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("completed");
            item.setDescription("用户的请假申请已审批");
            completed.add(item);
        }
        for (LeaveApplication app : rejectedLeaveApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("请假审批");
            item.setType("leave");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("completed");
            item.setDescription("用户的请假申请已审批");
            completed.add(item);
        }

        // 获取待处理的奖助申请
        var pendingAwardApps = awardApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("PENDING", PageRequest.of(0, 100));
        for (AwardApplication app : pendingAwardApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("奖助学金审批");
            item.setType("award");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("pending");
            item.setDescription("用户的奖助学金申请需要审批");
            pending.add(item);
        }

        // 获取已处理的奖助申请
        var approvedAwardApps = awardApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("APPROVED", PageRequest.of(0, 100));
        var rejectedAwardApps = awardApplicationRepository.findByApprovalStatusAndDeletedFalseOrderByCreateTimeDesc("REJECTED", PageRequest.of(0, 100));
        for (AwardApplication app : approvedAwardApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("奖助学金审批");
            item.setType("award");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("completed");
            item.setDescription("用户的奖助学金申请已审批");
            completed.add(item);
        }
        for (AwardApplication app : rejectedAwardApps.getContent()) {
            ProcessItem item = new ProcessItem();
            item.setId(app.getId().toString());
            item.setName("奖助学金审批");
            item.setType("award");
            item.setCreateTime(app.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setStatus("completed");
            item.setDescription("用户的奖助学金申请已审批");
            completed.add(item);
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
