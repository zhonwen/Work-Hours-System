-- 项目公告 / 规划板（单块 Markdown，上限 5000 字由应用层校验）
ALTER TABLE `gsms_project`
    ADD COLUMN `announcement` TEXT NULL COMMENT '项目公告Markdown内容' AFTER `description`,
    ADD COLUMN `announcement_update_user_id` BIGINT NULL COMMENT '公告最后更新人ID' AFTER `announcement`,
    ADD COLUMN `announcement_update_time` DATETIME NULL COMMENT '公告最后更新时间' AFTER `announcement_update_user_id`;
