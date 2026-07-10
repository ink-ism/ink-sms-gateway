<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <h1>INK短信网关</h1>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.nickname }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="app-aside">
        <el-menu :default-active="activeMenu" router class="aside-menu">
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/channels">
            <el-icon><Connection /></el-icon>
            <span>通道管理</span>
          </el-menu-item>
          <el-sub-menu index="sms">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span>短信管理</span>
            </template>
            <el-menu-item index="/sms/down">
              <el-icon><Download /></el-icon>
              <span>下行短信</span>
            </el-menu-item>
            <el-menu-item index="/sms/up">
              <el-icon><Upload /></el-icon>
              <span>上行短信</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/profile">
            <el-icon><Setting /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="app-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, ArrowDown, HomeFilled, UserFilled, Setting, Connection, ChatDotRound, Download, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

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

.app-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.app-aside {
  background: #fff;
  border-right: 1px solid #e8e8e8;
}

.aside-menu {
  height: 100%;
  border-right: none;
}

.app-main {
  background: #f5f5f5;
  padding: 20px;
}
</style>
