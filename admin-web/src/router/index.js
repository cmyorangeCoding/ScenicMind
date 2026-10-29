import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import Dashboard from '../views/Dashboard.vue'
import VisitorReport from '../views/VisitorReport.vue'
import AccountManagement from '../views/AccountManagement.vue'
import AttractionManagement from '../views/AttractionManagement.vue'
import KnowledgeDocManagement from '../views/KnowledgeDocManagement.vue'
import HotQuestionManage from '../views/HotQuestionManage.vue'
import AvatarManage from '../views/AvatarManage.vue'
import UserManage from '../views/UserManage.vue'
import ComplaintManage from '../views/ComplaintManage.vue'
import { getToken } from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: Dashboard,
        meta: { requiresAuth: true, activeMenu: '/admin' }
      },
      {
        path: 'visitor-report',
        name: 'VisitorReport',
        component: VisitorReport,
        meta: { requiresAuth: true, activeMenu: '/admin/visitor-report' }
      },
      {
        path: 'accounts',
        name: 'AccountManagement',
        component: AccountManagement,
        meta: { requiresAuth: true, activeMenu: '/admin/accounts' }
      },
      {
        path: 'knowledge/attractions',
        name: 'AttractionManagement',
        component: AttractionManagement,
        meta: { requiresAuth: true, activeMenu: '/admin/knowledge/attractions' }
      },
      {
        path: 'knowledge/docs',
        name: 'KnowledgeDocManagement',
        component: KnowledgeDocManagement,
        meta: { requiresAuth: true, activeMenu: '/admin/knowledge/docs' }
      },
      {
        path: 'knowledge/hot-questions',
        name: 'HotQuestionManage',
        component: HotQuestionManage,
        meta: { requiresAuth: true, activeMenu: '/admin/knowledge/hot-questions' }
      },
      {
        path: 'avatar-manage',
        name: 'AvatarManage',
        component: AvatarManage,
        meta: { requiresAuth: true, activeMenu: '/admin/avatar-manage' }
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        component: UserManage,
        meta: { requiresAuth: true, activeMenu: '/admin/user-manage' }
      },
      {
        path: 'complaint-manage',
        name: 'ComplaintManage',
        component: ComplaintManage,
        meta: { requiresAuth: true, activeMenu: '/admin/complaint-manage' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  // 仅对后台路由做登录态拦截（requiresAuth）
  // 未登录会被重定向回登录页
  if (to.path === '/login') return true
  if (to.meta?.requiresAuth && !getToken()) {
    return { path: '/login', replace: true }
  }
  return true
})

export default router
