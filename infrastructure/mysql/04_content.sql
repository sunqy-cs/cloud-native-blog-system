-- 博客/内容表（可为普通博客或知识库；点赞/收藏/浏览数冗余存储便于展示，与中间表可同步）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT       NOT NULL COMMENT '作者',
    `type`             VARCHAR(32)  NOT NULL DEFAULT 'BLOG' COMMENT '类型：BLOG-博客 / KNOWLEDGE-知识库',
    `column_id`        BIGINT       DEFAULT NULL COMMENT '所属专栏（可为空）',
    `title`            VARCHAR(256) NOT NULL COMMENT '标题',
    `summary`          VARCHAR(512) DEFAULT NULL COMMENT '摘要',
    `body`             LONGTEXT     DEFAULT NULL COMMENT '正文',
    `cover`            VARCHAR(512) DEFAULT NULL COMMENT '封面图 URL',
    `status`           VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿 / PUBLISHED-已发布',
    `article_type`     VARCHAR(32)  DEFAULT 'ORIGINAL' COMMENT '文章类型：ORIGINAL-原创 / REPRINT-转载 / TRANSLATED-翻译',
    `creation_statement` VARCHAR(64) DEFAULT NULL COMMENT '创作声明：none/ai-assisted/network/personal',
    `visibility`       VARCHAR(32)  NOT NULL DEFAULT 'ALL' COMMENT '可见范围：ALL-全部可见 / SELF-仅我可见 / FANS-粉丝可见',
    `like_count`       INT          NOT NULL DEFAULT 0 COMMENT '点赞数（与 content_like 可同步）',
    `collection_count` INT          NOT NULL DEFAULT 0 COMMENT '收藏数（与 content_collection 可同步）',
    `view_count`       INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    `comment_count`    INT          NOT NULL DEFAULT 0 COMMENT '评论数（与 comment 表可同步）',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_content_user_id` (`user_id`),
    KEY `idx_content_column_id` (`column_id`),
    KEY `idx_content_type` (`type`),
    KEY `idx_content_status` (`status`),
    KEY `idx_content_created_at` (`created_at`),
    CONSTRAINT `fk_content_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_column` FOREIGN KEY (`column_id`) REFERENCES `column` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客/内容表';
