-- 知识库-内容 收录关系表（知识库收录了哪些 content）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `knowledge_base_content` (
    `knowledge_base_id` BIGINT   NOT NULL COMMENT '知识库 ID',
    `content_id`       BIGINT   NOT NULL COMMENT '内容 ID',
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收录时间',
    PRIMARY KEY (`knowledge_base_id`, `content_id`),
    KEY `idx_kb_content_content_id` (`content_id`),
    CONSTRAINT `fk_kb_content_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kb_content_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库收录表（知识库与内容多对多）';
