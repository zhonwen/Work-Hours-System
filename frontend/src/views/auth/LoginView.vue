<template>
  <div class="login-page">
    <section class="login-brand" aria-hidden="false">
      <div class="brand-top">
        <img src="@/assets/logo/logo-tm-letters.png" alt="" class="brand-logo" />
        <div class="brand-copy">
          <h1 class="brand-name">TeamChron</h1>
          <p class="brand-tagline">统领工时管理平台</p>
        </div>
      </div>
      <div class="brand-visual">
        <img
          src="@/assets/loginCover.png"
          alt="TeamChron 工作台示意"
          class="cover-image"
        />
      </div>
      <p class="brand-footnote">轻量工时 · 清晰协作 · 一目了然</p>
    </section>

    <section class="login-panel">
      <div class="login-panel-inner">
        <header class="panel-header">
          <h2 class="panel-title">登录</h2>
          <p class="panel-desc">使用账号密码进入工作台</p>
        </header>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          size="large"
          @submit.prevent
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="请输入验证码"
                maxlength="6"
                :prefix-icon="Key"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="captcha-trigger"
                :disabled="captchaLoading"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  class="captcha-image"
                  alt="验证码"
                />
                <span v-else class="captcha-placeholder">
                  {{ captchaLoading ? '加载中' : '获取验证码' }}
                </span>
              </button>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            class="login-submit"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>

        <footer class="panel-footer">
          <span class="footer-text">还没有账号？</span>
          <router-link to="/register" class="footer-link">立即注册</router-link>
        </footer>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha, login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { message } from '@/utils/message'
import { encryptPasswordWithCid } from '@/utils/rsaEncrypt'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)

const captchaCid = ref('')
const captchaPublicKey = ref('')
const captchaImage = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captchaCid.value = res.cid
    captchaPublicKey.value = res.publicKey
    captchaImage.value = res.imageBase64
    loginForm.captchaCode = ''
  } catch (error) {
    console.error('获取验证码失败:', error)
    message.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!captchaCid.value || !captchaPublicKey.value) {
      message.warning('请先获取验证码')
      await refreshCaptcha()
      return
    }

    loading.value = true
    try {
      const encryptedPassword = encryptPasswordWithCid(
        captchaPublicKey.value,
        loginForm.password,
        captchaCid.value
      )

      const res = await login({
        username: loginForm.username,
        password: encryptedPassword,
        cid: captchaCid.value,
        captchaCode: loginForm.captchaCode
      })

      const token = res || (res as any)?.data?.token
      if (!token) {
        throw new Error('登录失败：未获取到 token')
      }

      authStore.setAuth(token as unknown as string, loginForm.username)

      try {
        await Promise.race([
          authStore.refreshAuth(),
          new Promise((_, reject) => setTimeout(() => reject(new Error('权限加载超时')), 5000))
        ])
      } catch (permError) {
        console.warn('加载权限信息失败，但继续登录流程:', permError)
      }

      message.success('登录成功')

      setTimeout(() => {
        router.push('/dashboard').catch((err) => {
          console.error('路由跳转失败:', err)
          window.location.href = '/dashboard'
        })
      }, 100)
    } catch (error: unknown) {
      console.error('登录错误:', error)
      await refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-page {
  --login-primary: #3370fe;
  --login-primary-hover: #245bdb;
  --login-text: #1f2329;
  --login-text-secondary: #646a73;
  --login-border: #e5e6eb;
  --login-bg: #f5f7fb;
  --login-panel: #ffffff;

  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  min-height: 100vh;
  background: var(--login-bg);
}

.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px 40px;
  overflow: hidden;
  background:
    radial-gradient(ellipse 80% 60% at 20% 10%, rgba(51, 112, 254, 0.08), transparent 55%),
    radial-gradient(ellipse 70% 50% at 85% 90%, rgba(51, 112, 254, 0.06), transparent 50%),
    linear-gradient(160deg, #eef3ff 0%, #f7f9fc 45%, #eef2f8 100%);
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
  z-index: 1;
}

.brand-logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
  border-radius: 10px;
}

.brand-name {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--login-text);
  line-height: 1.2;
}

.brand-tagline {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--login-text-secondary);
}

.brand-visual {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  z-index: 1;
}

.cover-image {
  width: min(100%, 640px);
  max-height: min(58vh, 520px);
  object-fit: contain;
  filter: drop-shadow(0 24px 48px rgba(31, 56, 120, 0.12));
  user-select: none;
  pointer-events: none;
}

