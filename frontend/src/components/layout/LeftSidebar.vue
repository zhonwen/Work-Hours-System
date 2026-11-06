<template>
  <el-aside
    :width="isCollapse ? '64px' : '240px'"
    class="sidebar-ones"
    :data-theme-type="themeStore.currentTheme.sidebarType"
  >
    <!-- Logo 区域 -->
    <div class="logo-container">
      <img src="@/assets/logo/logo-tm-letters.png" alt="TeamChron" class="logo-icon" :width="isCollapse ? 28 : 32" />
      <span v-if="!isCollapse" class="logo-text">TeamChron</span>
    </div>

    <!-- 动态菜单 -->
    <el-menu
      v-if="menuTree.length > 0"
      :default-active="activeMenu"
      :collapse="isCollapse"
      @select="handleMenuSelect"
      router
      class="sidebar-menu"
    >
      <template v-for="menu in menuTree" :key="menu.id">
        <!-- 目录类型（有子菜单） -->
        <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="String(menu.id)">
          <template #title>
            <el-icon v-if="menu.icon" :size="18" class="menu-icon">
              <component :is="iconMap[menu.icon]" />
            </el-icon>
            <span>{{ menu.name }}</span>
          </template>

          <!-- 递归渲染子菜单 -->
          <template v-for="subMenu in menu.children" :key="subMenu.id">
            <el-menu-item
              v-if="!subMenu.children || subMenu.children.length === 0"
              :index="subMenu.path"
            >
              <el-icon v-if="subMenu.icon" :size="18" class="menu-icon">
                <component :is="iconMap[subMenu.icon]" />
              </el-icon>
              <template #title>{{ subMenu.name }}</template>
            </el-menu-item>

            <!-- 三级菜单（如需要） -->
            <el-sub-menu v-else :index="String(subMenu.id)">
              <template #title>
                <el-icon v-if="subMenu.icon" :size="18" class="menu-icon">
                  <component :is="iconMap[subMenu.icon]" />
                </el-icon>
                <span>{{ subMenu.name }}</span>
              </template>
              <el-menu-item
                v-for="child in subMenu.children"
                :key="child.id"
                :index="child.path"
              >
                <el-icon v-if="child.icon" :size="18" class="menu-icon">
                  <component :is="iconMap[child.icon]" />
                </el-icon>
                <template #title>{{ child.name }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-sub-menu>

        <!-- 菜单项（无子菜单） -->
        <el-menu-item v-else :index="menu.path">
          <el-icon v-if="menu.icon" :size="18" class="menu-icon">
            <component :is="iconMap[menu.icon]" />
          </el-icon>
          <template #title>{{ menu.name }}</template>
        </el-menu-item>
      </template>
    </el-menu>

    <!-- 加载状态 -->
    <div v-else class="menu-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 底部功能区 -->
    <div class="sidebar-bottom">
      <!-- 折叠按钮 -->
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="20">
          <component :is="isCollapse ? Expand : Fold" />
        </el-icon>
      </div>

      <!-- 主题切换 -->
      <el-dropdown trigger="click" @command="handleThemeChange">
        <div class="bottom-action-btn">
          <el-icon :size="18">
            <component :is="Sunny" v-if="themeStore.currentTheme.sidebarType === 'light'" />
            <component :is="Moon" v-else />
          </el-icon>
          <span v-if="!isCollapse" class="action-text">{{ themeStore.currentTheme.name }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="theme in themeStore.availableThemes"
              :key="theme.id"
              :command="theme.id"
              :class="{ 'is-active': theme.id === themeStore.currentTheme.id }"
            >
              <div class="theme-option">
                <div
                  class="theme-color-preview"
                  :style="{ backgroundColor: theme.primaryColor }"
                ></div>
                <span>{{ theme.name }}</span>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 用户信息 -->
      <el-dropdown trigger="click" @command="handleUserAction">
        <div class="user-info">
          <el-avatar :size="32" :src="userAvatar">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span v-if="!isCollapse" class="username">{{ username }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-dropdown-item>
            <el-dropdown-item command="changePassword">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog
      v-model="changePasswordVisible"
      title="修改密码"
      width="480px"
      :close-on-click-modal="false"
      @close="handleChangePasswordClose"
    >
      <el-form
        ref="changePasswordFormRef"
        :model="changePasswordForm"
        :rules="changePasswordRules"
        label-width="100px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="changePasswordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入旧密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="changePasswordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（6-20位）"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="changePasswordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="changePasswordForm.captchaCode"
              placeholder="请输入验证码"
              maxlength="6"
              clearable
            />
            <img
              v-if="changeCaptchaImage"
              :src="changeCaptchaImage"
              class="captcha-image"
              alt="验证码"
              title="点击刷新验证码"
              @click="refreshChangeCaptcha"
            />
            <el-button v-else @click="refreshChangeCaptcha" :loading="changeCaptchaLoading">刷新</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="changePasswordLoading" @click="handleSubmitChangePassword">
          确定
        </el-button>
      </template>
    </el-dialog>
  </el-aside>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import { getUserMenuTree, type MenuInfo } from '@/api/menu'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha, changePassword } from '@/api/auth'
import { encryptPasswordWithCid } from '@/utils/rsaEncrypt'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const authStore = useAuthStore()

// 动态菜单树
const menuTree = ref<MenuInfo[]>([])

// 图标映射对象
const iconMap = ElementPlusIcons

// 侧边栏折叠状态（从 localStorage 读取，默认 false）
const isCollapse = ref(localStorage.getItem('sidebarCollapsed') === 'true')

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 用户名
const username = computed(() => authStore.username || localStorage.getItem('username') || '用户')
const userAvatar = ref('')

// 加载用户菜单
const fetchUserMenus = async () => {
  try {
    const menus = await getUserMenuTree()
    menuTree.value = menus
    console.log('菜单加载成功:', menus)
  } catch (error) {
    console.error('加载菜单失败:', error)
    ElMessage.error('加载菜单失败')
  }
}

// 处理菜单选择事件
const handleMenuSelect = (index: string) => {
  try {
    const menus = await getUserMenuTree()
    menuTree.value = menus
    console.log('菜单加载成功:', menus)
  } catch (error) {
    console.error('加载菜单失败:', error)
    ElMessage.error('加载菜单失败')
  }
}

// 切换侧边栏
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  // 保存到 localStorage
  localStorage.setItem('sidebarCollapsed', String(isCollapse.value))
}

