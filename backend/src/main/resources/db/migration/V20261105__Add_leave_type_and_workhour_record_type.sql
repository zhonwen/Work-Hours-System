-- 轻量请假：请假类型字典 + 工时表扩展记录类型
-- 正常工时仍关联项目；请假不选项目/任务，不计入项目工时统计

-- 1. 请假类型字典
CREATE TABLE IF NOT EXISTS `gsms_leave_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '请假类型ID',
  `name` VARCHAR(50) NOT NULL COMMENT '类型名称，如病假/年假',
  `code` VARCHAR(50) NOT NULL COMMENT '类型编码',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_user_id` BIGINT NOT NULL COMMENT '创建人ID',
  `update_user_id` BIGINT NOT NULL COMMENT '更新人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leave_type_code` (`code`),
  KEY `idx_leave_type_status` (`status`),
  KEY `idx_leave_type_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假类型字典';

INSERT INTO `gsms_leave_type` (`name`, `code`, `sort`, `status`, `remark`, `create_user_id`, `update_user_id`)
VALUES
('病假', 'SICK', 1, 1, '因病请假', 1, 1),
('年假', 'ANNUAL', 2, 1, '带薪年休假', 1, 1),
('事假', 'PERSONAL', 3, 1, '因私事请假', 1, 1),
('调休', 'COMPENSATORY', 4, 1, '加班调休', 1, 1);

-- 2. 工时表：记录类型 + 请假类型；项目可空（请假）
ALTER TABLE `gsms_work_hour` DROP FOREIGN KEY `fk_work_hour_project`;

ALTER TABLE `gsms_work_hour`
  MODIFY COLUMN `project_id` BIGINT DEFAULT NULL COMMENT '项目ID（请假可为空）',
  ADD COLUMN `record_type` TINYINT NOT NULL DEFAULT 1 COMMENT '记录类型 1:正常工时 2:请假' AFTER `content`,
  ADD COLUMN `leave_type_id` BIGINT DEFAULT NULL COMMENT '请假类型ID' AFTER `record_type`;

ALTER TABLE `gsms_work_hour`
  ADD CONSTRAINT `fk_work_hour_project`
    FOREIGN KEY (`project_id`) REFERENCES `gsms_project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_work_hour_leave_type`
    FOREIGN KEY (`leave_type_id`) REFERENCES `gsms_leave_type` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD KEY `idx_workhour_record_type` (`record_type`),
  ADD KEY `idx_workhour_leave_type` (`leave_type_id`);

-- 3. 菜单与权限（系统管理 - 请假类型）
-- 不写 permission_type，兼容该列尚未创建的库（版本号排序问题）
INSERT IGNORE INTO `sys_permission`
(`name`, `code`, `description`, `create_user_id`, `update_user_id`)
VALUES
('访问请假类型', 'MENU_LEAVE_TYPE', '可访问请假类型字典管理', 1, 1);

INSERT INTO `sys_menu`
(`id`, `name`, `path`, `component`, `icon`, `parent_id`, `sort`, `type`, `status`, `visible`, `create_user_id`, `update_user_id`)
VALUES
(18, '请假类型', '/system/leave-types', 'LeaveTypeList', NULL, 6, 8, 2, 1, 1, 1, 1);

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `permission_id`, `update_user_id`)
SELECT 18, id, 1 FROM `sys_permission` WHERE `code` = 'MENU_LEAVE_TYPE';

-- 给已有系统管理权限的角色补齐请假类型菜单权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`)
SELECT DISTINCT rp.role_id, p.id, NOW()
FROM `sys_role_permission` rp
INNER JOIN `sys_permission` sp ON sp.id = rp.permission_id AND sp.code = 'MENU_SYSTEM' AND rp.is_deleted = 0
INNER JOIN `sys_permission` p ON p.code = 'MENU_LEAVE_TYPE'
WHERE rp.is_deleted = 0;
