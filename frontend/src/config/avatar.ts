import user1 from '@/assets/avator/user1.jpg'
import user2 from '@/assets/avator/user2.jpg'
import user3 from '@/assets/avator/user3.jpg'
import user4 from '@/assets/avator/user4.jpg'

/** 预设头像 */
export const AVATAR_PRESETS = [
  { id: 'user1', key: 'preset:user1', src: user1, label: '默认' },
  { id: 'user2', key: 'preset:user2', src: user2, label: '预设 2' },
  { id: 'user3', key: 'preset:user3', src: user3, label: '预设 3' },
  { id: 'user4', key: 'preset:user4', src: user4, label: '预设 4' }
] as const

export const DEFAULT_AVATAR_KEY = 'preset:user1'
export const DEFAULT_AVATAR_SRC = user1

const PRESET_MAP: Record<string, string> = Object.fromEntries(
  AVATAR_PRESETS.map((p) => [p.key, p.src])
)

/**
 * 将库中的 avatarUrl 转为可展示的图片地址
 */
export function resolveAvatarSrc(avatarUrl?: string | null): string {
  if (!avatarUrl || !avatarUrl.trim()) {
    return DEFAULT_AVATAR_SRC
  }
  const value = avatarUrl.trim()
  if (PRESET_MAP[value]) {
    return PRESET_MAP[value]
  }
  if (value.startsWith('/uploads/')) {
    return value
  }
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }
  return DEFAULT_AVATAR_SRC
}

export function isPresetAvatar(avatarUrl?: string | null): boolean {
  return !!avatarUrl && /^preset:user[1-4]$/.test(avatarUrl.trim())
}
