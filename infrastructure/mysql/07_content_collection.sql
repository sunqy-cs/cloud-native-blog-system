-- 收藏中间表（用户-博客，收藏数由此表统计）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content_collection` (
    `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
    `content_id` BIGINT   NOT NULL COMMENT '内容 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`user_id`, `content_id`),
    KEY `idx_content_collection_content_id` (`content_id`),
    CONSTRAINT `fk_content_collection_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_collection_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容收藏表';
