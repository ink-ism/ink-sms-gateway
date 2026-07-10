<template>
  <AppLayout>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-tag>共 {{ total }} 个用户</el-tag>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="danger" size="small"
              @click="handleToggleStatus(row, 0)"
            >禁用</el-button>
            <el-button
              v-else
              type="success" size="small"
              @click="handleToggleStatus(row, 1)"
            >启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadUsers"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { User } from '../types'
import { getUserList, getUserCount, disableUser, enableUser } from '../api/user'
import AppLayout from '../components/AppLayout.vue'

const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList(currentPage.value, pageSize.value)
    users.value = res.data || []
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const loadTotal = async () => {
  try {
    const res = await getUserCount()
    total.value = res.data || 0
  } catch (error) {
    console.error('获取用户总数失败:', error)
  }
}

onMounted(() => {
  loadUsers()
  loadTotal()
})

const handleToggleStatus = async (user: User, status: number) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '提示', {
      type: 'warning'
    })
    if (status === 1) {
      await enableUser(user.id)
    } else {
      await disableUser(user.id)
    }
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
