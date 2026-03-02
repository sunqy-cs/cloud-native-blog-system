-- 知识库表（封面、简介、私有/公开可见性）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '创建者/所属用户',
    `name`        VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `cover`       VARCHAR(512) DEFAULT NULL COMMENT '封面图 URL',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '简介',
    `visibility`  VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC' COMMENT '可见性：PRIVATE-私有 / PUBLIC-公开',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_base_user_id` (`user_id`),
    KEY `idx_knowledge_base_visibility` (`visibility`),
    KEY `idx_knowledge_base_created_at` (`created_at`),
    CONSTRAINT `fk_knowledge_base_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
