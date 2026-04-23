-- ============================================================
-- Boarding Gate - 测试数据
-- 账号：admin / 123456     （超级管理员，已启用）
-- 账号：jack  / 123456     （普通用户，已启用）
-- 账号：demo  / 123456     （已停用，用于验证 A0212）
-- 账号：reset / 123456     （force_change_password = 1，用于验证 A0220）
--
-- password_hash 为 BCrypt("123456") 之一的有效哈希，可直接通过 BCrypt.matches 校验。
-- ============================================================

USE `boarding_gate`;

INSERT INTO `sys_user`
    (`username`, `password_hash`, `real_name`, `nickname`, `email`, `status`, `force_change_password`, `create_by`)
VALUES
    ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'Admin', 'admin@example.com', 1, 0, 'system'),
    ('jack',  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '普通用户',   'Jack',  'jack@example.com',  1, 0, 'system'),
    ('demo',  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '停用演示',   'Demo',  'demo@example.com',  0, 0, 'system'),
    ('reset', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '改密演示',   'Reset', 'reset@example.com', 1, 1, 'system');
