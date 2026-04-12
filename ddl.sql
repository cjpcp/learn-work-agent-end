-- ============================================
-- Learn Work Agent 数据库表结构 DDL
-- 数据库: learn_work_agent
-- ============================================

CREATE DATABASE IF NOT EXISTS learn_work_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learn_work_agent;

-- -------------------------------------------
-- 1. 角色表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `role_name` VARCHAR(20) NOT NULL COMMENT '角色名称',
    `power_id` VARCHAR(255) DEFAULT NULL COMMENT '权限id',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `page_path` VARCHAR(255) DEFAULT NULL COMMENT '页面路径',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- -------------------------------------------
-- 2. 权限表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `power` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `power_name` VARCHAR(255) NOT NULL COMMENT '权限名称',
    `power_url` VARCHAR(255) NOT NULL COMMENT '权限地址',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `pid` INT NOT NULL COMMENT '父级id',
    `level` INT NOT NULL COMMENT '等级',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- -------------------------------------------
-- 3. 教师表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `teacher` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `name` VARCHAR(50) NOT NULL COMMENT '老师姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `card_number` VARCHAR(30) DEFAULT NULL COMMENT '学工号',
    `state` INT NOT NULL COMMENT '状态（0：关闭 1：开启）',
    `create_time` INT NOT NULL COMMENT '创建时间',
    `update_time` INT NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

-- -------------------------------------------
-- 4. 管理员表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `admin` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `username` VARCHAR(30) NOT NULL COMMENT '用户名',
    `nick` VARCHAR(30) NOT NULL COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `role_id` BIGINT NOT NULL COMMENT '角色id',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `status` INT NOT NULL COMMENT '状态',
    `teacher_id` BIGINT NOT NULL COMMENT '教师id',
    PRIMARY KEY (`id`),
    KEY `idx_admin_role_id` (`role_id`),
    KEY `idx_admin_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账户表';

-- -------------------------------------------
-- 5. 请假申请表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `leave_type` VARCHAR(20) NOT NULL COMMENT '请假类型',
    `start_date` DATE NOT NULL COMMENT '请假开始日期',
    `end_date` DATE NOT NULL COMMENT '请假结束日期',
    `days` INT NOT NULL COMMENT '请假天数',
    `reason` TEXT COMMENT '请假原因',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approval_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
    `approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `leave_slip_status` VARCHAR(20) DEFAULT 'NOT_GENERATED' COMMENT '请假条生成状态',
    `leave_slip_url` VARCHAR(500) DEFAULT NULL COMMENT '请假条URL',
    `cancelled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已销假',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '销假时间',
    `cancel_requested` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已申请销假',
    `cancel_approval_status` VARCHAR(20) DEFAULT NULL COMMENT '销假审批状态',
    `cancel_approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '销假审批意见',
    `cancel_approval_time` DATETIME DEFAULT NULL COMMENT '销假审批时间',
    `student_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `department_name` VARCHAR(50) DEFAULT NULL COMMENT '院系名称',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
    PRIMARY KEY (`id`),
    KEY `idx_leave_applicant` (`applicant_id`),
    KEY `idx_leave_approver` (`approver_id`),
    KEY `idx_leave_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- -------------------------------------------
-- 6. 奖助申请表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `award_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `application_type` VARCHAR(20) NOT NULL COMMENT '申请类型（SCHOLARSHIP-奖学金, GRANT-助学金, SUBSIDY-困难补助）',
    `award_name` VARCHAR(200) NOT NULL COMMENT '申请名称',
    `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '申请金额',
    `reason` TEXT COMMENT '申请理由',
    `material_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '材料预审状态',
    `material_comment` VARCHAR(500) DEFAULT NULL COMMENT '材料预审意见',
    `material_review_time` DATETIME DEFAULT NULL COMMENT '材料预审时间',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approval_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
    `approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `attachment_urls` TEXT COMMENT '附件URL列表（JSON格式）',
    `student_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `department_id` BIGINT DEFAULT NULL COMMENT '院系ID',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
    PRIMARY KEY (`id`),
    KEY `idx_award_applicant` (`applicant_id`),
    KEY `idx_award_approver` (`approver_id`),
    KEY `idx_award_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖助申请表';

