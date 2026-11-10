-- ============================================
-- 工时页面权限拆分
-- 创建时间: 2026-10-16
-- 描述: 将工时日历、工时列表拆分为独立菜单权限，支持角色按页面配置访问范围
-- ============================================

-- 1. 新增工时页面菜单权限
-- 不写 permission_type：Flyway 按版本号排序时，本脚本可能早于 V202601154
--（该脚本才添加 permission_type）；列存在后由 V202601154 的 MENU_% 更新补齐。
INSERT IGNORE INTO `sys_permission`
(`name`, `code`, `description`, `create_user_id`, `update_user_id`)
VALUES
('访问工时日历', 'MENU_WORKHOUR_CALENDAR', '可访问工时日历页面', 1, 1),
('访问工时列表', 'MENU_WORKHOUR_LIST', '可访问工时列表页面', 1, 1);

-- 2. 移除子菜单对父级工时权限的直接绑定
DELETE mp
FROM `sys_menu_permission` mp
INNER JOIN `sys_permission` p ON p.id = mp.permission_id
WHERE mp.menu_id IN (7, 8)
  AND p.code = 'MENU_WORKHOUR';

-- 3. 为工时子页面绑定独立权限
INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `permission_id`, `update_user_id`)
SELECT 7, id, 1
FROM `sys_permission`
WHERE `code` = 'MENU_WORKHOUR_CALENDAR'
UNION ALL
SELECT 8, id, 1
FROM `sys_permission`
WHERE `code` = 'MENU_WORKHOUR_LIST';

-- 4. 为已有拥有工时模块权限的角色补齐页面权限，保证升级后访问范围不变
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`)
SELECT DISTINCT rp.role_id, child_permission.id, NOW()
FROM `sys_role_permission` rp
INNER JOIN `sys_permission` parent_permission
        ON parent_permission.id = rp.permission_id
       AND parent_permission.code = 'MENU_WORKHOUR'
INNER JOIN `sys_permission` child_permission
        ON child_permission.code IN ('MENU_WORKHOUR_CALENDAR', 'MENU_WORKHOUR_LIST')
WHERE rp.is_deleted = 0;

-- ============================================
-- 迁移完成
-- ============================================
-- 验证SQL:
-- SELECT p.code, p.name, p.permission_type
-- FROM sys_permission p
-- WHERE p.code IN ('MENU_WORKHOUR', 'MENU_WORKHOUR_CALENDAR', 'MENU_WORKHOUR_LIST');
--
-- SELECT m.name AS menu_name, p.code AS permission_code
-- FROM sys_menu_permission mp
-- INNER JOIN sys_menu m ON m.id = mp.menu_id
-- INNER JOIN sys_permission p ON p.id = mp.permission_id
-- WHERE mp.menu_id IN (7, 8)
-- ORDER BY mp.menu_id, p.code;
