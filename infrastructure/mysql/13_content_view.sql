-- 阅读中间表（用户-内容，同一用户对同一文章只计一次阅读，用于去重增加 view_count）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content_view` (
    `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
    `content_id` BIGINT   NOT NULL COMMENT '内容 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`user_id`, `content_id`),
    KEY `idx_content_view_content_id` (`content_id`),
    CONSTRAINT `fk_content_view_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_view_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容阅读记录表（去重用）';
