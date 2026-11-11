-- 个人资料：头像路径 + 展示用部门文本（与 department_id 组织树解耦）
ALTER TABLE `sys_user`
    ADD COLUMN `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像：preset:userN 或 /uploads/avatars/...' AFTER `theme_id`,
    ADD COLUMN `department_display` VARCHAR(50) DEFAULT NULL COMMENT '个人资料部门（自由文本）' AFTER `avatar_url`;
