import { ElMessage } from 'element-plus'
import type { MessageHandler } from 'element-plus'

/** 相同 type + 文案在该时间窗内只展示一次 */
const THROTTLE_MS = 2000
const recentKeys = new Map<string, number>()

function shouldShow(type: string, msg: string): boolean {
  const key = `${type}:${msg}`
  const now = Date.now()
  const last = recentKeys.get(key)
  if (last !== undefined && now - last < THROTTLE_MS) {
    return false
  }
  recentKeys.set(key, now)
  if (recentKeys.size > 50) {
    for (const [k, t] of recentKeys) {
      if (now - t >= THROTTLE_MS) recentKeys.delete(k)
    }
  }
  return true
}

function show(
  type: 'success' | 'warning' | 'info' | 'error',
  message: string
): MessageHandler | undefined {
  if (!shouldShow(type, message)) return undefined
  return ElMessage({ type, message, grouping: true })
}

/** 带节流的全局消息，避免相同提示短时间内重复弹出 */
export const message = {
  success: (msg: string) => show('success', msg),
  warning: (msg: string) => show('warning', msg),
  info: (msg: string) => show('info', msg),
  error: (msg: string) => show('error', msg)
}
