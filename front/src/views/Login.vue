<template>
  <div class="login-page">
    <!-- 氛围装饰 -->
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>

    <!-- 主容器 -->
    <div class="auth-container">
      <!-- 左侧品牌展示 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-icon">
            <el-icon :size="30"><ChatDotRound /></el-icon>
          </div>
          <h1 class="brand-title">INK<span class="brand-accent">OPS</span></h1>
          <p class="brand-subtitle">企业级短信网关运维管理平台</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><Connection /></el-icon>
              <span>多通道 CMPP 接入</span>
            </div>
            <div class="feature-item">
              <el-icon><DataLine /></el-icon>
              <span>连接池实时监控</span>
            </div>
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>高可用可靠投递</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2>用户登录</h2>
            <p>欢迎回来，请登录您的账号</p>
          </div>

          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password" type="password" placeholder="请输入密码"
                show-password @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin" class="submit-btn">
                登录
              </el-button>
            </el-form-item>

            <div class="form-footer">
              <span>还没有账号？</span>
              <el-link type="primary" @click="goToRegister" :underline="false">立即注册</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, ChatDotRound, Connection, DataLine, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    loading.value = true
    await loginFormRef.value.validate()
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
  z-index: 1;
}

/* 氛围光斑装饰 */
.decoration {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: float 9s ease-in-out infinite;
  pointer-events: none;
}

.decoration-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -80px;
  background: rgba(34, 211, 238, 0.1);
}

.decoration-2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  right: -60px;
  background: rgba(167, 139, 250, 0.09);
  animation-delay: 3s;
}

/* 主容器：玻璃拟态 */
.auth-container {
  display: flex;
  width: 900px;
  max-width: 100%;
  min-height: 520px;
  background: rgba(13, 20, 36, 0.72);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lift);
  position: relative;
  z-index: 10;
  overflow: hidden;
}

/* 品牌区 */
.brand-section {
  flex: 1;
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--border-light);
  background:
    linear-gradient(rgba(34, 211, 238, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(34, 211, 238, 0.03) 1px, transparent 1px);
  background-size: 28px 28px;
}

.brand-content {
  text-align: center;
}

.logo-icon {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 22px;
  color: #04121a;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-strong) 100%);
  box-shadow: var(--glow-accent);
  animation: float 5s ease-in-out infinite;
}

.brand-title {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.brand-accent {
  color: var(--accent);
  text-shadow: 0 0 16px rgba(34, 211, 238, 0.7);
}

.brand-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 18px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all var(--transition-fast) ease;
}

.feature-item:hover {
  border-color: var(--border-glow);
  color: var(--text-primary);
}

.feature-item .el-icon {
  color: var(--accent);
  font-size: 17px;
}

/* 表单区 */
.form-section {
  flex: 1;
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.form-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  border-radius: var(--radius-md);
}

/* 表单底部 */
.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-muted);
}

.form-footer span {
  margin-right: 6px;
}

/* 响应式 */
@media (max-width: 768px) {
  .auth-container {
    flex-direction: column;
    min-height: auto;
  }

  .brand-section {
    padding: 32px 24px;
    border-right: none;
    border-bottom: 1px solid var(--border-light);
  }

  .brand-features {
    display: none;
  }

  .form-section {
    padding: 32px 24px;
  }
}
</style>
