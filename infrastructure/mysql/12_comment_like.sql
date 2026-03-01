-- 评论点赞表（用户-评论，用于评论点赞数与「当前用户是否已点赞」）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '点赞用户',
    `comment_id` BIGINT   NOT NULL COMMENT '被点赞的评论',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_like_user_comment` (`user_id`, `comment_id`),
    KEY `idx_comment_like_comment_id` (`comment_id`),
    CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';
