-- 统一审核任务表（AI + 人工）：博客 / 公开知识库文档 / 评论 / 个人信息
-- 业务侧不重复建表；管理员提交不写入本表（由应用层过滤）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `moderation_task` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_type`        VARCHAR(32)  NOT NULL COMMENT 'ARTICLE, KNOWLEDGE_DOC, COMMENT, USER_PROFILE',
    `resource_id`          BIGINT       NOT NULL COMMENT 'content.id / comment.id / user.id',
    `owner_user_id`        BIGINT       NOT NULL COMMENT '提交者用户 ID',
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, NEEDS_HUMAN, APPROVED, REJECTED',
    `ai_decision`          VARCHAR(32)  DEFAULT NULL COMMENT 'PASS, REJECT, NEEDS_HUMAN',
    `ai_detail`            JSON         DEFAULT NULL COMMENT 'AI 输出：模型、评分、风险标签、摘要等',
    `ai_reviewed_at`       DATETIME     DEFAULT NULL COMMENT 'AI 完成时间',
    `human_reviewer_id`    BIGINT       DEFAULT NULL COMMENT '人工审核人 ID',
    `human_decision`       VARCHAR(32)  DEFAULT NULL COMMENT 'APPROVE, REJECT',
    `human_note`           VARCHAR(512) DEFAULT NULL COMMENT '人工备注',
    `human_reviewed_at`    DATETIME     DEFAULT NULL COMMENT '人工完成时间',
    `payload_snapshot`     JSON         DEFAULT NULL COMMENT '送审快照：标题/摘要/正文片段/资料字段等',
    `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moderation_resource` (`resource_type`, `resource_id`),
    KEY `idx_moderation_status_type` (`status`, `resource_type`),
    KEY `idx_moderation_owner` (`owner_user_id`),
    KEY `idx_moderation_created` (`created_at`),
    CONSTRAINT `fk_moderation_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一审核任务（AI+人工）';

-- 若从旧库升级且 04/09/01 未含新列，可取消下面注释执行（若列已存在会报错，可忽略或逐条执行）
-- ALTER TABLE `content` ADD COLUMN `moderation_status` VARCHAR(32) DEFAULT NULL COMMENT '审核：PENDING/NEEDS_HUMAN/APPROVED/REJECTED；NULL=历史或免审' AFTER `status`;
-- ALTER TABLE `comment` ADD COLUMN `moderation_status` VARCHAR(32) NOT NULL DEFAULT 'APPROVED' COMMENT '审核' AFTER `body`;
-- ALTER TABLE `user` ADD COLUMN `profile_moderation_status` VARCHAR(32) DEFAULT NULL COMMENT '资料审核' AFTER `bio`;