// 切换主题
const handleThemeChange = (themeId: string) => {
  themeStore.setTheme(themeId)
  ElMessage.success(`已切换到${themeStore.currentTheme.name}主题`)
}

const changePasswordVisible = ref(false)
const changePasswordLoading = ref(false)
const changeCaptchaLoading = ref(false)
const changePasswordFormRef = ref<FormInstance>()
const changeCaptchaCid = ref('')
const changeCaptchaPublicKey = ref('')
const changeCaptchaImage = ref('')
const changePasswordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captchaCode: ''
})

const changePasswordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== changePasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const refreshChangeCaptcha = async () => {
  changeCaptchaLoading.value = true
  try {
    const res = await getCaptcha()
    changeCaptchaCid.value = res.cid
    changeCaptchaPublicKey.value = res.publicKey
    changeCaptchaImage.value = res.imageBase64
    changePasswordForm.captchaCode = ''
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败')
  } finally {
    changeCaptchaLoading.value = false
  }
}

const openChangePassword = () => {
  changePasswordForm.oldPassword = ''
  changePasswordForm.newPassword = ''
  changePasswordForm.confirmPassword = ''
  changePasswordForm.captchaCode = ''
  changePasswordVisible.value = true
  refreshChangeCaptcha()
}

const handleChangePasswordClose = () => {
  changePasswordFormRef.value?.resetFields()
  changeCaptchaCid.value = ''
  changeCaptchaPublicKey.value = ''
  changeCaptchaImage.value = ''
}

const handleSubmitChangePassword = async () => {
  if (!changePasswordFormRef.value) return
  try {
    await changePasswordFormRef.value.validate()
    if (!changeCaptchaCid.value || !changeCaptchaPublicKey.value) {
      ElMessage.warning('请先获取验证码')
      await refreshChangeCaptcha()
      return
    }
    changePasswordLoading.value = true
    const oldCipher = encryptPasswordWithCid(
      changeCaptchaPublicKey.value,
      changePasswordForm.oldPassword,
      changeCaptchaCid.value
    )
    const newCipher = encryptPasswordWithCid(
      changeCaptchaPublicKey.value,
      changePasswordForm.newPassword,
      changeCaptchaCid.value
    )
    await changePassword({
      oldPassword: oldCipher,
      newPassword: newCipher,
      cid: changeCaptchaCid.value,
      captchaCode: changePasswordForm.captchaCode
    })
    ElMessage.success('密码修改成功，请重新登录')
    changePasswordVisible.value = false
    authStore.clearAuth()
    router.push('/login')
  } catch (error: any) {
    if (error !== false) {
      console.error('修改密码失败:', error)
      await refreshChangeCaptcha()
    }
  } finally {
    changePasswordLoading.value = false
  }
}

