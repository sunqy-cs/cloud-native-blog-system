-- 用户收件箱消息（系统通知、审核结果、互动提醒等）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `user_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL COMMENT '收件人用户 ID',
    `sender_user_id`  BIGINT       DEFAULT NULL COMMENT '发件人用户 ID；NULL 表示系统/平台',
    `title`           VARCHAR(256) NOT NULL COMMENT '标题',
    `body`            TEXT         DEFAULT NULL COMMENT '正文（可为纯文本或 HTML，由业务约定）',
    `msg_type`        VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：SYSTEM / AUDIT / INTERACTION / OTHER',
    `scene`           VARCHAR(64)  DEFAULT NULL COMMENT '业务场景：如 MODERATION_RESULT, COMMENT_REPLY',
    `extra`           JSON         DEFAULT NULL COMMENT '扩展：跳转链接、resource_id、业务类型等',
    `read_at`         DATETIME     DEFAULT NULL COMMENT '已读时间；NULL 表示未读',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_message_user` (`user_id`),
    KEY `idx_user_message_user_unread` (`user_id`, `read_at`),
    KEY `idx_user_message_created` (`created_at`),
    CONSTRAINT `fk_user_message_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_message_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收件箱消息';
