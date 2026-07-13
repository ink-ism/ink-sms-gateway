<template>
  <div class="home-page">
    <div class="bg-gradient"></div>
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>
    <div class="decoration decoration-3"></div>

    <header class="home-header">
      <div class="header-content">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
          <span class="logo-text">INK短信网关</span>
        </div>
        <div class="nav-actions">
          <el-button class="nav-btn" @click="goToLogin">登录</el-button>
          <el-button class="nav-btn nav-btn-primary" @click="goToRegister">注册</el-button>
        </div>
      </div>
    </header>

    <main class="home-main">
      <section class="hero-section">
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
        <div class="feature-card" v-for="(feature, index) in features" :key="index">
          <div class="feature-icon" :style="{ background: feature.gradient }">
            <el-icon :size="24"><component :is="feature.icon" /></el-icon>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.desc }}</p>
        </div>
      </section>
    </main>

    <footer class="home-footer">
      <p>INK短信网关 v1.0.0 | 基于 Spring Cloud + Vue 3 构建</p>
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
    gradient: 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)'
  },
  {
    icon: DataLine,
    title: '实时监控',
    desc: '实时展示通道连接状态、短信发送成功率、延迟等关键指标',
    gradient: 'linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%)'
  },
  {
    icon: CircleCheck,
    title: '安全可靠',
    desc: '完善的权限管理体系，操作日志审计，保障系统安全稳定运行',
    gradient: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)'
  }
]

const goToLogin = () => { router.push('/login') }
const goToRegister = () => { router.push('/register') }
</script>

<style scoped>
.home-page { min-height: 100vh; display: flex; flex-direction: column; position: relative; overflow: hidden; }
.bg-gradient { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(-45deg, #0f172a, #1e293b, #1e3a5f, #0f172a); background-size: 400% 400%; animation: gradientShift 20s ease infinite; }
@keyframes gradientShift { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }
.decoration { position: absolute; border-radius: 50%; filter: blur(80px); animation: float 10s ease-in-out infinite; }
.decoration-1 { width: 500px; height: 500px; top: -150px; right: -100px; background: rgba(59, 130, 246, 0.15); }
.decoration-2 { width: 400px; height: 400px; bottom: 10%; left: -100px; background: rgba(139, 92, 246, 0.12); animation-delay: 3s; }
.decoration-3 { width: 300px; height: 300px; top: 40%; right: 15%; background: rgba(16, 185, 129, 0.1); animation-delay: 6s; }
@keyframes float { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(40px, -40px) scale(1.1); } 66% { transform: translate(-30px, 30px) scale(0.9); } }
.home-header { position: relative; z-index: 100; padding: 20px 0; }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 32px; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; }
.logo-icon { width: 40px; height: 40px; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; }
.logo-text { font-size: 20px; font-weight: 700; color: white; }
.nav-actions { display: flex; gap: 12px; }
.nav-btn { padding: 8px 20px; border-radius: 8px; font-weight: 500; transition: all var(--transition-normal) ease; background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.2); color: white; }
.nav-btn:hover { background: rgba(255, 255, 255, 0.15); transform: translateY(-2px); }
.nav-btn-primary { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; }
.nav-btn-primary:hover { box-shadow: 0 8px 20px -5px rgba(59, 130, 246, 0.4); }
.home-main { flex: 1; display: flex; flex-direction: column; justify-content: center; position: relative; z-index: 10; padding: 60px 32px; }
.hero-section { text-align: center; max-width: 800px; margin: 0 auto; }
.hero-title { font-size: 56px; font-weight: 800; line-height: 1.2; margin: 0 0 24px; }
.title-line { display: block; color: white; }
.title-gradient { display: block; background: linear-gradient(135deg, var(--primary-light) 0%, #a78bfa 50%, #34d399 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.hero-subtitle { font-size: 18px; color: rgba(255, 255, 255, 0.7); margin: 0 0 40px; line-height: 1.6; }
.hero-actions { display: flex; gap: 16px; justify-content: center; }
.cta-btn { padding: 14px 32px; font-size: 16px; font-weight: 600; border-radius: 12px; transition: all var(--transition-normal) ease; }
.cta-btn-primary { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; color: white; display: flex; align-items: center; gap: 8px; }
.cta-btn-primary:hover { transform: translateY(-3px); box-shadow: 0 15px 30px -10px rgba(59, 130, 246, 0.5); }
.cta-btn-secondary { background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.3); color: white; }
.cta-btn-secondary:hover { background: rgba(255, 255, 255, 0.15); transform: translateY(-3px); }
.features-section { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; max-width: 1000px; margin: 80px auto 0; }
.feature-card { background: rgba(255, 255, 255, 0.05); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 32px; text-align: center; transition: all var(--transition-normal) ease; }
.feature-card:hover { transform: translateY(-8px); background: rgba(255, 255, 255, 0.08); border-color: rgba(59, 130, 246, 0.3); box-shadow: 0 20px 40px -15px rgba(59, 130, 246, 0.3); }
.feature-icon { width: 64px; height: 64px; border-radius: 16px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; color: white; }
.feature-title { font-size: 18px; font-weight: 600; color: white; margin: 0 0 12px; }
.feature-desc { font-size: 14px; color: rgba(255, 255, 255, 0.6); line-height: 1.6; margin: 0; }
.home-footer { position: relative; z-index: 10; padding: 24px 32px; text-align: center; border-top: 1px solid rgba(255, 255, 255, 0.1); }
.home-footer p { font-size: 13px; color: rgba(255, 255, 255, 0.5); margin: 0; }
@media (max-width: 768px) { .hero-title { font-size: 36px; } .hero-subtitle { font-size: 16px; } .features-section { grid-template-columns: 1fr; gap: 16px; } .hero-actions { flex-direction: column; align-items: center; } .cta-btn { width: 100%; max-width: 280px; } }
</style>
