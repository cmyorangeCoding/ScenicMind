export const TOKEN_KEY = 'admin_token'
export const PROFILE_KEY = 'admin_profile'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY)
    return
  }
  localStorage.setItem(TOKEN_KEY, String(token))
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)

  // 兼容历史/其它实现可能写入的 token key，避免“退出后仍残留”
  localStorage.removeItem('token')
  localStorage.removeItem('access_token')
  localStorage.removeItem('adminToken')
  localStorage.removeItem('Authorization')
}

// 登录回显信息（用于右上角显示账号/姓名）
// 由登录接口返回的 AdminLoginVO 中提取并存储，避免额外加一个 /me 接口也能展示
export function getProfile() {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setProfile(profile) {
  if (!profile) {
    localStorage.removeItem(PROFILE_KEY)
    return
  }
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function clearProfile() {
  localStorage.removeItem(PROFILE_KEY)
}
