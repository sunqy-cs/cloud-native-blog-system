-- 专栏表（一篇博客最多属于一个专栏）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `column` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `name`        VARCHAR(128) NOT NULL COMMENT '专栏名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '专栏描述',
    `cover`       VARCHAR(512) DEFAULT NULL COMMENT '封面图 URL',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_column_user_id` (`user_id`),
    CONSTRAINT `fk_column_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏表';
