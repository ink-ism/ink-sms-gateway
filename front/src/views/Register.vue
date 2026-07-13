<template>
  <div class="register-page">
    <div class="bg-gradient"></div>
    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>
    <div class="decoration decoration-3"></div>
    <div class="auth-container">
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo"><div class="logo-icon"><el-icon :size="32"><ChatDotRound /></el-icon></div></div>
          <h1 class="brand-title">INK短信网关</h1>
          <p class="brand-subtitle">创建您的管理员账号</p>
          <div class="brand-features">
            <div class="feature-item"><el-icon><UserFilled /></el-icon><span>多角色管理</span></div>
            <div class="feature-item"><el-icon><Document /></el-icon><span>操作日志</span></div>
            <div class="feature-item"><el-icon><Lock /></el-icon><span>权限控制</span></div>
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-card">
          <div class="form-header"><h2>用户注册</h2><p>填写以下信息完成注册</p></div>
          <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="0" size="large">
            <el-form-item prop="username"><el-input v-model="registerForm.username" placeholder="请输入用户名" class="custom-input"><template #prefix><el-icon><User /></el-icon></template></el-input></el-form-item>
            <el-form-item prop="email"><el-input v-model="registerForm.email" placeholder="请输入邮箱" class="custom-input"><template #prefix><el-icon><Message /></el-icon></template></el-input></el-form-item>
            <el-form-item prop="phone"><el-input v-model="registerForm.phone" placeholder="请输入手机号" class="custom-input"><template #prefix><el-icon><Phone /></el-icon></template></el-input></el-form-item>
            <el-form-item prop="password"><el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password class="custom-input"><template #prefix><el-icon><Lock /></el-icon></template></el-input></el-form-item>
            <el-form-item prop="confirmPassword"><el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" show-password class="custom-input"><template #prefix><el-icon><Lock /></el-icon></template></el-input></el-form-item>
            <el-form-item><el-button type="primary" :loading="loading" @click="handleRegister" class="submit-btn"><span v-if="!loading">注册</span><span v-else class="loading-text"><el-icon class="is-loading"><Loading /></el-icon>注册中...</span></el-button></el-form-item>
            <div class="form-footer"><span>已有账号？</span><el-link type="primary" @click="goToLogin" :underline="false">立即登录</el-link></div>
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
import { User, Lock, ChatDotRound, UserFilled, Document, Message, Phone, Loading } from '@element-plus/icons-vue'
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
.register-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; padding: 20px; }
.bg-gradient { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(-45deg, #1e3a5f, #2d5a87, #1e293b, #3b4f6a); background-size: 400% 400%; animation: gradientShift 15s ease infinite; }
@keyframes gradientShift { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }
.decoration { position: absolute; border-radius: 50%; background: rgba(59, 130, 246, 0.1); filter: blur(60px); animation: float 8s ease-in-out infinite; }
.decoration-1 { width: 400px; height: 400px; top: -100px; left: -100px; }
.decoration-2 { width: 300px; height: 300px; bottom: -50px; right: -50px; background: rgba(99, 102, 241, 0.15); animation-delay: 2s; }
.decoration-3 { width: 200px; height: 200px; top: 50%; right: 20%; background: rgba(139, 92, 246, 0.1); animation-delay: 4s; }
@keyframes float { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(30px, -30px) scale(1.05); } 66% { transform: translate(-20px, 20px) scale(0.95); } }
.auth-container { display: flex; width: 900px; max-width: 100%; min-height: 580px; background: rgba(255, 255, 255, 0.05); backdrop-filter: blur(20px); border-radius: 24px; border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); position: relative; z-index: 10; overflow: hidden; }
.brand-section { flex: 1; padding: 48px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(99, 102, 241, 0.1) 100%); border-right: 1px solid rgba(255, 255, 255, 0.1); }
.brand-content { text-align: center; color: white; }
.brand-logo { margin-bottom: 24px; }
.logo-icon { width: 72px; height: 72px; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto; box-shadow: 0 10px 30px -10px rgba(59, 130, 246, 0.5); }
.brand-title { font-size: 28px; font-weight: 700; margin: 0 0 8px; background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.brand-subtitle { font-size: 14px; color: rgba(255, 255, 255, 0.7); margin: 0 0 32px; }
.brand-features { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: rgba(255, 255, 255, 0.05); border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.1); font-size: 14px; color: rgba(255, 255, 255, 0.9); }
.feature-item .el-icon { color: var(--primary-light); font-size: 18px; }
.form-section { flex: 1; padding: 48px; display: flex; align-items: center; justify-content: center; }
.form-card { width: 100%; max-width: 360px; }
.form-header { margin-bottom: 32px; }
.form-header h2 { font-size: 24px; font-weight: 600; color: white; margin: 0 0 8px; }
.form-header p { font-size: 14px; color: rgba(255, 255, 255, 0.6); margin: 0; }
.custom-input :deep(.el-input__wrapper) { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: none; transition: all var(--transition-normal) ease; }
.custom-input :deep(.el-input__wrapper:hover) { border-color: rgba(255, 255, 255, 0.2); }
.custom-input :deep(.el-input__wrapper.is-focus) { border-color: var(--primary-color); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2); }
.custom-input :deep(.el-input__inner) { color: white; }
.custom-input :deep(.el-input__inner::placeholder) { color: rgba(255, 255, 255, 0.4); }
.custom-input :deep(.el-input__prefix) { color: rgba(255, 255, 255, 0.5); }
.submit-btn { width: 100%; height: 48px; font-size: 16px; font-weight: 500; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; border-radius: 12px; transition: all var(--transition-normal) ease; }
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 10px 20px -5px rgba(59, 130, 246, 0.4); }
.loading-text { display: flex; align-items: center; gap: 8px; }
.form-footer { text-align: center; margin-top: 24px; font-size: 14px; color: rgba(255, 255, 255, 0.6); }
.form-footer span { margin-right: 8px; }
.form-footer :deep(.el-link) { color: var(--primary-light); font-weight: 500; }
@media (max-width: 768px) { .auth-container { flex-direction: column; min-height: auto; } .brand-section { padding: 32px 24px; border-right: none; border-bottom: 1px solid rgba(255, 255, 255, 0.1); } .brand-features { display: none; } .form-section { padding: 32px 24px; } }
</style>
