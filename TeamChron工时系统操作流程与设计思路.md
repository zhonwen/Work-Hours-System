# TeamChron 工时系统操作流程与设计思路

## 一、问题分析

### 1.1 现象描述

**图1（admin账号）**：
- 角色：系统管理员
- 项目下拉框：显示"项目一"
- 状态：可以正常查看和选择项目

**图2（项目经理账号）**：
- 角色：项目经理
- 项目下拉框：显示"无数据"
- 状态：无法选择项目，无法填报工时

### 1.2 根本原因

根据代码分析，系统采用了**项目成员权限控制机制**：

#### 项目查询权限逻辑（ProjectServiceImpl.java）

```java
@Override
public PageResult<ProjectInfoResp> findAll(ProjectQueryReq req) {
    Long currentUserId = UserContext.getCurrentUserId();

    // 系统管理员和业务相关角色可以查看所有项目
    boolean canViewAllProjects = authService.canViewAllProjects(currentUserId);

    if (canViewAllProjects) {
        // 查看所有项目
        projects = projectMapper.selectByCondition(query);
    } else {
        // 普通用户只查询自己参与的项目
        query.setManagerId(currentUserId);
        projects = projectMapper.selectAccessibleProjectsByCondition(query);
    }
}
```

**权限判断**：
- ✅ **系统管理员**：`canViewAllProjects = true`，可以看到所有项目
- ❌ **普通用户/项目经理**：`canViewAllProjects = false`，只能看到自己参与的项目

#### 项目成员关联机制（ProjectServiceImpl.java）

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProjectInfoResp create(ProjectCreateReq createReq) {
    // ... 项目创建逻辑 ...

    // 将项目创建人加入项目成员，默认为项目管理员角色
    projectMemberMapper.insertProjectMember(project.getId(), currentUserId, 1, currentUserId);

    return ProjectInfoResp.from(project);
}
```

**关键点**：
- 项目创建时，**自动将创建人加入项目成员**
- 其他用户**不会自动加入**，需要手动添加

---

## 二、当前工作流程设计思路

### 2.1 项目成员管理流程

```
┌─────────────────────────────────────────────────────────────┐
│                     项目成员管理流程                          │
└─────────────────────────────────────────────────────────────┘

1. 项目创建
   ├─ 系统管理员/项目经理创建项目
   ├─ 自动将创建人加入项目成员（角色：项目管理员）
   └─ 项目状态：进行中

2. 添加项目成员
   ├─ 系统管理员：可以为任何项目添加成员
   ├─ 项目管理员：可以为所属项目添加成员
   └─ 操作路径：项目详情 → 成员管理 → 添加成员

3. 成员角色类型
   ├─ 1：项目管理员（可管理项目成员）
   ├─ 2：项目成员（仅参与项目）
   └─ 3：观察者（仅查看项目）

4. 权限控制
   ├─ 系统管理员：查看所有项目
   ├─ 项目管理员：查看参与的项目
   └─ 项目成员：查看参与的项目
```

### 2.2 工时填报流程

```
┌─────────────────────────────────────────────────────────────┐
│                     工时填报流程                               │
└─────────────────────────────────────────────────────────────┘

前置条件：用户必须是项目成员

1. 登录系统
   └─ 用户登录后，获取JWT Token

2. 进入工时管理页面
   └─ 系统调用 /api/projects 接口，返回用户可访问的项目列表

3. 选择项目
   ├─ 项目下拉框只显示用户参与的项目
   └─ 如果未参与任何项目，显示"无数据"

4. 填报工时
   ├─ 选择工作日期
   ├─ 选择项目（必须是用户参与的项目）
   ├─ 选择任务（可选）
   ├─ 输入工时数
   └─ 填写工作内容

5. 提交工时
   └─ 系统校验：用户是否为项目成员
```

### 2.3 权限控制设计

#### 三级权限控制

```
┌─────────────────────────────────────────────────────────────┐
│                     权限控制层级                               │
└─────────────────────────────────────────────────────────────┘

第一层：路由级权限（能否访问页面）
├─ 系统管理员：可访问所有页面
├─ 项目经理：可访问项目管理和工时管理
└─ 普通员工：仅可访问工时管理

第二层：按钮级权限（能否执行操作）
├─ 系统管理员：所有按钮可见
├─ 项目经理：项目管理相关按钮可见
└─ 普通员工：仅工时填报按钮可见

第三层：数据级权限（能否查看数据）
├─ 系统管理员：查看所有项目数据
├─ 项目管理员：查看参与的项目数据
└─ 普通员工：仅查看自己的工时数据
```

#### AuthService 权限检查

```java
// AuthService 提供的核心权限检查方法

