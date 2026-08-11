import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('../views/Channels.vue'),
        meta: { title: '通道管理' }
      },
      {
        path: 'sp',
        name: 'SpManage',
        component: () => import('../views/SpManage.vue'),
        meta: { title: '客户管理' }
      },
      {
        path: 'blacklist',
        name: 'Blacklist',
        component: () => import('../views/Blacklist.vue'),
        meta: { title: '黑名单管理' }
      },
      {
        path: 'sms/down',
        name: 'SmsDown',
        component: () => import('../views/SmsDown.vue'),
        meta: { title: '下行短信' }
      },
      {
        path: 'sms/up',
        name: 'SmsUp',
        component: () => import('../views/SmsUp.vue'),
        meta: { title: '上行短信' }
      },
      {
        path: 'stats',
        name: 'Statistics',
        component: () => import('../views/Statistics.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'sign-template',
        name: 'SignTemplate',
        component: () => import('../views/SignTemplate.vue'),
        meta: { title: '签名模板' }
      },
      {
        path: 'sensitive',
        name: 'SensitiveWords',
        component: () => import('../views/SensitiveWords.vue'),
        meta: { title: '敏感词管理' }
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('../views/AuditLog.vue'),
        meta: { title: '审计日志' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else if (to.path === '/' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