.brand-footnote {
  margin: 0;
  font-size: 13px;
  color: var(--login-text-secondary);
  letter-spacing: 0.04em;
  z-index: 1;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--login-panel);
  border-left: 1px solid rgba(229, 230, 235, 0.9);
  box-shadow: -12px 0 40px rgba(15, 35, 80, 0.04);
}

.login-panel-inner {
  width: 100%;
  max-width: 460px;
}

.panel-header {
  margin-bottom: 40px;
}

.panel-title {
  margin: 0;
  font-size: 32px;
  font-weight: 650;
  color: var(--login-text);
  letter-spacing: -0.02em;
}

.panel-desc {
  margin: 10px 0 0;
  font-size: 16px;
  color: var(--login-text-secondary);
}

.login-form {
  /* 统一：用户名 / 密码 / 验证码输入框 / 验证码图 / 登录按钮 同一高度 */
  --login-control-height: 56px;
  --el-component-size: var(--login-control-height);
  --el-component-size-large: var(--login-control-height);
  --el-input-height: var(--login-control-height);
  --el-input-inner-height: var(--login-control-height);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 26px;
}

.login-form :deep(.el-form-item__content) {
  line-height: normal;
}

.login-form :deep(.el-input),
.login-form :deep(.el-input--large) {
  width: 100%;
  height: var(--login-control-height) !important;
}

.login-form :deep(.el-input__wrapper) {
  height: var(--login-control-height) !important;
  min-height: var(--login-control-height) !important;
  max-height: var(--login-control-height) !important;
  padding: 0 16px !important;
  border-radius: 10px;
  box-sizing: border-box !important;
  box-shadow: 0 0 0 1px var(--login-border) inset;
  background: #fff;
  transition: box-shadow 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c9cdd4 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--login-primary) inset, 0 0 0 3px rgba(51, 112, 254, 0.12);
}

.login-form :deep(.el-input__inner) {
  height: var(--login-control-height) !important;
  line-height: var(--login-control-height) !important;
  font-size: 16px !important;
}

.login-form :deep(.el-input__prefix),
.login-form :deep(.el-input__suffix) {
  display: flex;
  align-items: center;
  color: #8f959e;
  font-size: 18px;
}

.login-form :deep(.el-form-item__error) {
  padding-top: 4px;
  font-size: 13px;
}

.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
  width: 100%;
  min-height: var(--login-control-height);
}

.captcha-row :deep(.el-input) {
  flex: 1;
  height: var(--login-control-height) !important;
}

.captcha-row :deep(.el-input__wrapper),
.captcha-row :deep(.el-input__inner) {
  height: var(--login-control-height) !important;
  min-height: var(--login-control-height) !important;
  max-height: var(--login-control-height) !important;
}

.captcha-trigger {
  flex-shrink: 0;
  align-self: stretch;
  width: 148px;
  height: var(--login-control-height) !important;
  min-height: var(--login-control-height) !important;
  padding: 0;
  border: 1px solid var(--login-border);
  border-radius: 10px;
  box-sizing: border-box;
  background: #f7f8fa;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.captcha-trigger:hover:not(:disabled) {
  border-color: #c9cdd4;
  background: #eef0f4;
}

.captcha-trigger:disabled {
  cursor: wait;
  opacity: 0.75;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 13px;
  color: var(--login-text-secondary);
}

.login-submit {
  width: 100%;
  height: var(--login-control-height, 56px) !important;
  margin-top: 10px;
  border: none;
  border-radius: 10px;
  font-size: 17px;
  font-weight: 560;
  letter-spacing: 0.04em;
  background: var(--login-primary);
  box-shadow: 0 6px 16px rgba(51, 112, 254, 0.28);
}

.login-submit:hover,
.login-submit:focus {
  background: var(--login-primary-hover) !important;
  border-color: var(--login-primary-hover) !important;
}

.panel-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 32px;
  font-size: 15px;
}

.footer-text {
  color: var(--login-text-secondary);
}

.footer-link {
  color: var(--login-primary);
  text-decoration: none;
  font-weight: 500;
}

.footer-link:hover {
  color: var(--login-primary-hover);
  text-decoration: underline;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-brand {
    min-height: 220px;
    padding: 28px 24px 16px;
  }

  .brand-name {
    font-size: 22px;
  }

  .brand-visual {
    padding: 8px 0;
  }

  .cover-image {
    max-height: 160px;
  }

  .brand-footnote {
    display: none;
  }

  .login-panel {
    border-left: none;
    box-shadow: none;
    padding: 32px 24px 48px;
    align-items: flex-start;
  }
}
</style>
