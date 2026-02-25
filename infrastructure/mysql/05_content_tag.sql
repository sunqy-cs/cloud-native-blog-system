-- 内容-标签中间表（一篇博客可有多个标签）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content_tag` (
    `content_id` BIGINT NOT NULL COMMENT '内容 ID',
    `tag_id`     BIGINT NOT NULL COMMENT '标签 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (`content_id`, `tag_id`),
    KEY `idx_content_tag_tag_id` (`tag_id`),
    CONSTRAINT `fk_content_tag_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容-标签关联表';
