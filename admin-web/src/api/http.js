import axios from 'axios'
import { clearToken, getToken } from '../utils/auth'

// axios 实例：统一 baseURL、超时、以及 Authorization 头
// - 后端约定：请求头 Authorization: Bearer <token>
// - Vite 代理：/api -> http://localhost:8080
const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  // 登录/登出接口不需要主动附带旧 token（避免切换账号时被历史 token 干扰）
  const url = String(config?.url || '')
  const skipAuth = url.includes('/admin/account/login')

  if (skipAuth) return config

  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一把后端返回的 msg/message 映射到 error.message，便于页面直接提示
    const msg =
      error?.response?.data?.msg ||
      error?.response?.data?.message ||
      error?.message ||
      '请求失败'

    if (String(msg).includes('未登录') || error?.response?.status === 401) {
      clearToken()
    }
    return Promise.reject({ ...error, message: msg })
  }
)

export default http
