<template>
  <div class="profile-page">
      <div class="profile-header-card">
        <div class="profile-bg"></div>
        <div class="profile-info">
          <div class="avatar-wrapper"><div class="avatar"><el-icon :size="36"><User /></el-icon></div></div>
          <div class="user-details">
            <h2 class="user-name">{{ profileForm.nickname || profileForm.username }}</h2>
            <p class="user-email">{{ profileForm.email }}</p>
          </div>
        </div>
      </div>
      <div class="profile-content">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基本信息" name="info">
            <div class="tab-content">
              <el-form ref="profileFormRef" :model="profileForm" label-width="100px" class="profile-form">
                <el-form-item label="用户名"><el-input v-model="profileForm.username" disabled><template #prefix><el-icon><User /></el-icon></template></el-input></el-form-item>
                <el-form-item label="昵称"><el-input v-model="profileForm.nickname" placeholder="请输入昵称"><template #prefix><el-icon><UserFilled /></el-icon></template></el-input></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="profileForm.email" placeholder="请输入邮箱"><template #prefix><el-icon><Message /></el-icon></template></el-input></el-form-item>
                <el-form-item label="手机号"><el-input v-model="profileForm.phone" placeholder="请输入手机号"><template #prefix><el-icon><Phone /></el-icon></template></el-input></el-form-item>
                <el-form-item><el-button type="primary" :loading="loading" @click="handleUpdate" class="submit-btn"><el-icon><Check /></el-icon>保存修改</el-button></el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
          <el-tab-pane label="修改密码" name="password">
            <div class="tab-content">
              <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
                <el-form-item label="原密码" prop="oldPassword"><el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码"><template #prefix><el-icon><Lock /></el-icon></template></el-input></el-form-item>
                <el-form-item label="新密码" prop="newPassword"><el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码"><template #prefix><el-icon><Lock /></el-icon></template></el-input></el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码"><template #prefix><el-icon><Lock /></el-icon></template></el-input></el-form-item>
                <el-form-item><el-button type="primary" :loading="passwordLoading" @click="handleChangePassword" class="submit-btn"><el-icon><Lock /></el-icon>修改密码</el-button></el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, UserFilled, Message, Phone, Lock, Check } from '@element-plus/icons-vue'
import { getUserInfo, updateUser, updatePassword } from '../api/user'
import { useUserStore } from '../stores/user'
const router = useRouter()
const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)
const passwordLoading = ref(false)
const activeTab = ref('info')
const profileForm = reactive({ username: '', nickname: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) { callback(new Error('两次输入的密码不一致')) } else { callback() }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    const user = res.data
    profileForm.username = user.username
    profileForm.nickname = user.nickname || ''
    profileForm.email = user.email
    profileForm.phone = user.phone
  } catch { ElMessage.error('获取用户信息失败') }
}

onMounted(() => { loadUserInfo() })

const handleUpdate = async () => {
  loading.value = true
  try {
    await updateUser({ username: profileForm.username, nickname: profileForm.nickname, email: profileForm.email, phone: profileForm.phone, avatar: '' })
    ElMessage.success('更新成功')
    await userStore.fetchUserInfo()
  } catch (error: any) { ElMessage.error(error.message || '更新失败') }
  finally { loading.value = false }
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearAuth()
    router.push('/login')
  } catch (error: any) { if (error.message) ElMessage.error(error.message) }
  finally { passwordLoading.value = false }
}
</script>

<style scoped>
.profile-page { display: flex; flex-direction: column; gap: 24px; max-width: 700px; margin: 0 auto; }
.profile-header-card { background: var(--bg-card); border-radius: var(--border-radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); position: relative; }
.profile-bg { height: 120px; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 50%, #8b5cf6 100%); position: relative; }
.profile-bg::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 40px; background: linear-gradient(to top, var(--bg-card), transparent); }
.profile-info { display: flex; align-items: center; gap: 20px; padding: 0 32px 24px; margin-top: -40px; position: relative; z-index: 1; }
.avatar { width: 80px; height: 80px; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; border: 4px solid var(--bg-card); box-shadow: var(--shadow-md); }
.user-details { flex: 1; padding-top: 48px; }
.user-name { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0 0 4px; }
.user-email { font-size: 14px; color: var(--text-secondary); margin: 0; }
.profile-content { background: var(--bg-card); border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.profile-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 24px; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.profile-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.profile-tabs :deep(.el-tabs__item) { height: 56px; line-height: 56px; font-size: 14px; font-weight: 500; color: var(--text-secondary); transition: all var(--transition-normal) ease; }
.profile-tabs :deep(.el-tabs__item:hover) { color: var(--primary-color); }
.profile-tabs :deep(.el-tabs__item.is-active) { color: var(--primary-color); }
.profile-tabs :deep(.el-tabs__active-bar) { height: 3px; background: linear-gradient(90deg, var(--primary-color) 0%, #6366f1 100%); border-radius: 3px 3px 0 0; }
.tab-content { padding: 32px; }
.profile-form :deep(.el-form-item) { margin-bottom: 20px; }
.profile-form :deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px var(--border-color); transition: all var(--transition-normal) ease; }
.profile-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--primary-light); }
.profile-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary-color); }
.submit-btn { padding: 10px 24px; font-weight: 500; background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; border-radius: 8px; transition: all var(--transition-normal) ease; }
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 16px -4px rgba(59, 130, 246, 0.4); }
@media (max-width: 768px) { .profile-info { flex-direction: column; text-align: center; padding: 0 24px 24px; } .user-details { padding-top: 12px; } .tab-content { padding: 24px; } }
</style>
