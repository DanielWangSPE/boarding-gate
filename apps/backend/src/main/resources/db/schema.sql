-- ============================================================
-- Boarding Gate - 认证与会话管理 数据库 DDL
-- MySQL 8.0（兼容 5.7+），默认字符集 utf8mb4
-- 对应设计文档：
--   design-docs/coding/01-认证与会话管理/01-01-登录认证.spec.md
--   design-docs/coding/01-认证与会话管理/01-03-会话管理.md
-- ============================================================

CREATE DATABASE IF NOT EXISTS `boarding_gate`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `boarding_gate`;

-- ------------------------------------------------------------
-- 系统用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`              VARCHAR(64)     NOT NULL                COMMENT '用户名（全局唯一）',
    `password_hash`         VARCHAR(100)    NOT NULL                COMMENT '密码 BCrypt 哈希',
    `real_name`             VARCHAR(64)              DEFAULT NULL   COMMENT '真实姓名',
    `nickname`              VARCHAR(64)              DEFAULT NULL   COMMENT '昵称',
    `email`                 VARCHAR(128)             DEFAULT NULL   COMMENT '邮箱',
    `mobile`                VARCHAR(32)              DEFAULT NULL   COMMENT '手机号',
    `avatar`                VARCHAR(255)             DEFAULT NULL   COMMENT '头像 URL',
    `status`                TINYINT         NOT NULL DEFAULT 1      COMMENT '账号状态：1-启用 0-停用',
    `force_change_password` TINYINT         NOT NULL DEFAULT 0      COMMENT '是否需要强制修改密码：1-是 0-否',
    `last_login_time`       DATETIME                 DEFAULT NULL   COMMENT '最近登录时间',
    `last_login_ip`         VARCHAR(64)              DEFAULT NULL   COMMENT '最近登录 IP',
    `password_update_time`  DATETIME                 DEFAULT NULL   COMMENT '密码最近修改时间',
    `create_by`             VARCHAR(64)              DEFAULT NULL   COMMENT '创建人',
    `create_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_by`             VARCHAR(64)              DEFAULT NULL   COMMENT '更新人',
    `update_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：1-已删 0-未删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_mobile` (`mobile`),
    KEY `idx_sys_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ------------------------------------------------------------
-- 会话表（服务端会话元数据，可用于令牌吊销）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_session`;
CREATE TABLE `sys_session` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id`            VARCHAR(64)     NOT NULL                COMMENT '会话 ID（UUID）',
    `user_id`               BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
    `username`              VARCHAR(64)     NOT NULL                COMMENT '用户名冗余，便于排查',
    `access_jti`            VARCHAR(64)              DEFAULT NULL   COMMENT '当前 Access Token 的 jti',
    `refresh_jti`           VARCHAR(64)              DEFAULT NULL   COMMENT '当前 Refresh Token 的 jti',
    `login_ip`              VARCHAR(64)              DEFAULT NULL   COMMENT '登录 IP',
    `user_agent`            VARCHAR(512)             DEFAULT NULL   COMMENT 'User-Agent',
    `status`                TINYINT         NOT NULL DEFAULT 1      COMMENT '会话状态：1-有效 0-已登出/失效',
    `expires_at`            DATETIME                 DEFAULT NULL   COMMENT 'Refresh 过期时刻',
    `create_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_session_session_id` (`session_id`),
    KEY `idx_sys_session_user_id` (`user_id`),
    KEY `idx_sys_session_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统会话';

-- ------------------------------------------------------------
-- 登录日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(64)     NOT NULL                COMMENT '登录用户名（成功或尝试）',
    `user_id`       BIGINT UNSIGNED          DEFAULT NULL   COMMENT '用户 ID（失败时可为空）',
    `login_time`    DATETIME        NOT NULL                COMMENT '登录时间',
    `login_ip`      VARCHAR(64)              DEFAULT NULL   COMMENT '登录 IP',
    `user_agent`    VARCHAR(512)             DEFAULT NULL   COMMENT 'User-Agent',
    `login_result`  TINYINT         NOT NULL                COMMENT '登录结果：1-成功 0-失败',
    `fail_reason`   VARCHAR(128)             DEFAULT NULL   COMMENT '失败原因（业务 code 或简短说明）',
    `session_id`    VARCHAR(64)              DEFAULT NULL   COMMENT '会话 ID（成功时）',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_login_log_username` (`username`),
    KEY `idx_sys_login_log_login_time` (`login_time`),
    KEY `idx_sys_login_log_result` (`login_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
