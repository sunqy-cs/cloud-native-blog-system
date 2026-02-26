-- 评论表（用户-内容关联，评论数在 content 表冗余存储便于展示）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `comment` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       NOT NULL COMMENT '评论用户',
    `content_id` BIGINT       NOT NULL COMMENT '所属内容/博客',
    `body`       TEXT         NOT NULL COMMENT '评论内容',
    `parent_id`  BIGINT       DEFAULT NULL COMMENT '父评论 ID（回复时）',
    `is_hot`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否热评：0-否 1-是（作者推荐）',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_content_id` (`content_id`),
    KEY `idx_comment_user_id` (`user_id`),
    KEY `idx_comment_parent_id` (`parent_id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
