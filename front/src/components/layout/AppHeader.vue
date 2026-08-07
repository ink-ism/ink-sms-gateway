<template>
  <header class="app-header">
    <!-- 面包屑（基于路由 meta.title） -->
    <div class="header-left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentPageTitle">{{ currentPageTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧操作区 -->
    <div class="header-right">
      <span class="env-badge">
        <span class="pulse-dot dot-online"></span>
        PROD
      </span>
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-dropdown-trigger">
          <div class="user-avatar">
            <el-icon :size="16"><User /></el-icon>
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
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前页面标题：取自路由 meta.title */
const currentPageTitle = computed(() => (route.meta.title as string) || '')

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.app-header {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(13, 20, 36, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  position: relative;
  z-index: 9;
}

.header-left :deep(.el-breadcrumb__inner) {
  color: var(--text-muted);
}

.header-left :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 环境徽标 */
.env-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--success);
  background: var(--success-soft);
  border: 1px solid rgba(0, 255, 163, 0.2);
  padding: 4px 10px;
  border-radius: 20px;
}

/* 用户下拉触发器 */
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px 5px 6px;
  border-radius: 24px;
  cursor: pointer;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast) ease;
}

.user-dropdown-trigger:hover {
  border-color: var(--border-glow);
  box-shadow: var(--glow-accent);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #04121a;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-strong) 100%);
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.arrow-icon {
  color: var(--text-muted);
  transition: transform var(--transition-fast) ease;
}

.user-dropdown-trigger:hover .arrow-icon {
  transform: rotate(180deg);
}
</style>
