# Postman 测试接口 - curl 命令

可以直接导入 Postman 的 curl 命令集合。

## 基础 URL
```
http://localhost:8080
```

> **注意**: 需要在 Header 中添加认证 Token：
> - Key: `Authorization`
> - Value: `Bearer {your_jwt_token}`

---

## 认证管理 (AuthController)

### 用户登录
```bash
curl --location --request POST 'http://localhost:8080/api/v1/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "your_username",
    "password": "your_password"
}'
```

### 用户注册
```bash
curl --location --request POST 'http://localhost:8080/api/v1/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "new_username",
    "password": "new_password",
    "nick": "用户昵称",
    "roleId": 1
}'
```

### 检查用户名是否存在
```bash
curl --location --request GET 'http://localhost:8080/api/v1/auth/check-username?username=your_username'
```

---

## 智能咨询 (ConsultationController)

### 提交咨询问题
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/questions' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "questionText": "咨询问题内容",
    "questionType": "TEXT",
    "category": "general",
    "sessionId": "session_123"
}'
```

### 获取问题详情
```bash
curl --location --request GET 'http://localhost:8080/api/v1/consultation/questions/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取问题对话历史
```bash
curl --location --request GET 'http://localhost:8080/api/v1/consultation/questions/{id}/history' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页查询我的问题
```bash
curl --location --request GET 'http://localhost:8080/api/v1/consultation/questions/my?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 申请转人工
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/questions/{id}/transfer' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "reason": "需要人工服务的原因",
    "questionType": "TEXT",
    "questionText": "补充说明"
}'
```

### 直接申请转人工
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/transfer' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "reason": "需要人工服务的原因",
    "questionType": "TEXT",
    "questionText": "问题描述"
}'
```

### 提交咨询问题（流式响应）
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/questions/stream' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "questionText": "流式咨询问题",
    "questionType": "TEXT",
    "category": "general",
    "sessionId": "session_456"
}'
```

### 上传咨询语音
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/upload/voice' \
--header 'Authorization: Bearer {your_jwt_token}' \
--form 'file=@"/path/to/voicefile.mp3"'
```

### 上传咨询文件
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/upload/file' \
--header 'Authorization: Bearer {your_jwt_token}' \
--form 'file=@"/path/to/document.pdf"'
```

### 提交咨询问题（流式响应+附件同步上传）
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/questions/stream/multipart' \
--header 'Authorization: Bearer {your_jwt_token}' \
--form 'questionText="带附件的流式问题"' \
--form 'sessionId="session_789"' \
--form 'files=@"/path/to/file1.pdf"' \
--form 'files=@"/path/to/file2.pdf"'
```

---

## 流程记录 (ProcessController)

### 获取所有待审批流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/pending/all?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取奖助待审批流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/pending/award?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取请假待审批流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/pending/leave?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取销假待审批流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/pending/leave-cancel?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取所有已完成流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/completed/all?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取奖助已完成流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/completed/award?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取请假已完成流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/completed/leave?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取销假已完成流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/completed/leave-cancel?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取流程详情
```bash
curl --location --request GET 'http://localhost:8080/api/v1/process/{id}?type=leave' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 系统管理 (SystemController)

### 获取可选角色列表
```bash
curl --location --request GET 'http://localhost:8080/api/v1/system/roles/staff'
```

### 获取所有角色列表
```bash
curl --location --request GET 'http://localhost:8080/api/v1/system/roles'
```

### 分页获取用户列表
```bash
curl --location --request GET 'http://localhost:8080/api/v1/system/users?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页获取用户列表（按角色筛选）
```bash
curl --location --request GET 'http://localhost:8080/api/v1/system/users?pageNum=1&pageSize=10&roleId=1' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页获取用户列表（关键词筛选）
```bash
curl --location --request GET 'http://localhost:8080/api/v1/system/users?pageNum=1&pageSize=10&teacherKeyword=张三' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 通知管理 (NotificationController)

### 获取通知列表
```bash
curl --location --request GET 'http://localhost:8080/api/v1/notifications?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取未读通知数量
```bash
curl --location --request GET 'http://localhost:8080/api/v1/notifications/unread-count' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 标记通知为已读
```bash
curl --location --request POST 'http://localhost:8080/api/v1/notifications/{id}/read' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 请假管理 (LeaveController)

### 提交请假申请
```bash
curl --location --request POST 'http://localhost:8080/api/v1/leave/applications' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "leaveType": "ANNUAL",
    "startDate": "2024-03-01",
    "endDate": "2024-03-05",
    "reason": "请假原因",
    "destination": "外出地点"
}'
```

### 获取请假申请详情
```bash
curl --location --request GET 'http://localhost:8080/api/v1/leave/applications/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页查询我的请假申请
```bash
curl --location --request GET 'http://localhost:8080/api/v1/leave/applications/my?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页查询待审批销假申请
```bash
curl --location --request GET 'http://localhost:8080/api/v1/leave/applications/pending-cancel?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 审批请假申请
```bash
curl --location --request POST 'http://localhost:8080/api/v1/leave/applications/{id}/approve' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "approvalStatus": "APPROVED",
    "approvalComment": "审批意见"
}'
```

### 生成请假条
```bash
curl --location --request POST 'http://localhost:8080/api/v1/leave/applications/{id}/generate-slip' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 申请销假
```bash
curl --location --request POST 'http://localhost:8080/api/v1/leave/applications/{id}/cancel' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 撤销请假申请
```bash
curl --location --request DELETE 'http://localhost:8080/api/v1/leave/applications/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 审批销假申请
```bash
curl --location --request POST 'http://localhost:8080/api/v1/leave/applications/{id}/approve-cancel' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "approvalStatus": "APPROVED",
    "approvalComment": "销假审批意见"
}'
```

### 下载请假条
```bash
curl --location --request GET 'http://localhost:8080/api/v1/leave/applications/{id}/download-slip' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 人工转接配置 (HumanTransferConfigController)

