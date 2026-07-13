import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'

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
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('../views/Channels.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'sms/down',
        name: 'SmsDown',
        component: () => import('../views/SmsDown.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'sms/up',
        name: 'SmsUp',
        component: () => import('../views/SmsUp.vue'),
        meta: { requiresAuth: true }
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