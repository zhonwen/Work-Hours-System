import { defineStore } from 'pinia'
import { ref, watch, computed } from 'vue'
import {
  themes,
  themeList,
  THEME_ID_ALIASES,
  type ThemeConfig
} from '@/config/theme'
import { getUserPreference, updateUserPreference } from '@/api/user'

const THEME_KEY = 'teamMaster_theme'

const resolveThemeId = (raw: string | null | undefined): string => {
  if (!raw) return '默认白'
  if (themes[raw]) return raw
  const aliased = THEME_ID_ALIASES[raw]
  if (aliased && themes[aliased]) return aliased
  return '默认白'
}

export const useThemeStore = defineStore('theme', () => {
  const resolvedInitial = resolveThemeId(localStorage.getItem(THEME_KEY))
  if (localStorage.getItem(THEME_KEY) !== resolvedInitial) {
    localStorage.setItem(THEME_KEY, resolvedInitial)
  }

  const currentThemeId = ref<string>(resolvedInitial)
  const currentTheme = ref<ThemeConfig>(themes[currentThemeId.value])
  const availableThemes = computed(() => themeList)
  const preferenceLoaded = ref(false)

  const applyTheme = (theme: ThemeConfig) => {
    const root = document.documentElement
    root.style.setProperty('--theme-primary', theme.primaryColor)
    root.style.setProperty('--theme-primary-light', theme.primaryLight)
    root.style.setProperty('--theme-primary-hover', theme.primaryHover)
    root.style.setProperty('--theme-primary-shadow', theme.primaryShadow)
    root.style.setProperty('--sidebar-gradient-start', theme.sidebarGradientStart)
    root.style.setProperty('--sidebar-gradient-end', theme.sidebarGradientEnd)

    root.style.setProperty('--primary', theme.primaryColor)
    root.style.setProperty('--primary-tint', theme.primaryLight)

    root.style.setProperty('--sidebar-type', theme.sidebarType)
    root.style.setProperty('--sidebar-text', theme.sidebarText)
    root.style.setProperty('--sidebar-text-hover', theme.sidebarTextHover)
    root.style.setProperty('--sidebar-background', theme.sidebarBackground)
    root.style.setProperty('--sidebar-border', theme.sidebarBorder)

    root.style.setProperty('--el-color-primary', theme.primaryColor)
    root.style.setProperty('--el-color-primary-light-9', theme.primaryLight)
    root.style.setProperty('--el-color-primary-light-8', theme.primaryLight)
    root.style.setProperty('--el-color-primary-dark-2', theme.primaryColor)
  }

  const applyLocal = (themeId: string) => {
    const resolved = resolveThemeId(themeId)
    if (!themes[resolved]) return
    currentThemeId.value = resolved
    currentTheme.value = themes[resolved]
    localStorage.setItem(THEME_KEY, resolved)
    applyTheme(themes[resolved])
  }

  watch(currentThemeId, (newId) => {
    if (!themes[newId]) return
    currentTheme.value = themes[newId]
    localStorage.setItem(THEME_KEY, newId)
    applyTheme(themes[newId])
  })

  /** 仅本地切换（不写库） */
  const setTheme = (themeId: string) => {
    applyLocal(themeId)
  }

  /** 切换并持久化到服务端 */
  const setThemeAndPersist = async (themeId: string) => {
    const resolved = resolveThemeId(themeId)
    applyLocal(resolved)
    await updateUserPreference({ themeId: resolved })
  }

  /** 从服务端加载偏好并应用 */
  const loadPreferenceFromServer = async () => {
    try {
      const pref = await getUserPreference()
      const themeId = resolveThemeId(pref?.themeId)
      applyLocal(themeId)
      preferenceLoaded.value = true
      return themeId
    } catch (e) {
      console.warn('加载用户主题偏好失败，使用本地缓存', e)
      applyTheme(currentTheme.value)
      return currentThemeId.value
    }
  }

  const restoreTheme = () => {
    applyTheme(currentTheme.value)
  }

  /** 退出登录时重置为默认主题并清本地缓存 */
  const clearPreference = () => {
    preferenceLoaded.value = false
    currentThemeId.value = '默认白'
    currentTheme.value = themes['默认白']
    applyTheme(themes['默认白'])
    localStorage.removeItem(THEME_KEY)
  }

  return {
    currentThemeId,
    currentTheme,
    themes,
    availableThemes,
    preferenceLoaded,
    setTheme,
    setThemeAndPersist,
    loadPreferenceFromServer,
    restoreTheme,
    clearPreference,
    applyTheme
  }
})
