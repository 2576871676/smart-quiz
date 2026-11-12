-- =============================================================
-- 智能出题与批改系统 数据库脚本
-- 数据库: smart_quiz   字符集: utf8mb4
-- 说明: 所有业务表统一含 deleted 逻辑删除列，与 MyBatis-Plus 配置一致
-- =============================================================

CREATE DATABASE IF NOT EXISTS smart_quiz DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_quiz;

-- 1. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色:0-学生,1-管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-正常',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删,1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 题目表
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `user_id`         BIGINT       NOT NULL COMMENT '创建用户ID',
    `subject`         VARCHAR(50)  DEFAULT NULL COMMENT '科目',
    `knowledge_point` VARCHAR(100) DEFAULT NULL COMMENT '知识点',
    `type`            TINYINT      DEFAULT NULL COMMENT '题型:1单选2多选3填空4简答',
    `difficulty`      TINYINT      DEFAULT NULL COMMENT '难度:1简单2中等3困难',
    `title`           TEXT         COMMENT '题干',
    `options`         TEXT         COMMENT '选项(JSON)',
    `answer`          TEXT         COMMENT '参考答案',
    `analysis`        TEXT         COMMENT '答案解析',
    `score`           INT          DEFAULT 5 COMMENT '分值',
    `source`          TINYINT      DEFAULT 1 COMMENT '来源:1-AI生成,2-手动录入',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 3. 试卷表
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    `user_id`        BIGINT       NOT NULL COMMENT '创建用户ID',
    `title`          VARCHAR(200) NOT NULL COMMENT '试卷标题',
    `subject`        VARCHAR(50)  DEFAULT NULL COMMENT '科目',
    `description`    VARCHAR(500) DEFAULT NULL COMMENT '试卷描述',
    `total_score`    INT          DEFAULT 0 COMMENT '总分',
    `question_count` INT          DEFAULT 0 COMMENT '题目数量',
    `duration`       INT          DEFAULT 60 COMMENT '考试时长(分钟)',
    `status`         TINYINT      DEFAULT 0 COMMENT '状态:0-草稿,1-已发布',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 4. 试卷-题目关联表
DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `paper_id`    BIGINT   NOT NULL COMMENT '试卷ID',
    `question_id` BIGINT   NOT NULL COMMENT '题目ID',
    `sort`        INT      DEFAULT 0 COMMENT '题目顺序',
    `score`       INT      DEFAULT 5 COMMENT '本题分值',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 5. 答题记录表
DROP TABLE IF EXISTS `answer_record`;
CREATE TABLE `answer_record` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `paper_id`    BIGINT   DEFAULT NULL COMMENT '试卷ID(单题练习为空)',
    `question_id` BIGINT   NOT NULL COMMENT '题目ID',
    `user_answer` TEXT     COMMENT '用户答案',
    `is_correct`  TINYINT  DEFAULT NULL COMMENT '是否正确:0错1对,null未判分',
    `score`       INT      DEFAULT 0 COMMENT '得分',
    `ai_comment`  TEXT     COMMENT 'AI批改评语',
    `duration`    INT      DEFAULT 0 COMMENT '答题用时(秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- 6. 考试记录表
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `paper_id`      BIGINT   NOT NULL COMMENT '试卷ID',
    `total_score`   INT      DEFAULT 0 COMMENT '总得分',
    `correct_count` INT      DEFAULT 0 COMMENT '答对题数',
    `wrong_count`   INT      DEFAULT 0 COMMENT '答错题数',
    `duration`      INT      DEFAULT 0 COMMENT '考试用时(秒)',
    `status`        TINYINT  DEFAULT 0 COMMENT '状态:0进行中1已提交2已批改',
    `start_time`    DATETIME DEFAULT NULL COMMENT '开始时间',
    `submit_time`   DATETIME DEFAULT NULL COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 7. 错题本表
DROP TABLE IF EXISTS `wrong_question`;
CREATE TABLE `wrong_question` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `question_id`     BIGINT   NOT NULL COMMENT '题目ID',
    `wrong_count`     INT      DEFAULT 1 COMMENT '错误次数',
    `last_wrong_time` DATETIME DEFAULT NULL COMMENT '最近错误时间',
    `mastered`        TINYINT  DEFAULT 0 COMMENT '是否已掌握:0未掌握1已掌握',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- 8. 学习计划表
DROP TABLE IF EXISTS `study_plan`;
CREATE TABLE `study_plan` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '计划标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '计划描述',
    `subject`     VARCHAR(50)  DEFAULT NULL COMMENT '科目',
    `target_date` DATE         DEFAULT NULL COMMENT '目标日期',
    `daily_goal`  VARCHAR(200) DEFAULT NULL COMMENT '每日目标',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态:0已结束1进行中',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- 9. 学习打卡记录表
DROP TABLE IF EXISTS `study_checkin`;
CREATE TABLE `study_checkin` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
    `plan_id`        BIGINT   DEFAULT NULL COMMENT '关联计划ID',
    `study_date`     DATE     DEFAULT NULL COMMENT '学习日期',
    `study_duration` INT      DEFAULT 0 COMMENT '学习时长(分钟)',
    `content`        VARCHAR(500) DEFAULT NULL COMMENT '学习内容',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `study_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习打卡记录表';

-- 10. AI对话记录表
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `session_id`  VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
    `role`        VARCHAR(20) DEFAULT NULL COMMENT '角色:user/assistant',
    `content`     TEXT     COMMENT '消息内容',
    `tokens`      INT      DEFAULT 0 COMMENT '消耗token数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- 11. 系统公告表
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content`     TEXT         COMMENT '公告内容',
    `type`        TINYINT      DEFAULT 0 COMMENT '类型:0普通公告1系统通知',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态:0下架1发布',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 12. 操作日志表（AOP 切面写入）
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(50)  DEFAULT NULL COMMENT '模块标题',
    `business_type`  VARCHAR(20)  DEFAULT NULL COMMENT '业务类型',
    `method`         VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `request_method` VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
    `oper_url`       VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    `oper_ip`        VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `oper_name`      VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    `oper_param`     TEXT         COMMENT '请求参数',
    `json_result`    TEXT         COMMENT '返回结果',
    `status`         TINYINT      DEFAULT 0 COMMENT '操作状态:0正常1异常',
    `error_msg`      TEXT         COMMENT '错误消息',
    `cost_time`      BIGINT       DEFAULT 0 COMMENT '消耗时间(ms)',
    `oper_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始化管理员账号（密码为 BCrypt 加密的 "admin123"，实际使用时请重新生成）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `status`)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 1, 1);
