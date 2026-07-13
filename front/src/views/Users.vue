<template>
  <div class="users-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-left">
          <h2>用户管理</h2>
          <el-tag class="count-badge" effect="dark" round>共 {{ total }} 人</el-tag>
        </div>
        <div class="header-right">
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
        </div>
      </div>

      <!-- 用户表格 -->
      <div class="table-card">
        <el-table
          :data="users"
          v-loading="loading"
          class="custom-table"
          :header-cell-style="headerCellStyle"
          :row-class-name="tableRowClassName"
        >
          <el-table-column prop="id" label="ID" width="70" align="center" />
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
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="createTime" label="注册时间" width="170" />
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
                  @click="handleToggleStatus(row, 0)"
                >
                  <el-icon><CircleClose /></el-icon>
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  circle
                  @click="handleToggleStatus(row, 1)"
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
                  <el-icon :size="64" color="#d1d5db"><User /></el-icon>
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
import type { User as UserType } from '../types'
import { getUserList, getUserCount, disableUser, enableUser } from '../api/user'
const loading = ref(false)
const users = ref<UserType[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const headerCellStyle = {
  background: '#f8fafc',
  color: '#374151',
  fontWeight: '600',
  fontSize: '13px'
}

const tableRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  return rowIndex % 2 === 0 ? '' : 'stripe-row'
}

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

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  padding: 20px 24px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.count-badge {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border: none;
  font-weight: 500;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--border-color);
  transition: all var(--transition-normal) ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary-light);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-color);
}

/* 表格卡片 */
.table-card {
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* 自定义表格 */
.custom-table {
  --el-table-border-color: #f1f5f9;
}

.custom-table :deep(.el-table__header) {
  border-bottom: 1px solid #e2e8f0;
}

.custom-table :deep(.el-table__row) {
  transition: all var(--transition-normal) ease;
}

.custom-table :deep(.el-table__row:hover > td) {
  background: #f8fafc !important;
}

.custom-table :deep(.stripe-row td) {
  background: #fafbfc;
}

.username-cell {
  font-weight: 500;
  color: var(--text-primary);
}

.email-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

/* 空状态 */
.empty-state {
  padding: 40px 0;
}

/* 分页 */
.pagination-wrapper {
  padding: 20px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f1f5f9;
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-button-bg-color: #f8fafc;
}

.pagination-wrapper :deep(.el-pager li) {
  border-radius: 6px;
  font-weight: 500;
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  color: white;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-right {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
