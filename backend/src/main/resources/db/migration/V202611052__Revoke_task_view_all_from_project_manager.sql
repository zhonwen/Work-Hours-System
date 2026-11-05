-- 项目经理仅管理「参与项目」范围内的数据，不应拥有全局 TASK_VIEW_ALL
-- 全局任务查看权限仅保留给 SYS_ADMIN / BUSINESS_MANAGER

UPDATE `sys_role_permission` rp
INNER JOIN `sys_role` r ON rp.role_id = r.id
INNER JOIN `sys_permission` p ON rp.permission_id = p.id
SET rp.is_deleted = 1
WHERE r.code = 'PROJECT_MANAGER'
  AND p.code = 'TASK_VIEW_ALL'
  AND IFNULL(rp.is_deleted, 0) = 0;
