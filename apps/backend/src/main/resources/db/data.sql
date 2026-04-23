-- ============================================================
-- Boarding Gate - 测试数据（可重复执行：先清空再插入）
-- 账号：admin / 123456     （超级管理员，已启用）
-- 账号：jack  / 123456     （普通用户，已启用）
-- 账号：demo  / 123456     （已停用，用于验证 A0212）
-- 账号：reset / 123456     （force_change_password = 1，用于验证 A0220）
--
-- password_hash 均为 BCrypt("123456") 的有效哈希（$2a$10$...），
-- 可直接通过 Spring Security BCryptPasswordEncoder.matches 校验。
-- 如需重新生成，可使用 bcryptjs / Spring Security 的 BCryptPasswordEncoder 任一工具。
-- ============================================================

USE `boarding-gate`;

--  TRUNCATE 会重置 AUTO_INCREMENT，保证再次种入后 admin.id 仍为 1。
--  关闭外键检查以兼容后续如引入外键的情况（本库当前无外键，稳妥写法保留）。
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `sys_login_log`;
TRUNCATE TABLE `sys_session`;
TRUNCATE TABLE `sys_user`;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `sys_user`
    (`username`, `password_hash`, `real_name`, `nickname`, `email`, `status`, `force_change_password`, `create_by`)
VALUES
    ('admin', '$2a$10$ZUjy7M0EbrV5eNksycULSe7oe/cp7.T6SJyN/xzhzFH48J7Zb19mC', '超级管理员', 'Admin', 'admin@example.com', 1, 0, 'system'),
    ('jack',  '$2a$10$xeQ2Eu8NQFq6XfISLGSQteyttzU/lNg07er9RI22dyw78HVEo2qD6', '普通用户',   'Jack',  'jack@example.com',  1, 0, 'system'),
    ('demo',  '$2a$10$ERLCf/TJp1px4CXmgAJl1.ax7ji9untpu5BJ0gdUNr3ekNWWC716u', '停用演示',   'Demo',  'demo@example.com',  0, 0, 'system'),
    ('reset', '$2a$10$8LpszoOzy.dHs3UU/pENDuT7l90TLWbbvHaAttoTmk0Rk2pSkLDRy', '改密演示',   'Reset', 'reset@example.com', 1, 1, 'system');