### 获取人工转接配置列表
```bash
curl --location --request GET 'http://localhost:8080/api/v1/consultation/transfer-config' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 创建人工转接配置
```bash
curl --location --request POST 'http://localhost:8080/api/v1/consultation/transfer-config' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "questionType": "TEXT",
    "conditions": {},
    "targetDepartment": "客服部",
    "priority": 1
}'
```

### 更新人工转接配置
```bash
curl --location --request PUT 'http://localhost:8080/api/v1/consultation/transfer-config/{id}' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "questionType": "VOICE",
    "conditions": {},
    "targetDepartment": "客服部",
    "priority": 2
}'
```

### 删除人工转接配置
```bash
curl --location --request DELETE 'http://localhost:8080/api/v1/consultation/transfer-config/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 健康检查 (HealthController)

### 健康检查
```bash
curl --location --request GET 'http://localhost:8080/health'
```

---

## 审批管理 (ApprovalController)

### 获取我的待审批任务
```bash
curl --location --request GET 'http://localhost:8080/api/v1/approval/tasks/pending' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 处理审批任务
```bash
curl --location --request POST 'http://localhost:8080/api/v1/approval/tasks/{id}/process' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "status": "APPROVED",
    "comment": "审批意见"
}'
```

### 获取审批详情
```bash
curl --location --request GET 'http://localhost:8080/api/v1/approval/instances/{businessType}/{businessId}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 奖助管理 (AwardController)

### 提交奖助申请
```bash
curl --location --request POST 'http://localhost:8080/api/v1/award/applications' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "awardType": "SCHOLARSHIP",
    "awardName": "奖学金名称",
    "amount": 5000,
    "reason": "申请理由",
    "attachments": ["https://example.com/attachment1.pdf"]
}'
```

### 获取奖助申请详情
```bash
curl --location --request GET 'http://localhost:8080/api/v1/award/applications/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页查询我的奖助申请
```bash
curl --location --request GET 'http://localhost:8080/api/v1/award/applications/my?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 分页查询待审批的奖助申请（审批人）
```bash
curl --location --request GET 'http://localhost:8080/api/v1/award/applications/pending?pageNum=1&pageSize=10' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 审批奖助申请
```bash
curl --location --request POST 'http://localhost:8080/api/v1/award/applications/{id}/approve' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "approvalStatus": "APPROVED",
    "approvalComment": "审批意见"
}'
```

### 撤销奖助申请
```bash
curl --location --request DELETE 'http://localhost:8080/api/v1/award/applications/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## 审批流程配置 (ApprovalConfigController)

### 获取所有审批流程
```bash
curl --location --request GET 'http://localhost:8080/api/v1/approval/config/processes' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 创建审批流程
```bash
curl --location --request POST 'http://localhost:8080/api/v1/approval/config/processes' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "processName": "请假审批流程",
    "processType": "LEAVE",
    "description": "员工请假审批流程",
    "enabled": true,
    "version": 1
}'
```

### 更新审批流程
```bash
curl --location --request PUT 'http://localhost:8080/api/v1/approval/config/processes/{id}' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "processName": "请假审批流程（修订版）",
    "processType": "LEAVE",
    "description": "更新后的描述",
    "enabled": true,
    "version": 2
}'
```

### 删除审批流程
```bash
curl --location --request DELETE 'http://localhost:8080/api/v1/approval/config/processes/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 获取流程的审批步骤
```bash
curl --location --request GET 'http://localhost:8080/api/v1/approval/config/processes/{processId}/steps' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 添加审批步骤
```bash
curl --location --request POST 'http://localhost:8080/api/v1/approval/config/steps' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "processId": 1,
    "stepOrder": 1,
    "stepName": "班主任审批",
    "stepType": "TEACHER",
    "approverId": 1,
    "description": "第一步审批"
}'
```

### 更新审批步骤
```bash
curl --location --request PUT 'http://localhost:8080/api/v1/approval/config/steps/{id}' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {your_jwt_token}' \
--data-raw '{
    "processId": 1,
    "stepOrder": 1,
    "stepName": "班主任审批（修订）",
    "stepType": "TEACHER",
    "approverId": 2,
    "description": "更新后的步骤描述"
}'
```

### 删除审批步骤
```bash
curl --location --request DELETE 'http://localhost:8080/api/v1/approval/config/steps/{id}' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 启用审批流程
```bash
curl --location --request POST 'http://localhost:8080/api/v1/approval/config/processes/{id}/enable' \
--header 'Authorization: Bearer {your_jwt_token}'
```

### 禁用审批流程
```bash
curl --location --request POST 'http://localhost:8080/api/v1/approval/config/processes/{id}/disable' \
--header 'Authorization: Bearer {your_jwt_token}'
```

---

## Postman 导入说明

1. 打开 Postman
2. 点击左上角 **Import** 按钮
3. 选择 **Raw text** 标签页
4. 粘贴上述任意 curl 命令
5. 点击 **Continue** / **Import**

或者直接在 Postman 的 **New Request** 窗口中：
1. 选择请求方法（GET/POST/PUT/DELETE）
2. 输入 URL
3. 在 **Headers** 标签页添加认证信息
4. 在 **Body** 标签页选择 **raw** 并设置 Content-Type 为 JSON
5. 输入请求体内容
