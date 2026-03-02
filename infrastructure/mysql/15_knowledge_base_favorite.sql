-- 知识库-用户 收藏关系表
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `knowledge_base_favorite` (
    `user_id`           BIGINT   NOT NULL COMMENT '用户 ID（收藏者）',
    `knowledge_base_id` BIGINT   NOT NULL COMMENT '知识库 ID',
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`user_id`, `knowledge_base_id`),
    KEY `idx_kb_favorite_kb_id` (`knowledge_base_id`),
    CONSTRAINT `fk_kb_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kb_favorite_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库收藏表（用户收藏知识库）';
