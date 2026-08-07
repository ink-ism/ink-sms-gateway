<template>
  <div class="profile-page">
    <!-- 用户信息卡 -->
    <div class="profile-header-card panel">
      <div class="profile-banner"></div>
      <div class="profile-info">
        <div class="avatar">
          <el-icon :size="34"><User /></el-icon>
        </div>
        <div class="user-details">
          <h2 class="user-name">{{ profileForm.nickname || profileForm.username }}</h2>
          <p class="user-email mono">{{ profileForm.email }}</p>
        </div>
        <div class="user-chip">
          <span class="pulse-dot dot-online"></span>
          已认证
        </div>
      </div>
    </div>

    <!-- 设置区 -->
    <div class="profile-content panel">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="基本信息" name="info">
          <div class="tab-content">
            <el-form ref="profileFormRef" :model="profileForm" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" disabled>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称">
                  <template #prefix><el-icon><UserFilled /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号">
                  <template #prefix><el-icon><Phone /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" @click="handleUpdate">
                  <el-icon><Check /></el-icon>
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password">
          <div class="tab-content">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码">
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码">
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码">
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-button>
              </el-form-item>
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
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 720px;
  margin: 0 auto;
}

/* 用户信息卡 */
.profile-header-card {
  overflow: hidden;
  position: relative;
}

.profile-banner {
  height: 110px;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(34, 211, 238, 0.25) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 100%, rgba(167, 139, 250, 0.18) 0%, transparent 60%),
    var(--bg-elevated);
  border-bottom: 1px solid var(--border-light);
}

.profile-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 32px 24px;
  margin-top: -36px;
  position: relative;
  z-index: 1;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: var(--bg-elevated);
  border: 2px solid var(--accent);
  box-shadow: var(--glow-accent);
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
  padding-top: 40px;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.user-email {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--success);
  background: var(--success-soft);
  border: 1px solid rgba(0, 255, 163, 0.2);
  padding: 5px 12px;
  border-radius: 20px;
  align-self: flex-end;
  margin-bottom: 6px;
  white-space: nowrap;
}

/* 设置区 */
.profile-content {
  overflow: hidden;
}

.profile-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 24px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-light);
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.profile-tabs :deep(.el-tabs__item) {
  height: 52px;
  line-height: 52px;
  font-size: 14px;
  font-weight: 500;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background: var(--accent);
  border-radius: 3px 3px 0 0;
  box-shadow: var(--glow-accent);
}

.tab-content {
  padding: 32px;
}

.profile-form {
  max-width: 480px;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-info {
    flex-direction: column;
    text-align: center;
    padding: 0 24px 24px;
  }

  .user-details {
    padding-top: 12px;
  }

  .user-chip {
    align-self: center;
    margin-bottom: 0;
  }

  .tab-content {
    padding: 24px;
  }
}
</style>
