import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { AuthToken } from '@/types/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise: Promise<string> | null = null

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `${auth.tokenType || 'Bearer'} ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message

    if (status === 401) {
      const auth = useAuthStore()
      const originalRequest = error.config as any
      const isRefreshRequest = String(originalRequest?.url || '').includes('/auth/refresh')
      if (auth.refreshToken && !originalRequest?._retry && !isRefreshRequest) {
        originalRequest._retry = true
        if (!refreshPromise) {
          refreshPromise = axios
            .post<AuthToken>(`${request.defaults.baseURL}/auth/refresh`, {
              refreshToken: auth.refreshToken,
            })
            .then((response) => {
              auth.applyToken(response.data)
              return response.data.accessToken
            })
            .finally(() => {
              refreshPromise = null
            })
        }
        return refreshPromise
          .then((accessToken) => {
            originalRequest.headers = originalRequest.headers || {}
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            return request(originalRequest)
          })
          .catch((refreshError) => {
            auth.logout()
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
            return Promise.reject(refreshError)
          })
      }
      auth.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else if (status === 404) {
      ElMessage.warning(message || '资源不存在')
    } else if (status >= 500) {
      ElMessage.error('服务端异常，请稍后重试')
    } else if (message) {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  },
)

export default request