-- -------------------------------------------
-- 7. 审批流程表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `approval_process` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `process_name` VARCHAR(100) NOT NULL COMMENT '流程名称',
    `process_type` VARCHAR(20) NOT NULL COMMENT '流程类型（LEAVE-请假, AWARD-奖助）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    PRIMARY KEY (`id`),
    KEY `idx_process_type` (`process_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程表';

-- -------------------------------------------
-- 8. 审批步骤表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `approval_stage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `process_id` BIGINT NOT NULL COMMENT '流程ID',
    `step_name` VARCHAR(100) NOT NULL COMMENT '步骤名称',
    `step_order` INT NOT NULL COMMENT '步骤顺序',
    `approval_type` VARCHAR(20) NOT NULL DEFAULT 'SINGLE' COMMENT '审批类型（SINGLE-单人审批, MULTIPLE-多人审批）',
    `approver_role` VARCHAR(20) NOT NULL COMMENT '审批人角色（COUNSELOR-辅导员, COLLEGE_LEADER-院领导, DEPARTMENT_LEADER-部门领导）',
    `must_pass` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必须通过',
    `approver_user_id` BIGINT DEFAULT NULL COMMENT '具体审批人ID',
    `assign_mode` VARCHAR(20) DEFAULT 'USER' COMMENT '分配模式（USER-指定用户, ROLE-按角色池分配）',
    `role_id` VARCHAR(50) DEFAULT NULL COMMENT '目标角色ID',
    PRIMARY KEY (`id`),
    KEY `idx_stage_process` (`process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批步骤表';

-- -------------------------------------------
-- 9. 审批实例表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `approval_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `business_type` VARCHAR(20) NOT NULL COMMENT '业务类型（LEAVE-请假, AWARD-奖助）',
    `business_id` BIGINT NOT NULL COMMENT '业务ID',
    `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人ID',
    `process_id` BIGINT NOT NULL COMMENT '流程ID',
    `current_step` INT DEFAULT NULL COMMENT '当前步骤',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '整体状态',
    `completed_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_business` (`business_type`, `business_id`),
    KEY `idx_instance_applicant` (`applicant_id`),
    KEY `idx_instance_process` (`process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批实例表';

-- -------------------------------------------
-- 10. 审批任务表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `approval_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `instance_id` BIGINT NOT NULL COMMENT '审批实例ID',
    `step_id` BIGINT NOT NULL COMMENT '审批步骤ID',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
    `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `task_order` INT DEFAULT NULL COMMENT '任务顺序',
    PRIMARY KEY (`id`),
    KEY `idx_task_instance` (`instance_id`),
    KEY `idx_task_step` (`step_id`),
    KEY `idx_task_approver` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批任务表';

-- -------------------------------------------
-- 11. 咨询问题表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `consultation_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question_text` TEXT COMMENT '问题内容',
    `question_type` VARCHAR(20) NOT NULL COMMENT '问题类型（TEXT-文本, VOICE-语音, IMAGE-图片）',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '问题分类',
    `voice_url` VARCHAR(500) DEFAULT NULL COMMENT '语音URL',
    `ai_answer` TEXT COMMENT 'AI回答内容',
    `answer_source` VARCHAR(20) DEFAULT NULL COMMENT '回答来源（AI-智能回答, HUMAN-人工回答）',
    `transferred_to_human` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已转人工',
    `transfer_reason` VARCHAR(500) DEFAULT NULL COMMENT '转人工原因',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `satisfaction_score` INT DEFAULT NULL COMMENT '满意度评分（1-5）',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
    PRIMARY KEY (`id`),
    KEY `idx_consult_user` (`user_id`),
    KEY `idx_consult_session` (`session_id`),
    KEY `idx_consult_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询问题表';

-- -------------------------------------------
-- 12. 人工转接配置表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `human_transfer_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务分类',
    `assign_mode` VARCHAR(20) NOT NULL COMMENT '分配模式：USER/ROLE',
    `role_id` BIGINT DEFAULT NULL COMMENT '目标角色ID',
    `user_ids` VARCHAR(1000) DEFAULT NULL COMMENT '目标用户ID列表，逗号分隔',
    `priority` INT NOT NULL DEFAULT 1 COMMENT '优先级',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_config_business` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工转接配置表';

-- -------------------------------------------
-- 13. 人工转接记录表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `human_transfer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `question_id` BIGINT NOT NULL COMMENT '咨询问题ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `staff_id` BIGINT DEFAULT NULL COMMENT '工作人员ID',
    `transfer_reason` VARCHAR(500) DEFAULT NULL COMMENT '转接原因',
    `transfer_type` VARCHAR(20) NOT NULL COMMENT '转接方式（AUTO-自动识别, MANUAL-用户主动申请）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `staff_reply` TEXT COMMENT '工作人员回复内容',
    `process_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_transfer_question` (`question_id`),
    KEY `idx_transfer_staff` (`staff_id`),
    KEY `idx_transfer_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工转接记录表';

-- -------------------------------------------
-- 14. 通知表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '读取时间',
    `business_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `business_type` VARCHAR(50) DEFAULT NULL COMMENT '关联业务类型',
    `channel` VARCHAR(20) DEFAULT NULL COMMENT '推送渠道',
    `channel_status` VARCHAR(500) DEFAULT NULL COMMENT '各渠道发送状态',
    PRIMARY KEY (`id`),
    KEY `idx_notification_user` (`user_id`),
    KEY `idx_notification_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- -------------------------------------------
-- 外键约束
-- -------------------------------------------
ALTER TABLE `admin` ADD CONSTRAINT `fk_admin_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`);
ALTER TABLE `admin` ADD CONSTRAINT `fk_admin_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher`(`id`);

ALTER TABLE `approval_stage` ADD CONSTRAINT `fk_stage_process` FOREIGN KEY (`process_id`) REFERENCES `approval_process`(`id`);

ALTER TABLE `approval_instance` ADD CONSTRAINT `fk_instance_process` FOREIGN KEY (`process_id`) REFERENCES `approval_process`(`id`);

ALTER TABLE `approval_task` ADD CONSTRAINT `fk_task_instance` FOREIGN KEY (`instance_id`) REFERENCES `approval_instance`(`id`);
ALTER TABLE `approval_task` ADD CONSTRAINT `fk_task_step` FOREIGN KEY (`step_id`) REFERENCES `approval_stage`(`id`);
