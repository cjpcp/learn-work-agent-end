create table if not exists admin
(
    id         bigint auto_increment comment '主键'
        primary key,
    created_at datetime(6)  null,
    login_time datetime(6)  null,
    nick       varchar(30)  not null comment '昵称',
    password   varchar(255) not null comment '密码',
    role_id    bigint       not null comment '角色id',
    status     int          not null comment '状态',
    teacher_id bigint       not null comment '教师id',
    updated_at datetime(6)  null,
    username   varchar(30)  not null comment '用户名'
);

create table if not exists approval_process
(
    id           bigint auto_increment
        primary key,
    create_time  datetime(6)  not null,
    deleted      bit          not null,
    update_time  datetime(6)  null,
    description  varchar(500) null,
    enabled      bit          not null,
    name         varchar(100) not null,
    type         varchar(20)  not null,
    process_name varchar(100) not null,
    process_type varchar(20)  not null,
    version      int          not null
);

create table if not exists approval_instance
(
    current_step   int         null,
    deleted        bit         not null,
    business_id    bigint      not null,
    completed_time datetime(6) null,
    create_time    datetime(6) not null,
    id             bigint auto_increment
        primary key,
    process_id     bigint      not null,
    update_time    datetime(6) null,
    business_type  varchar(20) not null,
    status         varchar(20) not null,
    applicant_id   bigint      null comment '申请人ID',
    constraint FKamr5uemifnbxspk9m912hnrui
        foreign key (process_id) references approval_process (id)
);

create table if not exists approval_stage
(
    id               bigint auto_increment
        primary key,
    create_time      datetime(6)  not null,
    deleted          bit          not null,
    update_time      datetime(6)  null,
    approval_type    varchar(20)  not null,
    approver_role    varchar(20)  not null,
    approver_type    varchar(20)  null,
    approver_user_id bigint       null,
    department       varchar(100) null,
    must_pass        bit          not null,
    name             varchar(100) null,
    order_index      int          not null,
    step_name        varchar(100) not null,
    step_order       int          not null,
    process_id       bigint       not null,
    department_id    bigint       null comment '部门ID（用于部门领导审批步骤筛选）',
    assign_mode      varchar(20)  null comment '分配模式（USER-指定用户, ROLE-按角色池分配）',
    role_id          varchar(50)  null comment '目标角色ID（按角色池分配时使用）',
    constraint FKm76wvhixcyf3rh8jht1m9qvcm
        foreign key (process_id) references approval_process (id)
);

create table if not exists approval_task
(
    id            bigint auto_increment
        primary key,
    create_time   datetime(6)  not null,
    deleted       bit          not null,
    update_time   datetime(6)  null,
    approval_time datetime(6)  null,
    approver_id   bigint       not null,
    comment       varchar(500) null,
    status        varchar(20)  not null,
    task_order    int          null,
    instance_id   bigint       not null,
    step_id       bigint       not null,
    constraint FKfbyj69061n7fqyxy5lif53cun
        foreign key (instance_id) references approval_instance (id),
    constraint FKqn5wkp1v0cxlydi0gb11wy5pe
        foreign key (step_id) references approval_stage (id)
);

create table if not exists award_application
(
    amount               decimal(10, 2) null,
    deleted              bit            not null,
    applicant_id         bigint         not null,
    approval_time        datetime(6)    null,
    approver_id          bigint         null,
    create_time          datetime(6)    not null,
    id                   bigint auto_increment
        primary key,
    material_review_time datetime(6)    null,
    update_time          datetime(6)    null,
    application_type     varchar(20)    not null,
    approval_status      varchar(20)    not null,
    grade                varchar(20)    null,
    material_status      varchar(20)    not null,
    class_name           varchar(50)    null,
    student_name         varchar(50)    null,
    department           varchar(100)   null,
    award_name           varchar(200)   not null,
    approval_comment     varchar(500)   null,
    material_comment     varchar(500)   null,
    attachment_urls      text           null,
    reason               text           null,
    department_id        bigint         null comment '院系ID',
    department_name      varchar(100)   null comment '院系名称'
);

create table if not exists consultation_question
(
    deleted              bit          not null,
    satisfaction_score   int          null,
    transferred_to_human bit          not null,
    create_time          datetime(6)  not null,
    id                   bigint auto_increment
        primary key,
    update_time          datetime(6)  null,
    user_id              bigint       not null,
    answer_source        varchar(20)  null,
    question_type        varchar(20)  not null,
    status               varchar(20)  not null,
    category             varchar(50)  null,
    image_url            varchar(500) null,
    transfer_reason      varchar(500) null,
    voice_url            varchar(500) null,
    ai_answer            text         null,
    question_text        text         null,
    session_id           varchar(64)  null comment '会话ID',
    file_urls            text         null comment '附件文件URL列表(JSON)',
    conversation_id      varchar(64)  null comment 'Dify对话ID',
    is_answering         bit          null comment 'AI是否正在回答中'
);

