-- 用户表（user-service 使用）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`     VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `nickname`     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar`       VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `cover`        VARCHAR(512) DEFAULT NULL COMMENT '背景图/封面 URL',
    `gender`       VARCHAR(16)  DEFAULT NULL COMMENT '性别',
    `intro`        VARCHAR(256) DEFAULT NULL COMMENT '一句话介绍',
    `residence`    VARCHAR(128) DEFAULT NULL COMMENT '居住地',
    `industry`     VARCHAR(64)  DEFAULT NULL COMMENT '所在行业',
    `bio`          TEXT         DEFAULT NULL COMMENT '个人简介',
    `account_quota` DECIMAL(12,2) DEFAULT 0 COMMENT '账户额度',
    `wechat_id`    VARCHAR(64)  DEFAULT NULL COMMENT '微信 ID',
    `role`         VARCHAR(32)  DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
