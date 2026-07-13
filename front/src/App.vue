<template>
  <el-config-provider :locale="zhCn">
    <div id="app">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style>
/* CSS 变量体系 - 全局主题 */
:root {
  /* 主题色 */
  --primary-color: #3b82f6;
  --primary-dark: #2563eb;
  --primary-light: #60a5fa;
  
  /* 侧边栏 */
  --sidebar-bg: #1e293b;
  --sidebar-hover: rgba(59, 130, 246, 0.15);
  --sidebar-active: rgba(59, 130, 246, 0.25);
  
  /* 背景色 */
  --bg-main: #f0f2f5;
  --bg-card: #ffffff;
  
  /* 文本色 */
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-placeholder: #9ca3af;
  
  /* 边框 */
  --border-color: #e5e7eb;
  --border-radius: 12px;
  --border-radius-sm: 8px;
  --border-radius-lg: 16px;
  
  /* 阴影 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  
  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  
  /* 过渡动画 */
  --transition-fast: 0.15s;
  --transition-normal: 0.3s;
  --transition-slow: 0.5s;
}

#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
  background: var(--bg-main);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: inherit;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-primary);
  background: var(--bg-main);
}

/* 页面过渡动画 */
.page-fade-enter-active {
  animation: pageFadeIn var(--transition-normal) ease-out;
}

.page-fade-leave-active {
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

/* 卡片入场动画 */
.card-stagger-enter-active {
  animation: cardSlideUp 0.4s ease-out;
  animation-fill-mode: both;
}

.card-stagger-enter-active:nth-child(1) { animation-delay: 0.05s; }
.card-stagger-enter-active:nth-child(2) { animation-delay: 0.1s; }
.card-stagger-enter-active:nth-child(3) { animation-delay: 0.15s; }
.card-stagger-enter-active:nth-child(4) { animation-delay: 0.2s; }
.card-stagger-enter-active:nth-child(5) { animation-delay: 0.25s; }
.card-stagger-enter-active:nth-child(6) { animation-delay: 0.3s; }

@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 浮动动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 发光效果 */
@keyframes glow {
  0%, 100% {
    box-shadow: 0 0 5px rgba(59, 130, 246, 0.5);
  }
  50% {
    box-shadow: 0 0 20px rgba(59, 130, 246, 0.8);
  }
}
</style>