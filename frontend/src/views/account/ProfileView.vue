<template>
  <div class="profile-view" v-loading="loading">
    <section class="profile-section">
      <h3 class="section-title">头像和封面</h3>
      <div class="avatar-cover-row">
        <div class="cover-preview">
          <img :src="defaultCover" alt="封面" class="cover-img" />
          <div class="avatar-overlap">
            <el-avatar :size="72" :src="previewAvatarSrc" class="avatar-img">
              {{ (form.nickname || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
          </div>
        </div>
        <div class="avatar-actions">
          <div class="preset-row">
            <button
              v-for="preset in AVATAR_PRESETS"
              :key="preset.key"
              type="button"
              class="preset-btn"
              :class="{ 'is-active': form.avatarUrl === preset.key }"
              :title="preset.label"
              @click="selectPreset(preset.key)"
            >
              <img :src="preset.src" :alt="preset.label" />
            </button>
          </div>
          <div class="action-row">
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
              :on-change="handleFileChange"
            >
              <el-button>上传头像</el-button>
            </el-upload>
            <el-button @click="restoreDefaultAvatar">恢复默认</el-button>
          </div>
          <p class="hint">仅支持 JPG、PNG、WEBP，头像不超过 1MB；封面为默认展示，暂不支持上传</p>
        </div>
      </div>
    </section>

    <section class="profile-section">
      <h3 class="section-title">基本信息</h3>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="profile-form"
        @submit.prevent
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="50" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="部门" prop="departmentDisplay">
          <el-input v-model="form.departmentDisplay" maxlength="50" placeholder="展示用部门（选填）" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="100" placeholder="选填" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  AVATAR_PRESETS,
  DEFAULT_AVATAR_KEY,
  resolveAvatarSrc
} from '@/config/avatar'
import defaultCover from '@/assets/cover/default-cover.svg'
import { getUserProfile, updateUserProfile, uploadUserAvatar } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  nickname: '',
  departmentDisplay: '',
  phone: '',
  email: '',
  avatarUrl: DEFAULT_AVATAR_KEY as string
})

const previewAvatarSrc = computed(() => resolveAvatarSrc(form.avatarUrl))

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ],
  departmentDisplay: [{ max: 50, message: '部门不能超过50个字符', trigger: 'blur' }],
  phone: [
    {
      validator: (_rule, value, callback) => {
        if (!value || !String(value).trim()) {
          callback()
          return
        }
        if (!/^1[3-9]\d{9}$/.test(String(value).trim())) {
          callback(new Error('手机号格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (_rule, value, callback) => {
        if (!value || !String(value).trim()) {
          callback()
          return
        }
        if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(value).trim())) {
          callback(new Error('邮箱格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadProfile = async () => {
  loading.value = true
  try {
    const profile = await getUserProfile()
    form.nickname = profile.nickname || ''
    form.departmentDisplay = profile.departmentDisplay || ''
    form.phone = profile.phone || ''
    form.email = profile.email || ''
    form.avatarUrl = profile.avatarUrl || DEFAULT_AVATAR_KEY
    authStore.setAvatarUrl(form.avatarUrl)
  } catch (e) {
    console.error(e)
    ElMessage.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

const selectPreset = (key: string) => {
  form.avatarUrl = key
}

const restoreDefaultAvatar = () => {
  form.avatarUrl = DEFAULT_AVATAR_KEY
}

const handleFileChange = async (uploadFile: UploadFile) => {
  const raw = uploadFile.raw
  if (!raw) return

  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg']
  if (raw.type && !allowed.includes(raw.type)) {
    ElMessage.error('头像仅支持 JPG、PNG、WEBP 格式')
    return
  }
  if (raw.size > 1048576) {
    ElMessage.error('头像大小不能超过 1MB')
    return
  }

  try {
    saving.value = true
    const profile = await uploadUserAvatar(raw)
    form.avatarUrl = profile.avatarUrl || form.avatarUrl
    authStore.setAvatarUrl(form.avatarUrl)
    ElMessage.success('头像已上传')
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        nickname: form.nickname.trim(),
        departmentDisplay: form.departmentDisplay?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        email: form.email?.trim() || undefined,
        avatarUrl: form.avatarUrl?.startsWith('preset:') ? form.avatarUrl : undefined
      }
      const profile = await updateUserProfile(payload)
      form.avatarUrl = profile.avatarUrl || form.avatarUrl
      authStore.setAvatarUrl(form.avatarUrl)
      if (profile.nickname) {
        // 昵称变化不影响登录用户名展示
      }
      ElMessage.success('保存成功')
    } catch (e) {
      console.error(e)
    } finally {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-view {
  font-size: 13px;
}

.profile-section + .profile-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--line-1, #e5e6eb);
}

.section-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
}

.avatar-cover-row {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.cover-preview {
  position: relative;
  width: 280px;
  height: 120px;
  border-radius: 8px;
  overflow: visible;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  display: block;
}

.avatar-overlap {
  position: absolute;
  left: 50%;
  bottom: -28px;
  transform: translateX(-50%);
}

.avatar-img {
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(15, 35, 80, 0.12);
  background: #fff;
}

.avatar-actions {
  flex: 1;
  min-width: 240px;
  padding-top: 8px;
}

.preset-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.preset-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: transparent;
}

.preset-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.preset-btn.is-active {
  border-color: var(--primary, #3370fe);
}

.action-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.hint {
  margin: 0;
  font-size: 12px;
  color: var(--text-3, #8f959e);
  line-height: 1.5;
}

.profile-form {
  max-width: 480px;
  margin-top: 8px;
}

.profile-form :deep(.el-form-item__label),
.profile-form :deep(.el-input__inner),
.profile-form :deep(.el-button) {
  font-size: 13px;
}
</style>
