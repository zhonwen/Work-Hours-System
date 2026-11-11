<template>
  <div class="password-view">
    <header class="section-header">
      <h3 class="section-title">修改密码</h3>
      <p class="section-desc">修改成功后需使用新密码重新登录</p>
    </header>

    <el-form
      ref="formRef"
      class="password-form"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent
    >
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          placeholder="请输入旧密码"
          autocomplete="current-password"
          size="large"
        />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="请输入新密码（6-20位）"
          autocomplete="new-password"
          size="large"
        />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
          autocomplete="new-password"
          size="large"
        />
      </el-form-item>

      <el-form-item label="验证码" prop="captchaCode">
        <div class="captcha-row">
          <el-input
            v-model="form.captchaCode"
            placeholder="请输入验证码"
            maxlength="6"
            clearable
            size="large"
            @keyup.enter="handleSubmit"
          />
          <button
            type="button"
            class="captcha-trigger"
            :disabled="captchaLoading"
            title="点击刷新验证码"
            @click="refreshCaptcha"
          >
            <img v-if="captchaImage" :src="captchaImage" class="captcha-image" alt="验证码" />
            <span v-else class="captcha-placeholder">
              {{ captchaLoading ? '加载中' : '获取验证码' }}
            </span>
          </button>
        </div>
      </el-form-item>

      <el-button
        type="primary"
        class="submit-btn"
        size="large"
        :loading="loading"
        @click="handleSubmit"
      >
        确定
      </el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCaptcha, changePassword } from '@/api/auth'
import { encryptPasswordWithCid } from '@/utils/rsaEncrypt'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useProjectStore } from '@/stores/project'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const projectStore = useProjectStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)
const captchaCid = ref('')
const captchaPublicKey = ref('')
const captchaImage = ref('')

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captchaCode: ''
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
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

const refreshCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captchaCid.value = res.cid
    captchaPublicKey.value = res.publicKey
    captchaImage.value = res.imageBase64
    form.captchaCode = ''
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (!captchaCid.value || !captchaPublicKey.value) {
      ElMessage.warning('请先获取验证码')
      await refreshCaptcha()
      return
    }
    loading.value = true
    const oldCipher = encryptPasswordWithCid(
      captchaPublicKey.value,
      form.oldPassword,
      captchaCid.value
    )
    const newCipher = encryptPasswordWithCid(
      captchaPublicKey.value,
      form.newPassword,
      captchaCid.value
    )
    await changePassword({
      oldPassword: oldCipher,
      newPassword: newCipher,
      cid: captchaCid.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.clearAuth()
    themeStore.clearPreference()
    projectStore.clearUserCache()
    router.push('/login')
  } catch (error: unknown) {
    if (error !== false) {
      console.error('修改密码失败:', error)
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.password-view {
  max-width: 420px;
  font-size: 13px;
}

.section-header {
  margin-bottom: 28px;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
}

.section-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-3, #8f959e);
}

.password-form :deep(.el-form-item__label) {
  font-weight: 500;
  font-size: 13px;
  color: var(--text-2, #646a73);
  padding-bottom: 6px;
}

.password-form :deep(.el-input__inner),
.password-form :deep(.el-button) {
  font-size: 13px;
}

.password-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--line-1, #e5e6eb) inset;
}

.password-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary, #3370fe) inset, 0 0 0 3px rgba(51, 112, 254, 0.12);
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-trigger {
  flex-shrink: 0;
  width: 128px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: 8px;
  background: var(--fill-1, #f7f8fa);
  cursor: pointer;
  overflow: hidden;
}

.captcha-trigger:hover:not(:disabled) {
  border-color: #c9cdd4;
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
  color: var(--text-3, #8f959e);
}

.submit-btn {
  width: 120px;
  margin-top: 8px;
  border-radius: 8px;
  font-weight: 500;
}
</style>
