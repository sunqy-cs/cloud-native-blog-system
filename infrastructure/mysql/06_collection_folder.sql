-- 收藏夹表（用户可创建多个收藏夹，其中之一为默认收藏夹）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `collection_folder` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `name`        VARCHAR(64)  NOT NULL COMMENT '收藏夹名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '收藏夹简介',
    `is_default`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认收藏夹：0-否 1-是（每用户仅一个）',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_collection_folder_user_id` (`user_id`),
    CONSTRAINT `fk_collection_folder_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏夹表';

-- 新用户注册时自动创建默认收藏夹（每用户仅一个）
DROP TRIGGER IF EXISTS `tr_user_after_insert_default_folder`;
DELIMITER ;;
CREATE TRIGGER `tr_user_after_insert_default_folder`
AFTER INSERT ON `user`
FOR EACH ROW
BEGIN
    INSERT INTO `collection_folder` (`user_id`, `name`, `is_default`)
    VALUES (NEW.id, '默认收藏夹', 1);
END;;
DELIMITER ;
