import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as userApi from '../api/user'
import type { User, LoginRequest } from '../types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

  /** 登录 */
  async function login(credentials: LoginRequest) {
    const res = await userApi.login(credentials)
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
    return res
  }

  /** 登出 */
  async function logout() {
    try {
      await userApi.logout()
    } catch {
      // 即使接口失败也清除本地状态
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  /** 获取用户信息 */
  async function fetchUserInfo() {
    const res = await userApi.getUserInfo()
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    return res.data
  }

  /** 清除状态 */
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    login,
    logout,
    fetchUserInfo,
    clearAuth
  }
})