// 用户操作
const handleUserAction = (command: string) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'changePassword':
      openChangePassword()
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          authStore.clearAuth()
          ElMessage.success('退出成功')
          router.push('/login')
        })
        .catch(() => {})
      break
  }
}

// 组件挂载时恢复主题并加载菜单
onMounted(async () => {
  await fetchUserMenus()
  themeStore.restoreTheme()
})

// 暴露折叠状态供父组件使用
defineExpose({
  isCollapse
})
</script>

<style scoped>
.sidebar-ones {
  transition: width 0.3s, background 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Logo 区域 */
.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  gap: 12px;
  border-bottom: 1px solid var(--sidebar-border);
  flex-shrink: 0;
  transition: all 0.3s;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  line-height: 1;
  white-space: nowrap;
}

.logo-icon {
  flex-shrink: 0;
  filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.6)) drop-shadow(0 0 12px var(--theme-primary-shadow));
}

/* 菜单容器，添加滚动 */
.sidebar-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 240px;
}

/* 菜单加载状态 */
.menu-loading {
  padding: 20px;
}

/* 底部功能区 */
.sidebar-bottom {
  flex-shrink: 0;
  padding: 8px;
  border-top: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.collapse-btn {
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--sidebar-text);
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  transition: all 0.3s;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.bottom-action-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  height: 40px;
  cursor: pointer;
  color: var(--sidebar-text);
  border-radius: 6px;
  transition: background-color 0.3s;
}

.bottom-action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.action-text {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-color-preview {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  color: var(--sidebar-text);
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.username {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Element Plus 菜单项样式覆盖 */
:deep(.el-menu) {
  background: transparent;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  transition: all 0.3s;
}

:deep(.el-menu-item:hover) {
  background-color: var(--theme-primary-hover) !important;
  color: #fff !important;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--theme-primary) !important;
  color: #fff !important;
  box-shadow: 0 2px 8px var(--theme-primary-shadow);
}

:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.75);
  transition: all 0.3s;
}

:deep(.el-sub-menu__title:hover) {
  background-color: var(--theme-primary-hover) !important;
  color: #fff !important;
}

:deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.2) !important;
}

.menu-icon {
  color: #fff;
}

/* 浅色主题样式 */
.sidebar-ones[data-theme-type="light"] {
  background: var(--sidebar-background);
  border-right: 1px solid var(--sidebar-border);
}

.sidebar-ones[data-theme-type="light"] .logo-container {
  border-bottom: 1px solid var(--sidebar-border);
}

.sidebar-ones[data-theme-type="light"] .logo-text {
  color: #333;
  text-shadow: none;
}

.sidebar-ones[data-theme-type="light"] .logo-icon {
  color: var(--theme-primary);
  filter: none;
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu-item) {
  color: var(--sidebar-text);
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu-item:hover) {
  background-color: var(--theme-primary-light) !important;
  color: var(--sidebar-text-hover) !important;
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu-item.is-active) {
  background-color: var(--theme-primary) !important;
  color: #fff !important;
}

.sidebar-ones[data-theme-type="light"] :deep(.el-sub-menu__title) {
  color: var(--sidebar-text);
}

.sidebar-ones[data-theme-type="light"] :deep(.el-sub-menu__title:hover) {
  background-color: var(--theme-primary-light) !important;
  color: var(--sidebar-text-hover) !important;
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.02) !important;
}

.sidebar-ones[data-theme-type="light"] .menu-icon {
  color: var(--sidebar-text);
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu-item:hover) .menu-icon,
.sidebar-ones[data-theme-type="light"] :deep(.el-sub-menu__title:hover) .menu-icon {
  color: var(--sidebar-text-hover);
}

.sidebar-ones[data-theme-type="light"] :deep(.el-menu-item.is-active) .menu-icon {
  color: #fff;
}

.sidebar-ones[data-theme-type="light"] .sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
}

.sidebar-ones[data-theme-type="light"] .sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.sidebar-ones[data-theme-type="light"] .collapse-btn,
.sidebar-ones[data-theme-type="light"] .bottom-action-btn,
.sidebar-ones[data-theme-type="light"] .user-info {
  color: var(--sidebar-text);
}

.sidebar-ones[data-theme-type="light"] .collapse-btn:hover,
.sidebar-ones[data-theme-type="light"] .bottom-action-btn:hover,
.sidebar-ones[data-theme-type="light"] .user-info:hover {
  background: rgba(0, 0, 0, 0.05);
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-image {
  height: 40px;
  width: 120px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  flex-shrink: 0;
  object-fit: cover;
  background: #f5f7fa;
}
</style>
