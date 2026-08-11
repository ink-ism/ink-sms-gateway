<template>
  <div class="sms-up-page">
    <!-- 页面头部 -->
    <PageHeader title="上行短信" :badge="`共 ${total} 条`">
      <template #actions>
        <el-input
          v-model="keyword"
          placeholder="搜索手机号/目的号码/内容/通道"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="typeFilter"
          placeholder="类型筛选"
          clearable
          class="type-select"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="上行短信" value="0" />
          <el-option label="状态报告" value="1" />
        </el-select>
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

    <!-- 上行短信表格 -->
    <div class="table-card panel">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="msgId" label="消息ID" width="150" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono msg-id-cell">{{ row.msgId }}</span></template>
        </el-table-column>
        <el-table-column prop="srcTerminalId" label="源手机号" width="140">
          <template #default="{ row }"><span class="mono phone-cell">{{ row.srcTerminalId }}</span></template>
        </el-table-column>
        <el-table-column prop="destId" label="目的号码" width="120">
          <template #default="{ row }"><span class="mono">{{ row.destId }}</span></template>
        </el-table-column>
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
        <el-table-column prop="channelCode" label="通道编码" width="120">
          <template #default="{ row }"><span class="code-chip">{{ row.channelCode }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="接收时间" width="170">
          <template #default="{ row }"><span class="mono dim-cell">{{ formatDateTime(row.createTime) }}</span></template>
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
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import { getSmsUpList } from '../api/sms'
import type { SmsUp } from '../types'

const keyword = ref('')
const typeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<SmsUp[]>([])
const loading = ref(false)

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
.sms-up-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input {
  width: 280px;
}

.type-select {
  width: 140px;
}

/* 表格卡片 */
.table-card {
  overflow: hidden;
}

.msg-id-cell {
  font-size: 12px;
  color: var(--accent);
}

.phone-cell {
  font-weight: 500;
  color: var(--text-primary);
}

.content-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.dim-cell {
  color: var(--text-secondary);
  font-size: 12px;
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
  .type-select {
    width: 100%;
  }
}
</style>
