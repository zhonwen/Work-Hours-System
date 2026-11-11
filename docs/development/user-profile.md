# 个人资料（一期）需求与技术设计

**文档版本：** 1.0  
**创建时间：** 2026-11-11  
**状态：** 一期开发中  

---

## 1. 产品范围

### 做

- 入口：用户菜单 → 个人信息 → `/account/profile`
- 帐号设置侧栏：个人资料 / 修改密码 / 关于
- 头像
  - 默认：`frontend/src/assets/avator/user1.jpg`
  - 可选同目录预设：`user1`～`user4`
  - 自定义上传：jpg / png / webp，≤ 1MB
  - 文件落后端磁盘；库只存访问路径 / 标识
- 封面：本地默认图展示，不支持上传
- 基本信息（用户自填）
  - 昵称：必填
  - 部门：自由文本（`department_display`），不与「部门管理」组织树强制联动
  - 手机、邮箱：选填；有值则做格式校验
- 不做短信 / 邮箱验证码换绑

### 不做（一期）

- 帐号设置块（改用户名 / 邮箱重绑 / 手机验证码）
- 封面上传
- 七牛等对象存储（仅预留可替换存储层）

---

## 2. 数据模型

Flyway：`V20261112__Add_user_profile_fields.sql`（版本号在 `V20261111` 之后递增）

| 字段 | 类型 | 说明 |
|------|------|------|
| `avatar_url` | `VARCHAR(255) NULL` | 头像标识或路径 |
| `department_display` | `VARCHAR(50) NULL` | 个人资料部门文本 |

不改动现有 `department_id`。

### `avatar_url` 约定

| 值 | 含义 |
|----|------|
| `null` / 空 | 前端默认 `user1` |
| `preset:user1` … `preset:user4` | 前端静态资源 |
| `/uploads/avatars/{userId}/{file}` | 自定义上传 |

---

## 3. 存储

- 接口：`FileStorageService`
- 一期实现：`LocalFileStorageService`
- 配置：`gsms.upload.base-dir`、`gsms.upload.avatar-max-bytes`
- 静态映射：`/uploads/**` → 本地目录；JWT 放行（`<img>` 无需 Token）
- 后续接七牛：只替换存储实现，库字段仍存 URL

---

## 4. API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/profile` | 当前用户个人资料 |
| PUT | `/api/users/profile` | 更新资料（含选预设头像） |
| POST | `/api/users/profile/avatar` | multipart 上传自定义头像 |

校验：

- 昵称必填，1–50
- 部门选填，≤50
- 手机选填；有值 `^1[3-9]\d{9}$`
- 邮箱选填；有值则邮箱格式
- PUT 的 `avatarUrl` 仅允许 `preset:user[1-4]`；自定义走上传接口

---

## 5. 前端

- 路由：`/account/profile`
- 布局：`AccountLayout`（左侧「个人资料」，预留帐号设置）
- 页面：封面 + 头像区 + 基本信息表单
- 预设与 URL 解析：`src/config/avatar.ts`
- 顶栏头像与入口联动

---

## 6. 验收

- [ ] 默认头像为 user1；可选 user2～4；上传 jpg/png/webp ≤1MB
- [ ] 超限 / 错误类型有提示
- [ ] 昵称空不可提交；手机/邮箱空可通过，格式错误拦截
- [ ] 部门文本不写 `department_id`
- [ ] 换账号不串头像（按 userId 分目录）
- [ ] Docker / 本地需持久化 `UPLOAD_DIR`（可选）
