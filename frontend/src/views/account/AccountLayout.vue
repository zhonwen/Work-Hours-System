<template>
  <div class="account-layout page-root">
    <div class="page-header">
      <h2 class="page-title">帐号设置</h2>
    </div>

    <div class="account-body">
      <aside class="account-nav">
        <div class="nav-group">
          <div class="nav-group-title">访问</div>
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </div>
      </aside>

      <main class="account-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/account/profile', label: '个人资料' },
  { path: '/account/password', label: '修改密码' },
  { path: '/account/about', label: '关于' }
]

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
</script>

<style scoped>
.account-layout {
  font-size: 13px;
  color: var(--text-1, #1f2329);
}

.account-body {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.account-nav {
  background: var(--surface, #fff);
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  padding: 16px 12px;
  min-height: 320px;
}

.nav-group-title {
  padding: 4px 12px 10px;
  font-size: 12px;
  color: var(--text-3, #8f959e);
  letter-spacing: 0.04em;
}

.nav-item {
  display: block;
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  color: var(--text-1, #1f2329);
  text-decoration: none;
  font-size: 13px;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-item:hover {
  background: var(--fill-1, #f7f8fa);
}

.nav-item.is-active {
  background: rgba(51, 112, 254, 0.08);
  color: var(--primary, #3370fe);
  font-weight: 500;
}

.account-content {
  background: var(--surface, #fff);
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  padding: 24px;
  min-height: 480px;
  font-size: 13px;
}

.account-content :deep(.el-form-item__label),
.account-content :deep(.el-input__inner),
.account-content :deep(.el-button),
.account-content :deep(.el-textarea__inner) {
  font-size: 13px;
}

@media (max-width: 768px) {
  .account-body {
    grid-template-columns: 1fr;
  }

  .account-nav {
    min-height: auto;
  }
}
</style>
