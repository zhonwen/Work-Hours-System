-- 用户菜单/主题颜色偏好（个人设置）
ALTER TABLE `sys_user`
    ADD COLUMN `theme_id` VARCHAR(64) NOT NULL DEFAULT '默认白' COMMENT '菜单主题偏好ID' AFTER `password_reset_required`;
