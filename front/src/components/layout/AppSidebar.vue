<template>
  <aside class="app-sidebar" :class="{ collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-logo" @click="router.push('/dashboard')">
      <div class="logo-icon">
        <el-icon :size="22"><ChatDotRound /></el-icon>
      </div>
      <span v-show="!collapsed" class="logo-text">INK<span class="logo-accent">OPS</span></span>
    </div>

    <!-- 导航菜单（配置驱动） -->
    <el-menu
      :default-active="activeMenu"
      :default-openeds="defaultOpeneds"
      :collapse="collapsed"
      :collapse-transition="false"
      router
      class="sidebar-menu"
    >
      <template v-for="item in menus" :key="item.key">
        <el-sub-menu v-if="item.children" :index="item.key">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            <el-icon><component :is="child.icon" /></el-icon>
            <template #title>
              <span>{{ child.title }}</span>
            </template>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path!">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>
            <span>{{ item.title }}</span>
          </template>
        </el-menu-item>
      </template>
    </el-menu>

    <!-- 折叠按钮 -->
    <div class="sidebar-collapse-btn" @click="emit('toggle')">
      <el-icon :size="18">
        <component :is="collapsed ? Expand : Fold" />
      </el-icon>
      <span v-show="!collapsed" class="collapse-text">收起菜单</span>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound, HomeFilled, UserFilled, User, Connection, Avatar, CircleClose,
  Download, Upload, Setting, Expand, Fold, TrendCharts, Document, Lock, Notebook, Operation
} from '@element-plus/icons-vue'

interface MenuItem {
  key: string
  title: string
  icon: Component
  path?: string
  children?: { path: string; title: string; icon: Component }[]
}

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ toggle: [] }>()

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

/** 当前路由所在分组自动展开 */
const defaultOpeneds = computed(() =>
  menus.filter(m => m.children?.some(c => c.path === route.path)).map(m => m.key)
)

/** 菜单配置：与路由一一对应 */
const menus: MenuItem[] = [
  { key: '/dashboard', path: '/dashboard', title: '仪表盘', icon: HomeFilled },
  { key: '/stats', path: '/stats', title: '数据统计', icon: TrendCharts },
  { key: '/sp', path: '/sp', title: '客户管理', icon: Avatar },
  { key: '/channels', path: '/channels', title: '通道管理', icon: Connection },
  {
    key: 'sms',
    title: '短信管理',
    icon: ChatDotRound,
    children: [
      { path: '/sms/down', title: '下行短信', icon: Download },
      { path: '/sms/up', title: '上行短信', icon: Upload }
    ]
  },
  {
    key: 'ops',
    title: '运营管理',
    icon: Operation,
    children: [
      { path: '/blacklist', title: '黑名单管理', icon: CircleClose },
      { path: '/sensitive', title: '敏感词管理', icon: Lock },
      { path: '/sign-template', title: '签名模板', icon: Document }
    ]
  },
  {
    key: 'sys',
    title: '系统管理',
    icon: Setting,
    children: [
      { path: '/users', title: '用户管理', icon: UserFilled },
      { path: '/audit', title: '审计日志', icon: Notebook },
      { path: '/profile', title: '个人中心', icon: User }
    ]
  }
]
</script>

<style scoped>
.app-sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: rgba(13, 20, 36, 0.85);
  backdrop-filter: blur(12px);
  border-right: 1px solid var(--border-light);
  transition: width var(--transition-normal) ease;
  position: relative;
  z-index: 10;
}

.app-sidebar.collapsed {
  width: 64px;
}

/* Logo */
.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light);
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #04121a;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-strong) 100%);
  box-shadow: var(--glow-accent);
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-primary);
}

.logo-accent {
  color: var(--accent);
  text-shadow: 0 0 12px rgba(34, 211, 238, 0.6);
}

/* 菜单 */
.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 12px 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: var(--radius-sm);
  position: relative;
  transition: all var(--transition-fast) ease;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
  box-shadow: var(--glow-accent);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
}

/* 折叠按钮 */
.sidebar-collapse-btn {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  cursor: pointer;
  color: var(--text-muted);
  border-top: 1px solid var(--border-light);
  white-space: nowrap;
  overflow: hidden;
  transition: all var(--transition-fast) ease;
}

.sidebar-collapse-btn:hover {
  color: var(--accent);
  background: var(--bg-hover);
}

.collapse-text {
  font-size: 13px;
}
</style>
