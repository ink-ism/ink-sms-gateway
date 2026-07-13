<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-sidebar">
      <!-- Logo 区域 -->
      <div class="sidebar-logo" @click="router.push('/dashboard')">
        <div class="logo-icon">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <transition name="logo-text">
          <span v-show="!isCollapsed" class="logo-text">INK短信网关</span>
        </transition>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        class="sidebar-menu"
        :collapse-transition="false"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>
            <span>首页</span>
          </template>
        </el-menu-item>
        
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>
            <span>用户管理</span>
          </template>
        </el-menu-item>
        
        <el-menu-item index="/channels">
          <el-icon><Connection /></el-icon>
          <template #title>
            <span>通道管理</span>
          </template>
        </el-menu-item>
        
        <el-sub-menu index="sms">
          <template #title>
            <el-icon><ChatDotRound /></el-icon>
            <span>短信管理</span>
          </template>
          <el-menu-item index="/sms/down">
            <el-icon><Download /></el-icon>
            <template #title>
              <span>下行短信</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/sms/up">
            <el-icon><Upload /></el-icon>
            <template #title>
              <span>上行短信</span>
            </template>
          </el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/profile">
          <el-icon><Setting /></el-icon>
          <template #title>
            <span>个人中心</span>
          </template>
        </el-menu-item>
      </el-menu>

      <!-- 折叠按钮 -->
      <div class="sidebar-collapse-btn" @click="toggleCollapse">
        <el-icon :size="20">
          <component :is="isCollapsed ? Expand : Fold" />
        </el-icon>
        <transition name="collapse-text">
          <span v-show="!isCollapsed" class="collapse-text">收起菜单</span>
        </transition>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="app-header">
        <!-- 面包屑 -->
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentPageName">{{ currentPageName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 右侧操作区 -->
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-dropdown-trigger">
              <div class="user-avatar">
                <el-icon :size="18"><User /></el-icon>
              </div>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  User, ArrowDown, HomeFilled, UserFilled, Setting, Connection, 
  ChatDotRound, Download, Upload, Expand, Fold, SwitchButton 
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)

// 当前页面名称
const pageNameMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/users': '用户管理',
  '/channels': '通道管理',
  '/sms/down': '下行短信',
  '/sms/up': '上行短信',
  '/profile': '个人中心'
}
const currentPageName = computed(() => pageNameMap[route.path] || '')

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    handleLogout()
  }
}

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

/* 侧边栏 */
.app-sidebar {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal) ease;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  cursor: pointer;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(99, 102, 241, 0.1) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: white;
  white-space: nowrap;
}

.logo-text-enter-active,
.logo-text-leave-active {
  transition: opacity var(--transition-normal) ease;
}

.logo-text-enter-from,
.logo-text-leave-to {
  opacity: 0;
}

/* 侧边栏菜单 */
.sidebar-menu {
  flex: 1;
  background: transparent;
  border-right: none;
  padding: 12px 0;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  transition: all var(--transition-normal) ease;
  position: relative;
}

.sidebar-menu :deep(.el-menu-item::before),
.sidebar-menu :deep(.el-sub-menu__title::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: var(--primary-color);
  border-radius: 0 3px 3px 0;
  transition: height var(--transition-normal) ease;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--sidebar-hover);
  color: white;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-active);
  color: white;
}

.sidebar-menu :deep(.el-menu-item.is-active::before),
.sidebar-menu :deep(.el-sub-menu.is-opened .el-sub-menu__title::before) {
  height: 24px;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 56px !important;
  min-width: auto;
}

.sidebar-menu :deep(.el-sub-menu.is-opened) {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
  margin: 4px 8px;
}

.sidebar-menu :deep(.el-sub-menu.is-opened .el-menu) {
  background: transparent;
}

/* 折叠按钮 */
.sidebar-collapse-btn {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  transition: all var(--transition-normal) ease;
}

.sidebar-collapse-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.05);
}

.collapse-text {
  font-size: 13px;
  white-space: nowrap;
}

.collapse-text-enter-active,
.collapse-text-leave-active {
  transition: opacity var(--transition-normal) ease;
}

.collapse-text-enter-from,
.collapse-text-leave-to {
  opacity: 0;
}

/* 主容器 */
.main-container {
  flex-direction: column;
  overflow: hidden;
}

/* 顶部栏 */
.app-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left :deep(.el-breadcrumb__inner) {
  color: var(--text-secondary);
}

.header-left :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 用户下拉触发器 */
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 24px;
  cursor: pointer;
  transition: all var(--transition-normal) ease;
  background: rgba(59, 130, 246, 0.05);
}

.user-dropdown-trigger:hover {
  background: rgba(59, 130, 246, 0.1);
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.arrow-icon {
  color: var(--text-secondary);
  transition: transform var(--transition-normal) ease;
}

.user-dropdown-trigger:hover .arrow-icon {
  transform: rotate(180deg);
}

/* 主内容区 */
.app-main {
  background: var(--bg-main);
  padding: 0;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 页面过渡动画 - 在 AppLayout 内部生效 */
.app-main :deep(.page-fade-enter-active) {
  animation: pageFadeIn var(--transition-normal) ease-out;
}

.app-main :deep(.page-fade-leave-active) {
  animation: pageFadeOut var(--transition-normal) ease-in;
}

@keyframes pageFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pageFadeOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-20px);
  }
}

/* 页面内容容器样式 - 通过 :deep() 选择器应用于子路由根元素 */
.app-main :deep(.dashboard-page),
.app-main :deep(.channels-page),
.app-main :deep(.users-page),
.app-main :deep(.profile-page),
.app-main :deep(.sms-down-page),
.app-main :deep(.sms-up-page) {
  padding: 24px;
  max-width: 100%;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
  flex: 1;
}
</style>
