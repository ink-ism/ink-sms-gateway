<template>
  <div class="not-found-page">
    <!-- 氛围光斑 -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- 主内容 -->
    <div class="content-wrapper">
      <div class="error-code mono">
        <span class="digit" v-for="(digit, index) in '404'.split('')" :key="index" :style="{ animationDelay: `${index * 0.2}s` }">
          {{ digit }}
        </span>
      </div>

      <h1 class="error-title">页面走丢了</h1>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>

      <div class="action-buttons">
        <el-button class="home-btn" @click="goHome" size="large">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button class="back-btn" @click="goBack" size="large">
          <el-icon><ArrowLeft /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HomeFilled, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-base);
}

/* 氛围光斑 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
}

.bg-glow-1 {
  width: 420px;
  height: 420px;
  top: 8%;
  left: 8%;
  background: rgba(34, 211, 238, 0.1);
}

.bg-glow-2 {
  width: 320px;
  height: 320px;
  bottom: 12%;
  right: 12%;
  background: rgba(255, 77, 109, 0.08);
}

/* 主内容 */
.content-wrapper {
  text-align: center;
  position: relative;
  z-index: 10;
  padding: 40px;
}

/* 404 霓虹大字 */
.error-code {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.digit {
  font-size: 140px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--accent) 0%, var(--violet) 55%, var(--success) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 24px rgba(34, 211, 238, 0.35));
  line-height: 1;
  animation: digitFloat 3s ease-in-out infinite;
}

@keyframes digitFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 标题和描述 */
.error-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.error-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0 0 40px;
}

/* 按钮组 */
.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.home-btn,
.back-btn {
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
}

.home-btn {
  background: var(--accent);
  border: none;
  color: #06121a;
}

.home-btn:hover {
  transform: translateY(-3px);
  box-shadow: var(--glow-accent);
  background: var(--accent);
  color: #06121a;
}

.back-btn {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.back-btn:hover {
  background: rgba(34, 211, 238, 0.08);
  border-color: rgba(34, 211, 238, 0.4);
  color: var(--accent);
  transform: translateY(-3px);
}

/* 响应式 */
@media (max-width: 768px) {
  .digit {
    font-size: 80px;
  }

  .error-title {
    font-size: 22px;
  }

  .error-desc {
    font-size: 14px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }

  .home-btn,
  .back-btn {
    width: 100%;
    max-width: 240px;
  }
}
</style>
