-- 标签表（博客与标签多对多，通过中间表关联）
SET NAMES utf8mb4;
USE blog;

CREATE TABLE IF NOT EXISTS `tag` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(64)  NOT NULL COMMENT '标签名',
    `is_main`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否主标签：0-否 1-是',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 若表已存在且无 is_main 列，可执行：ALTER TABLE `tag` ADD COLUMN `is_main` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主标签：0-否 1-是' AFTER `name`;

-- 插入 12 个主标签
INSERT IGNORE INTO `tag` (`name`, `is_main`) VALUES
('机器学习理论与优化', 1),
('深度学习架构与表示学习', 1),
('大模型与对齐（LLM/RLHF/DPO）', 1),
('多模态与生成模型（VLM/Diffusion）', 1),
('强化学习与决策（Online/Offline RL）', 1),
('自监督与迁移学习（SSL/Finetune）', 1),
('概率建模与贝叶斯方法', 1),
('图学习与结构化数据（GNN）', 1),
('因果推断与可解释性', 1),
('评测基准与实验方法（Benchmark/Ablation）', 1),
('系统与工程化（分布式训练/推理加速）', 1),
('其他', 1);
