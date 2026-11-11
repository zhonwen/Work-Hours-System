<template>
  <div class="page-root">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
    </div>

    <div class="settings-panel">
      <div class="settings-section">
        <h3 class="section-title">外观偏好</h3>
        <p class="section-desc">菜单颜色将按账号保存，换设备登录后自动恢复</p>

        <div class="theme-grid" v-loading="loading">
          <button
            v-for="theme in themeStore.availableThemes"
            :key="theme.id"
            type="button"
            class="theme-card"
            :class="{ 'is-active': themeStore.currentThemeId === theme.id }"
            @click="handleSelectTheme(theme.id)"
          >
            <div
              class="theme-swatch"
              :class="{ 'is-light': theme.sidebarType === 'light' }"
              :style="{ backgroundColor: theme.previewColor }"
            ></div>
            <div class="theme-meta">
              <div class="theme-name">{{ theme.name }}</div>
              <div class="theme-side">{{ theme.sidebarType === 'light' ? '浅色侧栏' : '深色侧栏' }}</div>
            </div>
            <el-icon v-if="themeStore.currentThemeId === theme.id" class="check-icon"><Check /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const loading = ref(false)

const handleSelectTheme = async (themeId: string) => {
  if (themeId === themeStore.currentThemeId) return
  try {
    loading.value = true
    await themeStore.setThemeAndPersist(themeId)
    ElMessage.success(`已切换到「${themeStore.currentTheme.name}」`)
  } catch (e) {
    console.error(e)
    ElMessage.error('保存主题偏好失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    loading.value = true
    await themeStore.loadPreferenceFromServer()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.settings-panel {
  background: var(--surface, #fff);
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  padding: 20px;
}

.settings-section .section-title {
  margin: 0 0 4px;
}

.section-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--text-3, #8f959e);
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.theme-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  background: var(--surface, #fff);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, background-color 0.15s;
}

.theme-card:hover {
  border-color: var(--primary, #3370fe);
  background: #eaf3ff;
}

.theme-card.is-active {
  border-color: var(--primary, #3370fe);
  background: var(--primary-tint, #eef3ff);
}

.theme-swatch {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
}

.theme-swatch.is-light {
  border-color: #c9cdd4;
}

.theme-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1, #1f2329);
}

.theme-side {
  font-size: 12px;
  color: var(--text-3, #8f959e);
  margin-top: 2px;
}

.check-icon {
  margin-left: auto;
  color: var(--primary, #3370fe);
  font-size: 16px;
}
</style>
