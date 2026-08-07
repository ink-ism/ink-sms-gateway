<template>
  <div class="register-page">
    <!-- 氛围装饰 -->
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>

    <div class="auth-container">
      <!-- 左侧品牌展示 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-icon">
            <el-icon :size="30"><ChatDotRound /></el-icon>
          </div>
          <h1 class="brand-title">INK<span class="brand-accent">OPS</span></h1>
          <p class="brand-subtitle">创建您的管理员账号</p>
          <div class="brand-features">
            <div class="feature-item"><el-icon><UserFilled /></el-icon><span>多角色管理</span></div>
            <div class="feature-item"><el-icon><Document /></el-icon><span>操作日志审计</span></div>
            <div class="feature-item"><el-icon><Lock /></el-icon><span>细粒度权限控制</span></div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2>用户注册</h2>
            <p>填写以下信息完成注册</p>
          </div>
          <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="0" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password>
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" show-password>
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleRegister" class="submit-btn">
                注册
              </el-button>
            </el-form-item>
            <div class="form-footer">
              <span>已有账号？</span>
              <el-link type="primary" @click="goToLogin" :underline="false">立即登录</el-link>
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
import { User, Lock, ChatDotRound, UserFilled, Document, Message, Phone } from '@element-plus/icons-vue'
import { register } from '../api/user'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerForm = reactive({ username: '', email: '', phone: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) { callback(new Error('两次输入的密码不一致')) } else { callback() }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { len: 11, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  try {
    loading.value = true
    await registerFormRef.value.validate()
    await register({ username: registerForm.username, password: registerForm.password, email: registerForm.email, phone: registerForm.phone })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) { if (error.message) ElMessage.error(error.message) }
  finally { loading.value = false }
}
const goToLogin = () => { router.push('/login') }
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
  z-index: 1;
}

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
  right: -80px;
  background: rgba(34, 211, 238, 0.1);
}

.decoration-2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -60px;
  background: rgba(167, 139, 250, 0.09);
  animation-delay: 3s;
}

.auth-container {
  display: flex;
  width: 900px;
  max-width: 100%;
  min-height: 620px;
  background: rgba(13, 20, 36, 0.72);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lift);
  position: relative;
  z-index: 10;
  overflow: hidden;
}

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

.form-section {
  flex: 1;
  padding: 40px 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 24px;
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

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  border-radius: var(--radius-md);
}

.form-footer {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.form-footer span {
  margin-right: 6px;
}

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
    padding: 24px;
  }
}
</style>
