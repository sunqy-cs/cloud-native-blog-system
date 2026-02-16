-- 建库，字符集 utf8mb4 支持中文及 emoji
-- 初始化时让执行脚本的客户端连接使用 utf8mb4，避免中文乱码
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog;
