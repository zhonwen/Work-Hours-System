import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useProjectStore } from '@/stores/project'
import { message as toast } from '@/utils/message'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL,
  timeout: Number(import.meta.env.VITE_APP_API_TIMEOUT) || 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从 auth store 获取 token
    const authStore = useAuthStore()
    const token = authStore.token

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    // 检查是否是 PageResult 格式（有 total, pageNum, pageSize 等字段）
    // PageResult 继承自 Result<List<T>>，所以会有 code, message, data, total 等字段
    if (data.total !== undefined && (data.pageNum !== undefined || data.pageSize !== undefined)) {
      // 转换为前端期望的格式 { list: data.data, total: data.total, ... }
      return {
        list: data.data,
        total: data.total,
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        totalPages: data.totalPages
      }
    }

    // 处理标准 Result 格式
    const { code, message, data: resultData } = data

    // 成功响应（支持 code=200 和 code=0 两种格式）
    if (code === 200 || code === 0) {
      return resultData
    }

    // 业务错误（节流，避免与页面 catch 重复弹窗）
    toast.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    console.error('Response error:', error)

    // 处理 HTTP 错误状态码
    if (error.response) {
      const { status } = error.response

      switch (status) {
        case 401: {
          toast.error('未授权，请重新登录')
          useAuthStore().clearAuth()
          useThemeStore().clearPreference()
          useProjectStore().clearUserCache()
          router.push('/login')
          break
        }
        case 403:
          toast.error('没有权限访问')
          break
        case 404:
          toast.error('请求的资源不存在')
          break
        case 500:
          toast.error('服务器错误')
          break
        default:
          toast.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      toast.error('网络错误，请检查网络连接')
    } else {
      toast.error('请求失败，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default service