// 1. 检查是否可以查看所有项目
boolean canViewAllProjects(Long userId)

// 2. 检查项目访问权限
void checkProjectAccess(Long userId, Long projectId)

// 3. 检查工时访问权限
void checkWorkHourAccess(Long userId, Long workHourUserId)

// 4. 检查任务访问权限
void checkTaskAccess(Long userId, Long taskId)

// 5. 检查角色
boolean hasRole(Long userId, String roleName)
```

---

## 三、完整操作流程

### 3.1 管理员操作流程

```
Step 1: 创建项目
├─ 登录系统（admin账号）
├─ 进入"项目管理"页面
├─ 点击"创建项目"
├─ 填写项目信息
│  ├─ 项目名称
│  ├─ 项目类型
│  ├─ 项目经理（选择用户）
│  └─ 项目描述
└─ 提交创建
   └─ 系统自动将创建人加入项目成员

Step 2: 添加项目成员
├─ 进入项目详情
├─ 点击"成员管理"
├─ 点击"添加成员"
├─ 选择用户
├─ 选择角色（项目管理员/项目成员/观察者）
└─ 提交添加

Step 3: 分配任务（可选）
├─ 进入项目详情
├─ 点击"任务管理"
├─ 创建任务
├─ 分配给具体成员
└─ 设置任务优先级和截止日期
```

### 3.2 项目经理操作流程

```
Step 1: 查看项目
├─ 登录系统（项目经理账号）
├─ 进入"项目管理"页面
└─ 查看自己参与的项目列表

Step 2: 管理项目成员
├─ 进入项目详情
├─ 点击"成员管理"
├─ 添加/移除成员
└─ 调整成员角色

Step 3: 填报工时
├─ 进入"工时管理"页面
├─ 选择项目（只能选择参与的项目）
├─ 填写工时信息
└─ 提交工时
```

### 3.3 普通员工操作流程

```
Step 1: 查看项目
├─ 登录系统（普通员工账号）
├─ 进入"工时管理"页面
└─ 查看可用的项目列表（必须是项目成员）

Step 2: 填报工时
├─ 选择工作日期
├─ 选择项目
├─ 选择任务（可选）
├─ 输入工时数
├─ 填写工作内容
└─ 提交工时

