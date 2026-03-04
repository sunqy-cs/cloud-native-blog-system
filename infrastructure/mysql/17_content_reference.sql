-- 双链笔记：笔记之间的引用（仅知识库类型内容参与）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `content_reference` (
    `source_content_id` BIGINT NOT NULL COMMENT '引用方内容 ID（含该链接的笔记）',
    `target_content_id` BIGINT NOT NULL COMMENT '被引用方内容 ID',
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`source_content_id`, `target_content_id`),
    KEY `idx_content_ref_target` (`target_content_id`),
    CONSTRAINT `fk_content_ref_source` FOREIGN KEY (`source_content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_ref_target` FOREIGN KEY (`target_content_id`) REFERENCES `content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容引用表（双链，仅知识库）';
