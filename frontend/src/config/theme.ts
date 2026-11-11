/**
 * 主题配置
 * 定义多个配色方案供用户选择
 */

export interface ThemeConfig {
  id: string
  name: string
  primaryColor: string // 主题色
  primaryLight: string // 浅色背景（用于按钮未激活状态等）
  primaryHover: string // 悬停背景色（半透明）
  primaryShadow: string // 阴影颜色（半透明）
  /** 下拉预览色块：浅色主题用侧栏底色，深色主题用主色 */
  previewColor: string
  sidebarGradientStart: string // 侧边栏渐变起始色
  sidebarGradientEnd: string // 侧边栏渐变结束色
  sidebarType: 'dark' | 'light' // 侧边栏类型：深色或浅色
  sidebarText: string // 侧边栏文字颜色
  sidebarTextHover: string // 侧边栏文字悬停颜色
  sidebarBackground: string // 侧边栏背景色（浅色主题使用）
  sidebarBorder: string // 侧边栏边框颜色
}

/**
 * 主题列表（顺序即下拉展示顺序）
 * 已去掉近重复蓝系；浅色主题 preview 用底色，避免误显成主色蓝
 */
export const themes: Record<string, ThemeConfig> = {
  默认白: {
    id: '默认白',
    name: '默认白',
    primaryColor: '#3370fe',
    primaryLight: '#eef3ff',
    primaryHover: 'rgba(51, 112, 254, 0.1)',
    primaryShadow: 'rgba(51, 112, 254, 0.25)',
    previewColor: '#ffffff',
    sidebarGradientStart: '',
    sidebarGradientEnd: '',
    sidebarType: 'light',
    sidebarText: '#646a73',
    sidebarTextHover: '#3370fe',
    sidebarBackground: '#ffffff',
    sidebarBorder: '#e5e6eb'
  },
  浅灰: {
    id: '浅灰',
    name: '浅灰',
    primaryColor: '#3370fe',
    primaryLight: '#eef3ff',
    primaryHover: 'rgba(51, 112, 254, 0.1)',
    primaryShadow: 'rgba(51, 112, 254, 0.25)',
    previewColor: '#f5f6f7',
    sidebarGradientStart: '',
    sidebarGradientEnd: '',
    sidebarType: 'light',
    sidebarText: '#646a73',
    sidebarTextHover: '#3370fe',
    sidebarBackground: '#f5f6f7',
    sidebarBorder: '#e5e6eb'
  },
  浅蓝: {
    id: '浅蓝',
    name: '浅蓝',
    primaryColor: '#3370fe',
    primaryLight: '#eef3ff',
    primaryHover: 'rgba(51, 112, 254, 0.1)',
    primaryShadow: 'rgba(51, 112, 254, 0.25)',
    previewColor: '#eef3ff',
    sidebarGradientStart: '',
    sidebarGradientEnd: '',
    sidebarType: 'light',
    sidebarText: '#646a73',
    sidebarTextHover: '#3370fe',
    sidebarBackground: '#eef3ff',
    sidebarBorder: '#dbe5ff'
  },
  浅紫: {
    id: '浅紫',
    name: '浅紫',
    primaryColor: '#722ed1',
    primaryLight: '#f9f0ff',
    primaryHover: 'rgba(114, 46, 209, 0.1)',
    primaryShadow: 'rgba(114, 46, 209, 0.3)',
    previewColor: '#f9f0ff',
    sidebarGradientStart: '',
    sidebarGradientEnd: '',
    sidebarType: 'light',
    sidebarText: '#595959',
    sidebarTextHover: '#722ed1',
    sidebarBackground: '#f9f0ff',
    sidebarBorder: '#efdbff'
  },
  默认蓝: {
    id: '默认蓝',
    name: '默认蓝',
    primaryColor: '#3370fe',
    primaryLight: '#eef3ff',
    primaryHover: 'rgba(51, 112, 254, 0.15)',
    primaryShadow: 'rgba(51, 112, 254, 0.25)',
    previewColor: '#3370fe',
    sidebarGradientStart: '#1c4cc0',
    sidebarGradientEnd: '#0f1c3f',
    sidebarType: 'dark',
    sidebarText: 'rgba(255, 255, 255, 0.75)',
    sidebarTextHover: '#fff',
    sidebarBackground: '',
    sidebarBorder: 'rgba(255, 255, 255, 0.04)'
  },
  紫罗兰: {
    id: '紫罗兰',
    name: '紫罗兰',
    primaryColor: '#6366f1',
    primaryLight: '#eef2ff',
    primaryHover: 'rgba(99, 102, 241, 0.15)',
    primaryShadow: 'rgba(99, 102, 241, 0.4)',
    previewColor: '#6366f1',
    sidebarGradientStart: '#312e81',
    sidebarGradientEnd: '#1e1b4b',
    sidebarType: 'dark',
    sidebarText: 'rgba(255, 255, 255, 0.75)',
    sidebarTextHover: '#fff',
    sidebarBackground: '',
    sidebarBorder: 'rgba(255, 255, 255, 0.04)'
  },
  青蓝: {
    id: '青蓝',
    name: '青蓝',
    primaryColor: '#0ea5e9',
    primaryLight: '#e0f2fe',
    primaryHover: 'rgba(14, 165, 233, 0.15)',
    primaryShadow: 'rgba(14, 165, 233, 0.4)',
    previewColor: '#0ea5e9',
    sidebarGradientStart: '#0c4a6e',
    sidebarGradientEnd: '#082f49',
    sidebarType: 'dark',
    sidebarText: 'rgba(255, 255, 255, 0.75)',
    sidebarTextHover: '#fff',
    sidebarBackground: '',
    sidebarBorder: 'rgba(255, 255, 255, 0.04)'
  }
}

/** 旧主题 id → 新主题 id（兼容 localStorage） */
export const THEME_ID_ALIASES: Record<string, string> = {
  default: '默认蓝',
  antdesign: '默认蓝',
  柔和蓝: '默认蓝',
  天蓝: '青蓝',
  深宝蓝: '默认蓝',
  温暖白: '默认白',
  浅灰: '浅灰'
}

/** 有序列表，供下拉渲染 */
export const themeList: ThemeConfig[] = Object.values(themes)

export default themes
