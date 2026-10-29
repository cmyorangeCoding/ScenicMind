<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { clearProfile, clearToken, getProfile, getToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)

const activeMenu = computed(() => {
  return route.meta?.activeMenu || route.path
})

const accountName = computed(() => {
  if (!getToken()) return '未登录'
  const profile = getProfile()
  return profile?.realName || profile?.account || '管理员'
})

const passwordDialogOpen = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }]
}

const openPasswordDialog = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordDialogOpen.value = true
}

const submitPassword = async () => {
  // 对接后端：PUT /admin/account/password
  // 后端从拦截器中取 adminId（request attribute），前端只需要提交 oldPassword/newPassword
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate()
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  passwordLoading.value = true
  try {
    await http.put('/admin/account/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogOpen.value = false

    // 修改密码后：建议让用户重新登录
    // - 前端：清空 token/profile 并回到登录页
    // - 后端：可同时把旧 token 失效（如果你们后端有 token 黑名单/缓存）
    try {
      await http.post('/admin/account/logout')
    } catch {
    }
    clearToken()
    clearProfile()
    router.replace('/login')
  } catch (e) {
    ElMessage.error(e?.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const logout = async () => {
  // 对接后端：POST /admin/account/logout
  // 请求头 Authorization: Bearer <token>（由 http 拦截器自动携带）
  try {
    await ElMessageBox.confirm('确定退出登录？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await http.post('/admin/account/logout')
  } catch {
  } finally {
    clearToken()
    clearProfile()
    router.replace('/login')
  }
}

const onMenuSelect = (index) => {
  if (index) router.push(index)
}
</script>

<template>
  <div class="admin-shell">
    <el-container class="admin-container">
      <el-aside class="admin-aside" :width="collapsed ? '72px' : '220px'">
        <div class="aside-brand" :class="{ collapsed }">
          <div class="brand-mark" />
          <div v-if="!collapsed" class="brand-text">
            <div class="brand-title">灵山胜境</div>
            <div class="brand-sub">景区后台管理</div>
          </div>
        </div>

        <el-menu
          class="aside-menu"
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          @select="onMenuSelect"
        >
          <el-menu-item index="/admin">
            <span>数据大屏</span>
          </el-menu-item>
          <el-menu-item index="/admin/visitor-report">
            <span>游客感受度报告</span>
          </el-menu-item>
          <el-menu-item index="/admin/user-manage">
            <span>用户数据管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/complaint-manage">
            <span>投诉工单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/accounts">
            <span>账号管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/avatar-manage">
            <span>数字人管理</span>
          </el-menu-item>

          <el-sub-menu index="/admin/knowledge">
            <template #title>
              <span>知识库管理</span>
            </template>
            <el-menu-item index="/admin/knowledge/attractions">
              <span>景点库</span>
            </el-menu-item>
            <el-menu-item index="/admin/knowledge/docs">
              <span>通用知识库</span>
            </el-menu-item>
            <el-menu-item index="/admin/knowledge/hot-questions">
              <span>用户高频问题</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="aside-footer">
          <el-button class="collapse-btn" text @click="collapsed = !collapsed">
            {{ collapsed ? '展开' : '收起' }}
          </el-button>
        </div>
      </el-aside>

      <el-container class="admin-main">
        <el-header class="admin-header" height="56px">
          <div class="header-left">
            <div class="header-title">灵山胜境 · 管理端</div>
          </div>
          <div class="header-right">
            <el-dropdown trigger="click">
              <div class="account-chip">
                <span class="account-name">{{ accountName }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openPasswordDialog">
                    修改密码
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="logout">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="admin-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-model="passwordDialogOpen" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-shell {
  background: #f5f7f5;
}

.admin-container {
  display: flex;
  height: 100vh;
}

.admin-aside {
  background: linear-gradient(180deg, #0f2f1a 0%, #0b2314 100%);
  color: rgba(255, 255, 255, 0.88);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.aside-brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.brand-mark {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f0c000;
  box-shadow: 0 0 18px rgba(240, 192, 0, 0.45);
}

.brand-text {
  line-height: 1.1;
}

.brand-title {
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-sub {
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.72;
}

/* 关键修改：菜单占满剩余高度，折叠按钮固定到底部 */
.aside-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding-top: 8px;
  overflow-y: auto;
}

/* 一级菜单样式 */
.aside-menu :deep(.el-menu-item),
.aside-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.92);
  background: transparent !important;
  padding-left: 22px !important;
  justify-content: flex-start !important;
  text-align: left;
}
.aside-menu :deep(.el-menu-item:hover),
.aside-menu :deep(.el-sub-menu__title:hover) {
  color: #ffffff;
  background: rgba(92, 131, 67, 0.24) !important;
}
.aside-menu :deep(.el-menu-item.is-active),
.aside-menu :deep(.el-sub-menu.is-active > .el-submenu__title) {
  color: #ffffff !important;
  background: rgba(80, 128, 60, 0.28) !important;
}
.aside-menu :deep(.el-menu-item.is-active) {
  border-left: 3px solid #f0c000;
  padding-left: calc(22px - 3px) !important;
}
.aside-menu :deep(.el-sub-menu__title) {
  padding-left: 22px !important;
}
.aside-menu :deep(.el-sub-menu__title.is-active) {
  background: rgba(80, 128, 60, 0.28) !important;
}
.aside-menu :deep(.el-menu-item),
.aside-menu :deep(.el-sub-menu__title) {
  border-radius: 0;
}

/* 二级子菜单 */
.aside-menu :deep(.el-sub-menu__wrap),
.aside-menu :deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}
.aside-menu :deep(.el-sub-menu .el-menu-item) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.92) !important;
  padding-left: 42px !important;
  justify-content: flex-start !important;
}
.aside-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  border-left: 3px solid #f0c000;
  padding-left: calc(42px - 3px) !important;
  background: rgba(80, 128, 60, 0.28) !important;
}
.aside-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background: rgba(92, 131, 67, 0.24) !important;
  color: #fff !important;
}

/* 底部折叠按钮固定在最下方 */
.aside-footer {
  margin-top: auto;
  padding: 10px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.collapse-btn {
  width: 100%;
  justify-content: center;
  color: rgba(255, 255, 255, 0.75);
}

.admin-main {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.admin-header {
  background: linear-gradient(90deg, #1b5e20 0%, #2e7d32 60%, #3b8d3f 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.header-title {
  font-weight: 700;
  letter-spacing: 0.8px;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.22);
  cursor: pointer;
  user-select: none;
}

.account-name {
  font-size: 13px;
  color: #ffffff;
  font-weight: 500;
}

.admin-content {
  padding: 18px;
  position: relative;
  flex: 1;
  overflow: auto;
}

.admin-content::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url('/assets/lingshan.jpg');
  background-size: cover;
  background-position: center;
  opacity: 0.06;
  pointer-events: none;
  z-index: 0;
}

.admin-content > * {
  position: relative;
  z-index: 1;
}
</style>