Step 3: 查看工时统计
├─ 查看今日工时
├─ 查看本周工时
├─ 查看本月工时
└─ 查看总工时
```

---

## 四、数据库设计

### 4.1 核心表结构

#### 项目成员表（gsms_project_member）

```sql
CREATE TABLE gsms_project_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_type TINYINT NOT NULL COMMENT '角色类型：1-项目管理员 2-项目成员 3-观察者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_user_id BIGINT COMMENT '更新人ID',
  is_deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-有效 1-已删除',
  UNIQUE KEY uk_project_user (project_id, user_id, is_deleted),
  KEY idx_project_id (project_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员关联表';
```

**设计说明**：
- `project_id` + `user_id` 唯一约束，防止重复添加
- `role_type` 定义成员在项目中的角色
- 软删除设计（`is_deleted`）

#### 项目表（gsms_project）

```sql
CREATE TABLE gsms_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  code VARCHAR(50) UNIQUE NOT NULL COMMENT '项目编码',
  name VARCHAR(100) NOT NULL COMMENT '项目名称',
  project_type TINYINT NOT NULL COMMENT '项目类型：1-常规型 2-中大型',
  status TINYINT NOT NULL COMMENT '项目状态：1-未开始 2-进行中 3-已暂停 4-已完成',
  manager_id BIGINT COMMENT '项目经理ID',
  description TEXT COMMENT '项目描述',
  start_date DATE COMMENT '计划开始日期',
  end_date DATE COMMENT '计划结束日期',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-有效 1-已删除',
  KEY idx_manager_id (manager_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';
```

### 4.2 关键查询SQL

#### 查询用户可访问的项目

```sql
-- 系统管理员：查询所有项目
SELECT * FROM gsms_project WHERE is_deleted = 0;

-- 普通用户：查询参与的项目
SELECT p.*
FROM gsms_project p
INNER JOIN gsms_project_member pm ON p.id = pm.project_id
WHERE pm.user_id = #{currentUserId}
  AND pm.is_deleted = 0
  AND p.is_deleted = 0;
```

#### 查询项目成员列表

```sql
SELECT
  pm.id,
  pm.project_id,
  pm.user_id,
  pm.role_type,
  u.username,
  u.nickname
FROM gsms_project_member pm
INNER JOIN sys_user u ON pm.user_id = u.id
WHERE pm.project_id = #{projectId}
  AND pm.is_deleted = 0;
```

---

## 五、前端实现细节

### 5.1 项目列表获取（WorkHourList.vue）

```typescript
// 获取项目列表
const fetchProjects = async () => {
  try {
    const res = await getProjectList({
      pageNum: 1,
      pageSize: 1000
    })
    projectList.value = res.list || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}
```

**API调用**：
- 前端调用 `/api/projects` 接口
- 后端根据当前用户权限返回可访问的项目列表
- 如果用户未参与任何项目，返回空列表

### 5.2 项目下拉框渲染

```vue
<el-select v-model="queryForm.projectId" placeholder="请选择项目" clearable filterable>
  <el-option
    v-for="project in projectList"
    :key="project.id"
    :label="project.name"
    :value="project.id"
  />
</el-select>
```

**显示逻辑**：
- 如果 `projectList` 为空，显示"无数据"
- 如果 `projectList` 有数据，显示项目列表

---

## 六、问题解决方案

### 6.1 当前问题

**问题描述**：
- 系统管理员创建项目后，其他用户无法看到项目
- 项目经理无法为项目添加工时

**根本原因**：
- 项目创建时只自动加入创建人
- 其他用户需要手动添加为项目成员

### 6.2 解决方案

#### 方案1：手动添加项目成员（当前方案）

**操作步骤**：
1. 系统管理员登录
2. 进入"项目管理" → 项目详情
3. 点击"成员管理"
4. 点击"添加成员"
5. 选择需要添加的用户
6. 选择角色（项目管理员/项目成员）
7. 提交添加

**优点**：
- 权限控制严格
- 数据安全性高
- 符合企业级应用规范

**缺点**：
- 操作步骤较多
- 需要管理员手动维护

#### 方案2：项目创建时选择参与成员（推荐改进）

**改进思路**：
在项目创建表单中增加"参与成员"选择功能

**实现步骤**：
1. 修改项目创建表单（ProjectCreate.vue）
2. 增加成员选择组件
3. 前端传递成员列表
4. 后端创建项目时批量添加成员

**伪代码**：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProjectInfoResp create(ProjectCreateReq createReq) {
    // ... 创建项目逻辑 ...

    // 添加项目成员
    if (createReq.getMemberIds() != null && !createReq.getMemberIds().isEmpty()) {
        for (Long memberId : createReq.getMemberIds()) {
            projectMemberMapper.insertProjectMember(
                project.getId(),
                memberId,
                2, // 默认角色：项目成员
                currentUserId
            );
        }
    }

    return ProjectInfoResp.from(project);
}
```

**优点**：
- 减少手动操作
- 一次性完成项目创建和成员分配
- 提升用户体验

**缺点**：
- 需要修改前端和后端代码
- 增加开发工作量

#### 方案3：按部门自动分配项目成员（高级方案）

**改进思路**：
根据项目所属部门，自动将该部门的所有员工加入项目成员

**实现步骤**：
1. 项目增加"所属部门"字段
2. 创建项目时，查询部门所有员工
3. 批量添加为项目成员

**优点**：
- 自动化程度高
- 减少手动操作
- 适合大型组织

**缺点**：
- 可能增加不必要的成员
- 需要部门管理功能支持

### 6.3 推荐方案

**短期方案**：使用方案1（手动添加成员）
- 快速解决问题
- 无需修改代码
- 符合当前系统设计

**长期方案**：实施方案2（创建时选择成员）
- 提升用户体验
- 减少管理员工作量
- 符合现代化产品设计

---

## 七、最佳实践建议

### 7.1 项目创建流程

```
建议的项目创建流程：

1. 系统管理员创建项目
   ├─ 填写项目基本信息
   ├─ 选择项目经理
   ├─ 选择参与成员（推荐改进）
   └─ 提交创建

2. 自动化操作
   ├─ 自动将创建人加入项目成员（项目管理员）
   ├─ 自动将选择的成员加入项目成员（项目成员）
   └─ 自动创建默认任务（可选）

3. 后续管理
   ├─ 项目经理可以继续添加成员
   ├─ 项目经理可以分配任务
   └─ 成员开始填报工时
```

### 7.2 权限管理建议

```
角色权限分配建议：

系统管理员（ADMIN）：
├─ 查看所有项目
├─ 创建/编辑/删除项目
├─ 管理所有项目成员
├─ 查看所有工时记录
└─ 系统配置管理

项目经理（PROJECT_MANAGER）：
├─ 查看参与的项目
├─ 创建项目（需要权限）
├─ 管理项目成员
├─ 分配任务
└─ 查看项目工时统计

项目成员（MEMBER）：
├─ 查看参与的项目
├─ 填报工时
├─ 编辑自己的工时
└─ 查看个人工时统计

观察者（OBSERVER）：
├─ 查看参与的项目
└─ 查看项目进度（只读）
```

### 7.3 工时管理建议

```
工时填报最佳实践：

1. 工时填报规范
   ├─ 每日填报工时
   ├─ 工时数精确到0.5小时
   ├─ 工作内容详细描述
   └─ 关联具体任务

2. 工时审核流程
   ├─ 提交工时
   ├─ 项目经理审核
   ├─ 驳回修改
   └─ 审核通过

3. 工时统计维度
   ├─ 个人维度：个人工时统计
   ├─ 项目维度：项目工时统计
   ├─ 部门维度：部门工时统计
   └─ 时间维度：日/周/月统计
```

---

## 八、总结

### 8.1 当前设计特点

✅ **优点**：
- 权限控制严格，数据安全性高
- 项目成员管理清晰
- 支持多角色权限
- 符合企业级应用规范

❌ **缺点**：
- 项目创建后需要手动添加成员
- 操作步骤较多
- 用户体验有待提升

### 8.2 改进方向

1. **项目创建时选择成员**
   - 减少手动操作
   - 提升用户体验

2. **批量导入成员**
   - 支持Excel导入
   - 适合大规模团队

3. **部门自动分配**
   - 根据部门自动分配项目成员
   - 提高管理效率

4. **项目模板**
   - 预设项目模板
   - 包含默认成员和任务
   - 快速创建项目

### 8.3 关键要点

1. **项目成员关联是必需的**
   - ✅ 用户必须先成为项目成员，才能看到项目
   - ✅ 只有项目成员才能为项目填报工时
   - ✅ 权限控制基于项目成员关系

2. **系统管理员权限最大**
   - ✅ 系统管理员可以查看所有项目
   - ✅ 系统管理员可以管理所有项目成员
   - ✅ 系统管理员不受项目成员限制

3. **项目经理权限受限**
   - ❌ 项目经理只能看到参与的项目
   - ❌ 项目经理需要被添加为项目成员
   - ❌ 项目经理只能管理参与的项目

4. **普通员工权限最小**
   - ❌ 普通员工只能看到参与的项目
   - ❌ 普通员工必须被添加为项目成员
   - ❌ 普通员工只能填报自己的工时

---

## 九、快速参考

### 9.1 常见问题

**Q1：为什么看不到项目？**
A：您需要先被添加为项目成员，请联系系统管理员或项目经理。

**Q2：如何添加项目成员？**
A：进入项目详情 → 成员管理 → 添加成员 → 选择用户 → 选择角色 → 提交。

**Q3：如何填报工时？**
A：进入工时管理页面 → 选择项目 → 填写工时信息 → 提交。

**Q4：系统管理员能看到所有项目吗？**
A：是的，系统管理员可以查看和管理所有项目。

**Q5：项目经理能看到所有项目吗？**
A：不能，项目经理只能看到自己参与的项目。

### 9.2 操作快捷方式

```
管理员：
项目管理 → 创建项目 → 成员管理 → 添加成员

项目经理：
项目管理 → 项目详情 → 成员管理 → 添加成员
工时管理 → 选择项目 → 填报工时

普通员工：
工时管理 → 选择项目 → 填报工时
```

---

## 十、附录

### 10.1 相关代码文件

**后端**：
- `ProjectServiceImpl.java` - 项目服务实现
- `ProjectMemberServiceImpl.java` - 项目成员服务实现
- `AuthServiceImpl.java` - 权限服务实现
- `WorkHourServiceImpl.java` - 工时服务实现

**前端**：
- `WorkHourList.vue` - 工时列表页面
- `ProjectList.vue` - 项目列表页面
- `ProjectDetail.vue` - 项目详情页面

### 10.2 数据库表

- `gsms_project` - 项目表
- `gsms_project_member` - 项目成员关联表
- `gsms_work_hour` - 工时记录表
- `sys_user` - 用户表
- `role` - 角色表
- `user_role` - 用户角色关联表

### 10.3 API接口

- `GET /api/projects` - 查询项目列表
- `POST /api/projects` - 创建项目
- `GET /api/projects/{id}/members` - 查询项目成员
- `POST /api/projects/{id}/members` - 添加项目成员
- `POST /api/work-hours` - 创建工时记录
- `GET /api/work-hours` - 查询工时列表
