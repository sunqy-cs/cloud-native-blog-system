-- 关注表（谁关注了谁：follower_id 关注 followee_id）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `follow` (
    `follower_id`  BIGINT   NOT NULL COMMENT '关注者用户 ID（谁在关注）',
    `followee_id`  BIGINT   NOT NULL COMMENT '被关注者用户 ID（关注了谁）',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`follower_id`, `followee_id`),
    KEY `idx_follow_followee_id` (`followee_id`),
    CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_follow_not_self` CHECK (`follower_id` != `followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';
