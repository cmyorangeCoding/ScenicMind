<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { clearProfile, clearToken, setProfile, setToken } from '../utils/auth'

const router = useRouter()
const loginForm = reactive({
  account: '',
  password: ''
})

const loading = ref(false)
const formRef = ref(null)
const showPassword = ref(false)

const rules = {
  account: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' }
  ]
}

const submitLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    // 后端 AdminLoginDTO 常见字段是 account/password
    // 为了兼容你后端字段命名，前端以 account 为准
    const account = String(loginForm.account || '').trim()
    const password = String(loginForm.password || '')

    // 支持“切换账号登录”：每次尝试登录前先清理旧登录态
    clearToken()
    clearProfile()

    // 你的后端 DTO 已明确是 AdminLoginDTO，字段应为 account/password。
    // 这里不再做多字段“重试”，避免后端先校验 account 导致出现“账号不能为空”等误导性提示。
    const payload = { account, password }

    const isSuccess = (result) => {
      const code = result?.code
      if (typeof code === 'number') return code === 1 || code === 200
      if (typeof code === 'string') return code === '1' || code === '200'
      if (typeof result?.success === 'boolean') return result.success
      return Boolean(result?.data)
    }

    const pickMessage = (result, fallback) => {
      return result?.msg || result?.message || result?.error || fallback
    }


    const normalizeLoginError = (raw) => {
      const msg = String(raw || '').trim()
      if (!msg) return '登录失败，请重试'
      if (msg.includes('禁用') || msg.includes('已停用') || msg.includes('停用') || msg.includes('冻结')) {
        return '账号已禁用！请联系超级管理员'
      }
      if (msg.includes('密码') || msg.includes('账号或密码') || msg.includes('账号密码') || msg.includes('错误')) {
        return '账号密码错误'
      }
      return msg
    }

    const response = await http.post('/admin/account/login', payload, {
      headers: { 'Content-Type': 'application/json' },
      withCredentials: true
    })

    const result = response?.data
    if (!isSuccess(result)) {
      throw new Error(normalizeLoginError(pickMessage(result, '登录失败，请重试')))
    }

    const token =
      result?.data?.token ||
      result?.data?.accessToken ||
      result?.data?.jwt ||
      result?.token

    if (token) setToken(token)

    // 登录回显：如果后端 AdminLoginVO 返回了管理员信息，则存起来用于右上角展示
    const profile = {
      adminId: result?.data?.adminId,
      realName: result?.data?.realName,
      account: result?.data?.account
    }
    if (profile.adminId || profile.realName || profile.account) {
      setProfile(profile)
    }

    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (error) {
    const msg = error?.message || '登录请求发生异常'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <!-- 背景层 -->
    <div class="bg-wrapper">
      <img src="/assets/lingshan.jpg" alt="灵山胜境背景" class="bg-image" />
      <div class="bg-overlay"></div>
      <div class="bg-grid"></div>
      <div class="buddha-light"></div>
    </div>

    <!-- 主体内容 -->
    <main class="content-wrapper">
      <header class="brand-header">
        <div class="lotus-icon">✦</div>
        <div class="brand-text">
          <h1>灵山胜境 · 管理端</h1>
          <p>数字人景区导览系统</p>
        </div>
      </header>

      <section class="login-card">
        <h2 class="login-title">管理员登录</h2>
        
        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
        >
          <el-form-item prop="account" class="form-item">
            <label class="field-label">账号</label>
            <el-input 
              v-model="loginForm.account" 
              placeholder="请输入账号"
              class="custom-input"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password" class="form-item">
            <label class="field-label">密码</label>
            <el-input 
              v-model="loginForm.password" 
              :type="showPassword ? 'text' : 'password'" 
              placeholder="请输入密码"
              class="custom-input password-input"
              size="large"
            >
              <template #suffix>
                <button 
                  type="button" 
                  class="password-toggle"
                  @click="showPassword = !showPassword"
                  title="显示/隐藏密码"
                >
                  <span class="password-icon">{{ showPassword ? '👁️' : '👁️‍🗨️' }}</span>
                </button>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button 
              type="primary" 
              class="login-button" 
              :loading="loading" 
              @click="submitLogin"
              :disabled="loading"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <footer class="page-footer">
        <small>© 灵山胜境 · 数字人导览系统</small>
      </footer>
    </main>
  </div>
</template>

<style scoped>
:root {
  --green-900: #0f3d1f;
  --green-800: #1b5e20;
  --green-700: #2e7d32;
  --green-400: #66bb6a;
  --accent-yellow: #f0c000;
  --glass-bg: rgba(10, 30, 15, 0.45);
  --glass-stroke: rgba(240, 240, 240, 0.18);
  --text-primary: #e9f5ea;
  --text-muted: #cfe7d2;
  --danger: #ff6b6b;
}

.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 背景层样式 */
.bg-wrapper {
  position: fixed;
  inset: 0;
  isolation: isolate;
}

.bg-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.08) contrast(1.06) brightness(0.98);
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(60% 60% at 72% 18%, rgba(240, 192, 0, 0.10), transparent 58%),
    radial-gradient(80% 80% at 20% 65%, rgba(46, 125, 50, 0.12), transparent 60%),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.28));
}

