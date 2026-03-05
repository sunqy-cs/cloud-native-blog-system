-- RAG 问答：会话与消息表（ai-service 使用，存聊天记录）
SET NAMES utf8mb4;
USE blog;

-- 会话：每个会话关联一个用户、可选一个知识库
CREATE TABLE IF NOT EXISTS `rag_conversation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID',
    `kb_id`       BIGINT       DEFAULT NULL COMMENT '知识库 ID，NULL 表示不限定知识库',
    `title`      VARCHAR(256) DEFAULT NULL COMMENT '会话标题（如首条问题摘要）',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_rag_conversation_user_id` (`user_id`),
    KEY `idx_rag_conversation_kb_id` (`kb_id`),
    KEY `idx_rag_conversation_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 问答会话表';

-- 消息：user / assistant / system
CREATE TABLE IF NOT EXISTS `rag_message` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id`  BIGINT       NOT NULL COMMENT '会话 ID',
    `role`             VARCHAR(32)  NOT NULL COMMENT '角色：user / assistant / system',
    `content`          LONGTEXT     NOT NULL COMMENT '消息内容',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rag_message_conversation_id` (`conversation_id`),
    KEY `idx_rag_message_created_at` (`created_at`),
    CONSTRAINT `fk_rag_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `rag_conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 问答消息表';
