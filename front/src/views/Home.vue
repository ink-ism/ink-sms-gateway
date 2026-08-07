<template>
  <div class="home-page">
    <!-- 氛围背景 -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <header class="home-header">
      <div class="header-content">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
          <span class="logo-text">INK短信网关</span>
          <span class="logo-version mono">v1.0.0</span>
        </div>
        <div class="nav-actions">
          <el-button class="nav-btn" @click="goToLogin">登录</el-button>
          <el-button class="nav-btn nav-btn-primary" @click="goToRegister">注册</el-button>
        </div>
      </div>
    </header>

    <main class="home-main">
      <section class="hero-section">
        <div class="hero-tag mono">
          <span class="pulse-dot dot-online"></span>
          CMPP 2.0 / 2.1 · SPRING CLOUD
        </div>
        <h1 class="hero-title">
          <span class="title-line">企业级</span>
          <span class="title-gradient">短信网关管理平台</span>
        </h1>
        <p class="hero-subtitle">基于 Spring Cloud 架构，提供高可用、高性能的短信收发服务</p>
        <div class="hero-actions">
          <el-button class="cta-btn cta-btn-primary" @click="goToLogin" size="large">
            立即登录
            <el-icon style="margin-left: 8px"><ChatDotRound /></el-icon>
          </el-button>
          <el-button class="cta-btn cta-btn-secondary" @click="goToRegister" size="large">注册账号</el-button>
        </div>
      </section>

      <section class="features-section">
        <div class="feature-card" :class="`feature-${feature.tone}`" v-for="(feature, index) in features" :key="index">
          <div class="feature-icon">
            <el-icon :size="24"><component :is="feature.icon" /></el-icon>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.desc }}</p>
        </div>
      </section>
    </main>

    <footer class="home-footer">
      <p class="mono">INK短信网关 v1.0.0 · 基于 Spring Cloud + Vue 3 构建</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ChatDotRound, Connection, DataLine, CircleCheck } from '@element-plus/icons-vue'

const router = useRouter()

const features = [
  {
    icon: Connection,
    title: '多通道支持',
    desc: '支持多个 CMPP 通道接入，自动负载均衡，确保短信发送的高可用性',
    tone: 'accent'
  },
  {
    icon: DataLine,
    title: '实时监控',
    desc: '实时展示通道连接状态、短信发送成功率、延迟等关键指标',
    tone: 'violet'
  },
  {
    icon: CircleCheck,
    title: '安全可靠',
    desc: '完善的权限管理体系，操作日志审计，保障系统安全稳定运行',
    tone: 'success'
  }
]

const goToLogin = () => { router.push('/login') }
const goToRegister = () => { router.push('/register') }
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
  width: 560px;
  height: 560px;
  top: -180px;
  right: -120px;
  background: rgba(34, 211, 238, 0.12);
}

.bg-glow-2 {
  width: 420px;
  height: 420px;
  bottom: 5%;
  left: -120px;
  background: rgba(167, 139, 250, 0.1);
}

/* 顶部导航 */
.home-header {
  position: relative;
  z-index: 100;
  padding: 20px 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: var(--accent-soft);
  border: 1px solid var(--accent);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  box-shadow: var(--glow-accent);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.logo-version {
  font-size: 11px;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 2px 6px;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.nav-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.nav-btn:hover {
  background: rgba(34, 211, 238, 0.08);
  border-color: rgba(34, 211, 238, 0.4);
  color: var(--accent);
  transform: translateY(-2px);
}

.nav-btn-primary {
  background: var(--accent);
  border-color: var(--accent);
  color: #06121a;
}

.nav-btn-primary:hover {
  color: #06121a;
  background: var(--accent);
  box-shadow: var(--glow-accent);
}

/* 主区域 */
.home-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 10;
  padding: 60px 32px;
}

.hero-section {
  text-align: center;
  max-width: 820px;
  margin: 0 auto;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 20px;
  padding: 6px 16px;
  margin-bottom: 28px;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 24px;
}

.title-line {
  display: block;
  color: var(--text-primary);
}

.title-gradient {
  display: block;
  background: linear-gradient(135deg, var(--accent) 0%, var(--violet) 55%, var(--success) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 18px;
  color: var(--text-secondary);
  margin: 0 0 40px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.cta-btn {
  padding: 14px 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

.cta-btn-primary {
  background: var(--accent);
  border: none;
  color: #06121a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cta-btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: var(--glow-accent);
  background: var(--accent);
  color: #06121a;
}

.cta-btn-secondary {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.cta-btn-secondary:hover {
  background: rgba(34, 211, 238, 0.08);
  border-color: rgba(34, 211, 238, 0.4);
  color: var(--accent);
  transform: translateY(-3px);
}

/* 特性卡片 */
.features-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  max-width: 1000px;
  margin: 80px auto 0;
}

.feature-card {
  background: var(--bg-panel);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 32px;
  text-align: center;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-8px);
  border-color: rgba(34, 211, 238, 0.35);
  box-shadow: 0 20px 40px -18px rgba(0, 0, 0, 0.6), 0 0 24px -8px rgba(34, 211, 238, 0.25);
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.feature-accent .feature-icon {
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid rgba(34, 211, 238, 0.25);
}

.feature-violet .feature-icon {
  color: var(--violet);
  background: var(--violet-soft);
  border: 1px solid rgba(167, 139, 250, 0.25);
}

.feature-success .feature-icon {
  color: var(--success);
  background: var(--success-soft);
  border: 1px solid rgba(0, 255, 163, 0.25);
}

.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.feature-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* 页脚 */
.home-footer {
  position: relative;
  z-index: 10;
  padding: 24px 32px;
  text-align: center;
  border-top: 1px solid var(--border-light);
}

.home-footer p {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .features-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .cta-btn {
    width: 100%;
    max-width: 280px;
  }
}
</style>