.bg-grid {
  position: absolute;
  inset: 0;
  --c: rgba(240, 192, 0, 0.08);
  background:
    linear-gradient(transparent 31px, var(--c) 32px, transparent 33px) 0 0/100% 32px,
    linear-gradient(90deg, transparent 31px, var(--c) 32px, transparent 33px) 0 0/32px 100%;
  mask-image: radial-gradient(ellipse at 60% 30%, rgba(255, 255, 255, 0.6), transparent 70%);
  pointer-events: none;
  opacity: 0.75;
  animation: gridMove 28s linear infinite;
}

.buddha-light {
  position: absolute;
  top: 10%;
  left: 50%;
  width: 400px;
  height: 600px;
  background: radial-gradient(ellipse at top, rgba(240, 192, 0, 0.10), transparent 70%);
  transform: translateX(-50%);
  animation: lightPulse 8s ease-in-out infinite;
  pointer-events: none;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(32px, 32px); }
}

@keyframes lightPulse {
  0%, 100% { opacity: 0.5; transform: translateX(-50%) scale(1); }
  50% { opacity: 1; transform: translateX(-50%) scale(1.1); }
}

/* 主体内容区 */
.content-wrapper {
  position: relative;
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr auto;
  padding: 3vh 4vw;
  z-index: 10;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 1vh 0 2vh;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.45);
}

.lotus-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: radial-gradient(closest-side, rgba(240, 192, 0, 0.85), rgba(240, 192, 0, 0.25), transparent);
  box-shadow: 0 0 32px rgba(240, 192, 0, 0.25);
  color: #fff;
  font-size: 18px;
  filter: drop-shadow(0 0 6px rgba(240, 192, 0, 0.6));
  animation: lotusGlow 4s ease-in-out infinite;
}

@keyframes lotusGlow {
  0%, 100% { box-shadow: 0 0 32px rgba(240, 192, 0, 0.25); }
  50% { box-shadow: 0 0 48px rgba(240, 192, 0, 0.5); }
}

.brand-text h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: #ffffff;
  text-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.5),
    0 0 20px rgba(240, 192, 0, 0.25);
}

.brand-text p {
  margin: 4px 0 0;
  color: #e8f5ea;
  font-size: 14px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}

/* 登录卡片 */
.login-card {
  width: min(420px, 92vw);
  margin: 4vh 0 0;
  padding: 32px 28px 26px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(10, 25, 15, 0.55);
  backdrop-filter: blur(8px);
  box-shadow: 
    0 8px 25px rgba(0, 0, 0, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
  position: relative;
  overflow: hidden;
}



.login-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.5),
    0 0 20px rgba(240, 192, 0, 0.3);
  letter-spacing: 0.5px;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-item {
  margin-bottom: 12px !important;
}

.field-label {
  display: block;
  font-size: 14px;
  color: #d4e8d7;
  margin-bottom: 8px;
  font-weight: 500;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.custom-input :deep(.el-input__wrapper) {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  box-shadow: none;
  transition: all .2s ease;
}

.custom-input :deep(.el-input__inner) {
  color: #ffffff;
  background: transparent;
  font-size: 14px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: rgba(200, 230, 205, 0.5);
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.35);
}

.custom-input :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: rgba(240, 192, 0, 0.7);
  box-shadow: 0 0 0 2px rgba(240, 192, 0, 0.15);
}

.custom-input :deep(.el-input__inner):-webkit-autofill,
.custom-input :deep(.el-input__inner):-webkit-autofill:hover,
.custom-input :deep(.el-input__inner):-webkit-autofill:focus,
.custom-input :deep(.el-input__inner):-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 100px rgba(10, 25, 15, 0.7) inset !important;
  -webkit-text-fill-color: #ffffff !important;
  background-color: transparent !important;
}

.password-input :deep(.el-input__wrapper) {
  padding-right: 40px !important;
}

.password-input :deep(.el-input__suffix) {
  padding: 0;
}

.password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .2s ease;
  color: rgba(200, 230, 205, 0.7);
}

.password-toggle:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
}

.password-icon {
  font-size: 14px;
  line-height: 1;
}

/* Element Plus错误提示样式覆盖 */
:deep(.el-form-item__error) {
  color: #ffb4b4 !important;
}

.form-actions {
  margin-top: 8px !important;
}

.login-button {
  width: 100%;
  padding: 12px 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: linear-gradient(90deg, rgba(46, 125, 50, 0.9), rgba(27, 94, 32, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  color: #fff;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.25);
  transition: all .2s ease;
  height: auto;
  font-size: 15px;
}

.login-button:hover:not(:disabled) {
  filter: brightness(1.06);
}

.login-button:active:not(:disabled) {
  transform: translateY(1px);
}

.page-footer {
  margin-top: 4vh;
  color: var(--text-muted);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
  font-size: 12px;
}

/* 响应式布局 */
@media (min-width: 900px) {
  .content-wrapper {
    grid-template-columns: 1fr auto;
    grid-template-rows: 1fr auto;
    align-items: center;
  }
  
  .brand-header {
    grid-column: 1 / 2;
    grid-row: 1 / 2;
  }
  
  .login-card {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
    margin: 0;
  }
  
  .page-footer {
    grid-column: 2 / 3;
    grid-row: 2 / 3;
    justify-self: end;
    margin-top: 0;
  }
}

@media (max-width: 500px) {
  .login-card {
    padding: 24px 20px;
  }
  
  .brand-text h1 {
    font-size: 18px;
  }
}
</style>
