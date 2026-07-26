create database if not exists oj;
drop database oj;
-- auto-generated definition
create table question_answer
(
    id            bigint auto_increment comment 'id'
        primary key,
    questionId    bigint                             not null comment '题目 id',
    userId        bigint                             not null comment '提交用户 id',
    language      varchar(32)                        not null comment '编程语言',
    content       text                               not null comment '答案内容',
    reviewStatus  int      default 0                 not null comment '状态：0-待审核, 1-通过, 2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息（如拒绝原因）',
    reviewerId    bigint                             null comment '审核人 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '题目答案' collate = utf8mb4_unicode_ci;

create index idx_questionId
    on question_answer (questionId);

-- auto-generated definition
create table question_base
(
    id          bigint auto_increment comment 'id'
        primary key,
    number      varchar(32)                           not null comment '题目编号',
    title       varchar(64)                           null comment '题目名称',
    difficulty  varchar(16) default 'Easy'            not null comment '难度',
    submitNum   int         default 0                 not null comment '题目提交数',
    acceptedNum int         default 0                 not null comment '题目通过数',
    timeLimit   int                                   not null comment '时间限制',
    memoryLimit int                                   not null comment '空间限制',
    userId      bigint                                not null comment '创建用户 id',
    createTime  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint     default 0                 not null comment '是否删除',
    constraint unique_idx_number
        unique (number)
)
    comment '题目' collate = utf8mb4_unicode_ci;

-- auto-generated definition
create table question_extent
(
    id              bigint                             not null comment 'id'
        primary key,
    content         text                               null comment '内容',
    sampleJudgeCase text                               null comment '测试判题用例',
    hiddenJudgeCase text                               null comment '判题用例',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除'
)
    comment '题目扩展信息表' collate = utf8mb4_unicode_ci;

-- auto-generated definition
create table question_submit
(
    id          bigint auto_increment comment 'id'
        primary key,
    language    varchar(32)                        not null comment '编程语言',
    code        text                               not null comment '用户代码',
    message     varchar(64)                        null comment '测评信息',
    judgeInfo   text                               null comment '判题信息（json 对象）',
    status      int      default 0                 not null comment '判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）',
    questionId  bigint                             not null comment '题目 id',
    title       varchar(64)                        not null comment '题目标题',
    number      varchar(32)                        not null comment '题目编号',
    userId      bigint                             not null comment '创建用户 id',
    userAccount varchar(32)                        not null comment '用户名',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '题目提交';

create index idx_message_create
    on question_submit (message asc, createTime desc);

create index idx_question_create
    on question_submit (number asc, createTime desc);

create index idx_status_create
    on question_submit (status asc, createTime desc);

create index idx_user_create
    on question_submit (userAccount asc, createTime desc);

-- auto-generated definition
create table question_tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    name       varchar(16)                        null comment '标签名称',
    parentId   bigint   default 0                 not null comment '父标签Id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
)
    comment '题目标签' collate = utf8mb4_unicode_ci;

create index idx_parentId
    on question_tag (parentId);

-- auto-generated definition
create table question_tag_related
(
    id            bigint auto_increment comment 'id'
        primary key,
    questionId    bigint                             not null comment '题目 id',
    questionTagId bigint                             not null comment '题目标签 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '题目标签' collate = utf8mb4_unicode_ci;

create index idx_questionId
    on question_tag_related (questionId);

create index idx_questionTagId
    on question_tag_related (questionTagId);

-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(32)                            not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(64)                            null comment '用户昵称',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户' collate = utf8mb4_unicode_ci;

