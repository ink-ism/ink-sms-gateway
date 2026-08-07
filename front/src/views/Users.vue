<template>
  <div class="users-page">
    <!-- 页面头部 -->
    <PageHeader title="用户管理" :badge="`共 ${total} 人`">
      <template #actions>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/昵称/邮箱"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="handleSearch">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </PageHeader>

    <!-- 用户表格 -->
    <div class="table-card panel">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" align="center">
          <template #default="{ row }">
            <span class="mono dim-cell">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="130">
          <template #default="{ row }">
            <span class="username-cell">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            <span class="email-cell">{{ row.email }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170">
          <template #default="{ row }">
            <span class="mono dim-cell">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              effect="light"
              round
              size="small"
            >
              <el-icon v-if="row.status === 1" style="margin-right: 4px;"><CircleCheck /></el-icon>
              <el-icon v-else style="margin-right: 4px;"><CircleClose /></el-icon>
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :content="row.status === 1 ? '禁用用户' : '启用用户'"
              placement="top"
            >
              <el-button
                v-if="row.status === 1"
                type="danger"
                size="small"
                circle
                @click="handleToggleStatus(row as UserType, 0)"
              >
                <el-icon><CircleClose /></el-icon>
              </el-button>
              <el-button
                v-else
                type="success"
                size="small"
                circle
                @click="handleToggleStatus(row as UserType, 1)"
              >
                <el-icon><CircleCheck /></el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- 空状态 -->
        <template #empty>
          <div class="empty-state">
            <el-empty description="暂无数据">
              <template #image>
                <el-icon :size="64" color="#2a3a5c"><User /></el-icon>
              </template>
            </el-empty>
          </div>
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
          background
          small
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, User, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import type { User as UserType } from '../types'
import { getUserList, getUserCount, disableUser, enableUser } from '../api/user'

const loading = ref(false)
const users = ref<UserType[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

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

const handleSearch = () => {
  loadUsers()
  loadTotal()
}

onMounted(() => {
  loadUsers()
  loadTotal()
})

const handleToggleStatus = async (user: UserType, status: number) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (status === 1) {
      await enableUser(user.id)
    } else {
      await disableUser(user.id)
    }
    ElMessage.success(`${action}成功`)
    loadUsers()
    loadTotal()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}
</script>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input {
  width: 260px;
}

/* 表格卡片 */
.table-card {
  overflow: hidden;
}

.username-cell {
  font-weight: 500;
  color: var(--text-primary);
}

.email-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.dim-cell {
  color: var(--text-secondary);
  font-size: 12px;
}

/* 空状态 */
.empty-state {
  padding: 40px 0;
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* 响应式 */
@media (max-width: 768px) {
  .search-input {
    width: 100%;
  }
}
</style>
