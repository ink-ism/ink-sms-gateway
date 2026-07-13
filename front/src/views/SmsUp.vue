<template>
  <div class="sms-up-page">
      <div class="page-header">
        <div class="header-left">
          <h2>上行短信</h2>
          <el-tag class="count-badge" effect="dark" round>共 {{ total }} 条</el-tag>
        </div>
      </div>
      <div class="search-card">
        <el-input v-model="keyword" placeholder="搜索手机号/目的号码/内容/通道" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="typeFilter" placeholder="类型筛选" clearable class="type-select" @change="handleSearch">
          <el-option label="全部" value="" />
          <el-option label="上行短信" value="0" />
          <el-option label="状态报告" value="1" />
        </el-select>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
        <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>
      <div class="table-card">
        <el-table :data="tableData" v-loading="loading" class="custom-table" :header-cell-style="headerCellStyle">
          <el-table-column prop="msgId" label="消息ID" width="150" show-overflow-tooltip>
            <template #default="{ row }"><span class="msg-id-cell">{{ row.msgId }}</span></template>
          </el-table-column>
          <el-table-column prop="srcTerminalId" label="源手机号" width="130">
            <template #default="{ row }"><span class="phone-cell">{{ row.srcTerminalId }}</span></template>
          </el-table-column>
          <el-table-column prop="destId" label="目的号码" width="120" />
          <el-table-column prop="msgContent" label="短信内容" min-width="220" show-overflow-tooltip>
            <template #default="{ row }"><span class="content-cell">{{ row.msgContent }}</span></template>
          </el-table-column>
          <el-table-column label="消息格式" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="msgFmtType(row.msgFmt)" effect="plain">{{ msgFmtText(row.msgFmt) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isReport === 1 ? 'danger' : 'success'" round>
                {{ row.isReport === 1 ? '状态报告' : '上行短信' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="channelCode" label="通道编码" width="110">
            <template #default="{ row }"><span class="code-cell">{{ row.channelCode }}</span></template>
          </el-table-column>
          <el-table-column prop="createTime" label="接收时间" width="170" />
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" background small />
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getSmsUpList } from '../api/sms'
import type { SmsUp } from '../types'

const keyword = ref('')
const typeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<SmsUp[]>([])
const loading = ref(false)

const headerCellStyle = { background: '#f8fafc', color: '#374151', fontWeight: '600', fontSize: '13px' }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSmsUpList(currentPage.value, pageSize.value, keyword.value || undefined)
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

const handleSearch = () => { currentPage.value = 1; fetchData() }
const handleReset = () => { keyword.value = ''; typeFilter.value = ''; currentPage.value = 1; fetchData() }

const msgFmtText = (fmt: number) => {
  switch (fmt) { case 0: return 'ASCII'; case 8: return 'UCS2'; case 15: return 'GB2312'; default: return '其他' }
}
const msgFmtType = (fmt: number) => {
  switch (fmt) { case 0: return 'info'; case 8: return 'primary'; case 15: return 'warning'; default: return 'info' }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.sms-up-page { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; background: var(--bg-card); padding: 20px 24px; border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-left h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.count-badge { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); border: none; font-weight: 500; }
.search-card { background: var(--bg-card); padding: 20px 24px; border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-input { width: 300px; }
.search-input :deep(.el-input__wrapper), .type-select :deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px var(--border-color); transition: all var(--transition-normal) ease; }
.search-input :deep(.el-input__wrapper:hover), .type-select :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--primary-light); }
.search-input :deep(.el-input__wrapper.is-focus), .type-select :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary-color); }
.type-select { width: 140px; }
.table-card { background: var(--bg-card); border-radius: var(--border-radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.custom-table { --el-table-border-color: #f1f5f9; }
.custom-table :deep(.el-table__header) { border-bottom: 1px solid #e2e8f0; }
.custom-table :deep(.el-table__row) { transition: all var(--transition-normal) ease; }
.custom-table :deep(.el-table__row:hover > td) { background: #f8fafc !important; }
.msg-id-cell { font-family: 'SF Mono', Monaco, monospace; font-size: 12px; color: #64748b; }
.phone-cell { font-weight: 500; color: var(--text-primary); }
.content-cell { color: var(--text-secondary); font-size: 13px; }
.code-cell { font-family: 'SF Mono', Monaco, monospace; font-size: 12px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #475569; }
.pagination-wrapper { padding: 20px 24px; display: flex; justify-content: flex-end; border-top: 1px solid #f1f5f9; }
.pagination-wrapper :deep(.el-pager li) { border-radius: 6px; font-weight: 500; }
.pagination-wrapper :deep(.el-pager li.is-active) { background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%); color: white; }
@media (max-width: 768px) { .search-card { flex-direction: column; align-items: stretch; } .search-input, .type-select { width: 100%; } }
</style>
