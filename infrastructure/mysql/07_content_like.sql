-- 点赞中间表（用户-博客，点赞数由此表统计）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content_like` (
    `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
    `content_id` BIGINT   NOT NULL COMMENT '内容 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`user_id`, `content_id`),
    KEY `idx_content_like_content_id` (`content_id`),
    CONSTRAINT `fk_content_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_like_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容点赞表';
