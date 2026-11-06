<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <img src="@/assets/logo/logo-tm-letters.png" alt="TeamChron" class="login-logo" />
          <h2>TeamChron</h2>
          <p class="login-subtitle">统领工时管理平台</p>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="请输入验证码"
              maxlength="6"
              @keyup.enter="handleLogin"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-image"
              alt="验证码"
              title="点击刷新验证码"
              @click="refreshCaptcha"
            />
            <el-button v-else @click="refreshCaptcha" :loading="captchaLoading">刷新</el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>

        <el-form-item>
          <div class="footer-links">
            <router-link to="/register">还没有账号？立即注册</router-link>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
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
      // 业务错误已由 request 拦截器提示；失败后刷新验证码
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
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
}

.login-header {
  text-align: center;
  padding: 10px 0;
}

.login-logo {
  width: 64px;
  object-fit: cover;
  margin-bottom: 10px;
}

.login-header h2 {
  margin: 10px 0 5px;
  text-align: center;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.login-subtitle {
  margin: 0;
  text-align: center;
  color: #666;
  font-size: 14px;
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

.footer-links {
  width: 100%;
  text-align: center;
}

.footer-links a {
  color: #409eff;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}
</style>