create table if not exists human_transfer
(
    deleted         bit          not null,
    create_time     datetime(6)  not null,
    id              bigint auto_increment
        primary key,
    process_time    datetime(6)  null,
    question_id     bigint       null comment '咨询问题ID',
    staff_id        bigint       null,
    update_time     datetime(6)  null,
    user_id         bigint       not null,
    status          varchar(20)  not null,
    transfer_type   varchar(20)  not null,
    transfer_reason varchar(500) null,
    staff_reply     text         null,
    question_text   text         null comment '用户填写的转接问题描述',
    question_type   varchar(50)  null comment '问题类型',
    file_urls       text         null comment '附件URL列表'
);

create table if not exists human_transfer_config
(
    id            bigint auto_increment
        primary key,
    create_time   datetime(6)   not null,
    deleted       bit           not null,
    update_time   datetime(6)   null,
    assign_mode   varchar(20)   not null comment '分配模式：USER/ROLE',
    business_type varchar(50)   not null comment '业务分类',
    enabled       bit           not null comment '是否启用',
    priority      int           not null comment '优先级',
    remark        varchar(500)  null comment '备注',
    role_id       bigint        null comment '目标角色ID',
    user_id       bigint        null comment '目标用户ID',
    user_ids      varchar(1000) null comment '目标用户ID列表，逗号分隔'
)
    comment '人工转接配置表';

create table if not exists leave_application
(
    cancelled               bit          not null,
    days                    int          not null,
    deleted                 bit          not null,
    end_date                date         not null,
    start_date              date         not null,
    applicant_id            bigint       not null,
    approval_time           datetime(6)  null,
    approver_id             bigint       null,
    cancel_time             datetime(6)  null,
    create_time             datetime(6)  not null,
    id                      bigint auto_increment
        primary key,
    update_time             datetime(6)  null,
    approval_status         varchar(20)  not null,
    grade                   varchar(20)  null,
    leave_slip_status       varchar(20)  null,
    leave_type              varchar(20)  not null,
    class_name              varchar(50)  null,
    student_name            varchar(50)  null,
    department              varchar(100) null,
    approval_comment        varchar(500) null,
    attachment_url          varchar(500) null,
    leave_slip_url          varchar(500) null,
    reason                  text         null,
    department_code         varchar(50)  null comment '院系代码',
    department_id           varchar(50)  null comment '院系名称',
    cancel_approval_comment varchar(500) null comment '销假审批意见',
    cancel_approval_status  varchar(20)  null comment '销假审批状态',
    cancel_approval_time    datetime(6)  null comment '销假审批时间',
    cancel_requested        bit          not null comment '是否已申请销假'
);

create table if not exists notification
(
    deleted        bit          not null,
    is_read        bit          not null,
    business_id    bigint       null,
    create_time    datetime(6)  not null,
    id             bigint auto_increment
        primary key,
    read_time      datetime(6)  null,
    update_time    datetime(6)  null,
    user_id        bigint       not null,
    channel        varchar(20)  null,
    business_type  varchar(50)  null,
    type           varchar(50)  not null,
    title          varchar(200) not null,
    channel_status varchar(500) null,
    content        text         null
);

create table if not exists power
(
    id         bigint       not null comment '主键'
        primary key,
    created_at datetime(6)  null,
    level      int          not null comment '等级',
    pid        int          not null comment '父级id',
    power_name varchar(255) not null comment '权限名称',
    power_url  varchar(255) not null comment '权限地址',
    updated_at datetime(6)  null
);

create table if not exists role
(
    id         bigint       not null comment '主键'
        primary key,
    created_at datetime(6)  null,
    page_path  varchar(255) null,
    power_id   varchar(255) null comment '权限id',
    role_name  varchar(20)  not null comment '角色名称',
    updated_at datetime(6)  null
);

create table if not exists teacher
(
    id          bigint auto_increment comment '主键'
        primary key,
    card_number varchar(30) null comment '学工号',
    create_time int         not null comment '创建时间',
    name        varchar(50) not null comment '老师姓名',
    phone       varchar(20) not null comment '联系电话',
    state       int         not null comment '状态（0：关闭 1：开启）',
    update_time int         not null comment '更新时间'
);

