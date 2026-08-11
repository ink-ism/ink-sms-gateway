<template>
  <div class="blacklist-page">
    <!-- 页面头部 -->
    <PageHeader title="黑名单管理" :badge="`共 ${total} 条`">
      <template #actions>
        <el-select
          v-model="channelFilter"
          placeholder="按通道筛选"
          clearable
          class="channel-select"
          @change="handleSearch"
        >
          <el-option v-for="ch in channelOptions" :key="ch.code" :label="`${ch.name}（${ch.code}）`" :value="ch.code" />
        </el-select>
        <el-input
          v-model="phoneFilter"
          placeholder="搜索手机号"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </template>
    </PageHeader>

    <!-- 黑名单表格 -->
    <div class="table-card panel">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="channelCode" label="通道编码" width="130">
          <template #default="{ row }"><span class="code-chip">{{ row.channelCode }}</span></template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }"><span class="mono phone-cell">{{ row.phone }}</span></template>
        </el-table-column>
        <el-table-column prop="keyword" label="触发关键字" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.keyword" size="small" type="danger" effect="plain">{{ row.keyword }}</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceMoId" label="上行消息ID" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono dim-cell">{{ row.sourceMoId || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="过期状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="isExpired(row.expireTime) ? 'info' : 'danger'" round>{{ isExpired(row.expireTime) ? '已过期' : '生效中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="170">
          <template #default="{ row }"><span class="mono dim-cell">{{ formatDateTime(row.expireTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }"><span class="mono dim-cell">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRemove(row as Blacklist)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
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
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import { getBlacklistList, removeBlacklist } from '../api/blacklist'
import { getChannelList } from '../api/channel'
import type { Blacklist, Channel } from '../types'

const channelFilter = ref('')
const phoneFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<Blacklist[]>([])
const channelOptions = ref<Channel[]>([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getBlacklistList(currentPage.value, pageSize.value, channelFilter.value || undefined, phoneFilter.value || undefined)
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

const fetchChannels = async () => {
  try {
    const res = await getChannelList(1, 100)
    if (res.code === 200) { channelOptions.value = res.data.list }
  } catch { /* 通道加载失败不阻塞页面 */ }
}

const handleSearch = () => { currentPage.value = 1; fetchData() }
const handleReset = () => { channelFilter.value = ''; phoneFilter.value = ''; currentPage.value = 1; fetchData() }

const isExpired = (expireTime: string) => !!expireTime && new Date(expireTime).getTime() < Date.now()

const handleRemove = (row: Blacklist) => {
  ElMessageBox.confirm(`确定移除黑名单 [${row.channelCode} / ${row.phone}] 吗？移除后该号码可在此通道重新接收短信。`, '移除确认', { type: 'warning' })
    .then(async () => {
      const res = await removeBlacklist(row.id)
      if (res.code === 200) { ElMessage.success(res.data); fetchData() } else { ElMessage.error(res.message) }
    })
    .catch(() => {})
}

onMounted(() => { fetchData(); fetchChannels() })
</script>

<style scoped>
.blacklist-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input {
  width: 220px;
}

.channel-select {
  width: 220px;
}

/* 表格卡片 */
.table-card {
  overflow: hidden;
}

.phone-cell {
  font-weight: 500;
  color: var(--text-primary);
}

.dim-cell {
  color: var(--text-secondary);
  font-size: 12px;
}

.cell-empty {
  color: var(--text-muted);
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
  .search-input,
  .channel-select {
    width: 100%;
  }
}
</style>
