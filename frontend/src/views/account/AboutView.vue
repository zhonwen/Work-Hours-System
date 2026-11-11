<template>
  <div class="about-view">
    <div class="about-card">
      <img src="@/assets/logo/logo-tm-letters.png" alt="TeamChron" class="about-logo" />
      <h3 class="about-title">TeamChron 统领工时管理平台</h3>
      <p class="about-version">{{ appVersion }}</p>
      <p class="about-desc">
        面向研发团队的轻量级工时与协作管理工具，帮助团队清晰规划、透明协作、高效交付。
      </p>

      <div class="about-links">
        <a class="about-link" href="javascript:void(0)" @click.prevent="onTerms">服务条款</a>
        <span class="about-divider">|</span>
        <a class="about-link" href="javascript:void(0)" @click.prevent="onPrivacy">隐私声明</a>
      </div>

      <p class="about-copy">TeamChron 版权所有 © {{ year }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'

/** 与 frontend/package.json version 保持同步 */
const appVersion = '1.0.0'
const year = new Date().getFullYear()

const onTerms = () => {
  ElMessage.info('服务条款即将上线')
}

const onPrivacy = () => {
  ElMessage.info('隐私声明即将上线')
}
</script>

<style scoped>
.about-view {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 12px;
}

.about-card {
  width: 100%;
  max-width: 420px;
  text-align: center;
  padding: 48px 40px 40px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 42%);
  border: 1px solid var(--line-1, #e5e6eb);
  box-shadow: 0 12px 40px rgba(31, 56, 120, 0.06);
}

.about-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin-bottom: 20px;
  border-radius: 12px;
}

.about-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  letter-spacing: 0.01em;
  line-height: 1.45;
}

.about-version {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--text-3, #8f959e);
  font-variant-numeric: tabular-nums;
}

.about-desc {
  margin: 20px auto 0;
  max-width: 320px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-2, #646a73);
}

.about-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 28px;
}

.about-link {
  font-size: 13px;
  color: var(--primary, #3370fe);
  text-decoration: none;
  font-weight: 500;
}

.about-link:hover {
  text-decoration: underline;
}

.about-divider {
  color: var(--line-2, #dee0e3);
  font-size: 12px;
  user-select: none;
}

.about-copy {
  margin: 24px 0 0;
  font-size: 12px;
  color: var(--text-3, #8f959e);
}
</style>
