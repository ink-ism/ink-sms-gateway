<template>
  <div class="blacklist-page">
    <div class="page-header">
      <div class="header-left">
        <h2>黑名单管理</h2>
        <el-tag class="count-badge" effect="dark" round>共 {{ total }} 条</el-tag>
      </div>
    </div>
    <div class="search-card">
      <el-select v-model="channelFilter" placeholder="按通道筛选" clearable class="channel-select" @change="handleSearch">
        <el-option v-for="ch in channelOptions" :key="ch.code" :label="`${ch.name}（${ch.code}）`" :value="ch.code" />
      </el-select>
      <el-input v-model="phoneFilter" placeholder="搜索手机号" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
      <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
    </div>
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" class="custom-table" :header-cell-style="headerCellStyle">
        <el-table-column prop="channelCode" label="通道编码" width="130">
          <template #default="{ row }"><span class="code-cell">{{ row.channelCode }}</span></template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }"><span class="phone-cell">{{ row.phone }}</span></template>
        </el-table-column>
        <el-table-column prop="keyword" label="触发关键字" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.keyword" size="small" type="danger" effect="plain">{{ row.keyword }}</el-tag>
            <span v-else class="report-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceMoId" label="上行消息ID" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="msg-id-cell">{{ row.sourceMoId || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="过期状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="isExpired(row.expireTime) ? 'info' : 'danger'" round>{{ isExpired(row.expireTime) ? '已过期' : '生效中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" background small />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
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

const headerCellStyle = { background: '#f8fafc', color: '#374151', fontWeight: '600', fontSize: '13px' }

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
.blacklist-page { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; background: var(--bg-card); padding: 20px 24px; border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-left h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.count-badge { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; font-weight: 500; }
.search-card { background: var(--bg-card); padding: 20px 24px; border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }
.channel-select { width: 220px; }
.search-input :deep(.el-input__wrapper), .channel-select :deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px var(--border-color); transition: all var(--transition-normal) ease; }
.search-input :deep(.el-input__wrapper:hover), .channel-select :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--primary-light); }
.search-input :deep(.el-input__wrapper.is-focus), .channel-select :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary-color); }
.table-card { background: var(--bg-card); border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.custom-table { --el-table-border-color: #f1f5f9; }
.custom-table :deep(.el-table__header) { border-bottom: 1px solid #e2e8f0; }
.custom-table :deep(.el-table__row) { transition: all var(--transition-normal) ease; }
.custom-table :deep(.el-table__row:hover > td) { background: #f8fafc !important; }
.msg-id-cell { font-family: 'SF Mono', Monaco, monospace; font-size: 12px; color: #64748b; }
.phone-cell { font-weight: 500; color: var(--text-primary); }
.code-cell { font-family: 'SF Mono', Monaco, monospace; font-size: 12px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #475569; }
.report-empty { color: #94a3b8; }
.pagination-wrapper { padding: 20px 24px; display: flex; justify-content: flex-end; border-top: 1px solid #f1f5f9; }
.pagination-wrapper :deep(.el-pager li) { border-radius: 6px; font-weight: 500; }
.pagination-wrapper :deep(.el-pager li.is-active) { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); color: white; }
@media (max-width: 768px) { .search-card { flex-direction: column; align-items: stretch; } .search-input, .channel-select { width: 100%; } }
</style>
