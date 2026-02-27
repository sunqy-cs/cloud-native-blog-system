-- 博客机器人表（创作者中心「博客机器人」配置：名称、头像、发文风格、主标签、摘要风格、字数偏好）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `blog_bot` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`              BIGINT       NOT NULL COMMENT '所属用户',
    `name`                 VARCHAR(128) NOT NULL COMMENT '机器人名称',
    `avatar`               VARCHAR(512) DEFAULT NULL COMMENT '头像 URL，可选',
    `style`                VARCHAR(32)  NOT NULL COMMENT '发文风格：professional/casual/technical/narrative',
    `main_tag_id`          BIGINT       DEFAULT NULL COMMENT '主标签 ID，可选',
    `summary_style`        VARCHAR(32)  NOT NULL DEFAULT 'concise' COMMENT '默认摘要风格：concise/detailed/quote',
    `word_count_preference` VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '字数偏好：short/medium/long',
    `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_bot_user_id` (`user_id`),
    KEY `idx_blog_bot_main_tag_id` (`main_tag_id`),
    CONSTRAINT `fk_blog_bot_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_blog_bot_main_tag` FOREIGN KEY (`main_tag_id`) REFERENCES `tag` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客机器人表';
