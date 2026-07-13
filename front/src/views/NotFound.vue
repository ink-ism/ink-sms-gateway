<template>
  <div class="not-found-page">
    <!-- 背景 -->
    <div class="bg-gradient"></div>
    
    <!-- 装饰元素 -->
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>
    <div class="decoration decoration-3"></div>

    <!-- 主内容 -->
    <div class="content-wrapper">
      <div class="error-code">
        <span class="digit" v-for="(digit, index) in '404'.split('')" :key="index" :style="{ animationDelay: `${index * 0.1}s` }">
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
}

/* 背景 */
.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(-45deg, #0f172a, #1e293b, #1e3a5f, #0f172a);
  background-size: 400% 400%;
  animation: gradientShift 20s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 装饰元素 */
.decoration {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: float 8s ease-in-out infinite;
}

.decoration-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  left: 10%;
  background: rgba(59, 130, 246, 0.15);
  animation-delay: 0s;
}

.decoration-2 {
  width: 250px;
  height: 250px;
  bottom: 15%;
  right: 15%;
  background: rgba(139, 92, 246, 0.12);
  animation-delay: 2s;
}

.decoration-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 60%;
  background: rgba(16, 185, 129, 0.1);
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

/* 主内容 */
.content-wrapper {
  text-align: center;
  position: relative;
  z-index: 10;
  padding: 40px;
}

/* 404 数字 */
.error-code {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.digit {
  font-size: 140px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary-light) 0%, #a78bfa 50%, #34d399 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  animation: digitFloat 3s ease-in-out infinite;
}

.digit:nth-child(1) {
  animation-delay: 0s;
}

.digit:nth-child(2) {
  animation-delay: 0.2s;
}

.digit:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes digitFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 标题和描述 */
.error-title {
  font-size: 28px;
  font-weight: 600;
  color: white;
  margin: 0 0 12px;
}

.error-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
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
  transition: all var(--transition-normal) ease;
}

.home-btn {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border: none;
  color: white;
}

.home-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px -8px rgba(59, 130, 246, 0.5);
}

.back-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.15);
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
