<template>
  <div class="login-page">
    <!-- 背景动态渐变 -->
    <div class="bg-gradient"></div>
    
    <!-- 装饰元素 -->
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>
    <div class="decoration decoration-3"></div>

    <!-- 主容器 -->
    <div class="auth-container">
      <!-- 左侧品牌展示 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo">
            <div class="logo-icon">
              <el-icon :size="32"><ChatDotRound /></el-icon>
            </div>
          </div>
          <h1 class="brand-title">INK短信网关</h1>
          <p class="brand-subtitle">企业级短信网关管理平台</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><Connection /></el-icon>
              <span>多通道支持</span>
            </div>
            <div class="feature-item">
              <el-icon><DataLine /></el-icon>
              <span>实时监控</span>
            </div>
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>安全可靠</span>
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
              <el-input v-model="loginForm.username" placeholder="请输入用户名" class="custom-input">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password" type="password" placeholder="请输入密码"
                show-password class="custom-input" @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary" 
                :loading="loading" 
                @click="handleLogin" 
                class="submit-btn"
              >
                <span v-if="!loading">登录</span>
                <span v-else class="loading-text">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  登录中...
                </span>
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
import { User, Lock, ChatDotRound, Connection, DataLine, CircleCheck, Loading } from '@element-plus/icons-vue'
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
}

/* 动态渐变背景 */
.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(-45deg, #1e3a5f, #2d5a87, #1e293b, #3b4f6a);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
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
  background: rgba(59, 130, 246, 0.1);
  filter: blur(60px);
  animation: float 8s ease-in-out infinite;
}

.decoration-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.decoration-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  right: -50px;
  background: rgba(99, 102, 241, 0.15);
  animation-delay: 2s;
}

.decoration-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 20%;
  background: rgba(139, 92, 246, 0.1);
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.95);
  }
}

/* 主容器 */
.auth-container {
  display: flex;
  width: 900px;
  max-width: 100%;
  min-height: 520px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
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
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(99, 102, 241, 0.1) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.brand-content {
  text-align: center;
  color: white;
}

.brand-logo {
  margin-bottom: 24px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  box-shadow: 0 10px 30px -10px rgba(59, 130, 246, 0.5);
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.feature-item .el-icon {
  color: var(--primary-light);
  font-size: 18px;
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
  color: white;
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 自定义输入框 */
.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none;
  transition: all var(--transition-normal) ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.2);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.custom-input :deep(.el-input__inner) {
  color: white;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.custom-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.5);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border: none;
  border-radius: 12px;
  transition: all var(--transition-normal) ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(59, 130, 246, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 表单底部 */
.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.form-footer span {
  margin-right: 8px;
}

.form-footer :deep(.el-link) {
  color: var(--primary-light);
  font-weight: 500;
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
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
  
  .brand-features {
    display: none;
  }
  
  .form-section {
    padding: 32px 24px;
  }
}
</style>
