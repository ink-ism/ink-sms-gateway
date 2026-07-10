<template>
  <AppLayout>
    <div class="profile-content">
      <el-card class="profile-card">
        <template #header>
          <span>基本信息</span>
        </template>

        <el-form ref="profileFormRef" :model="profileForm" label-width="80px" size="large">
          <el-form-item label="用户名">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleUpdate">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="password-card">
        <template #header>
          <span>修改密码</span>
        </template>

        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px" size="large">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getUserInfo, updateUser, updatePassword } from '../api/user'
import { useUserStore } from '../stores/user'
import AppLayout from '../components/AppLayout.vue'

const router = useRouter()
const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({ username: '', nickname: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    const user = res.data
    profileForm.username = user.username
    profileForm.nickname = user.nickname || ''
    profileForm.email = user.email
    profileForm.phone = user.phone
  } catch {
    ElMessage.error('获取用户信息失败')
  }
}

onMounted(() => { loadUserInfo() })

const handleUpdate = async () => {
  loading.value = true
  try {
    await updateUser({
      username: profileForm.username,
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      avatar: ''
    })
    ElMessage.success('更新成功')
    await userStore.fetchUserInfo()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearAuth()
    router.push('/login')
  } catch (error: any) {
    if (error.message) ElMessage.error(error.message)
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-content {
  max-width: 600px;
  margin: 0 auto;
}
.profile-card, .password-card {
  margin-bottom: 20px;
}
</style>
