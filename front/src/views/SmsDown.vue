<template>
  <AppLayout>
    <div class="sms-down-page">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索手机号/源号码/内容/通道"
          clearable
          style="width: 320px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="msgId" label="消息ID" width="160" show-overflow-tooltip />
        <el-table-column prop="srcId" label="源号码" width="130" />
        <el-table-column prop="destTerminalId" label="目标手机号" width="140" />
        <el-table-column prop="msgContent" label="短信内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="消息格式" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.msgFmt === 8 ? 'primary' : row.msgFmt === 0 ? 'info' : 'warning'">
              {{ msgFmtText(row.msgFmt) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serviceId" label="业务类型" width="120" show-overflow-tooltip />
        <el-table-column prop="channelCode" label="通道编码" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusReport" label="状态报告" width="120" show-overflow-tooltip />
        <el-table-column prop="errorMsg" label="错误信息" width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="180" />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { getSmsDownList } from '../api/sms'
import type { SmsDown } from '../types'

const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<SmsDown[]>([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSmsDownList(currentPage.value, pageSize.value, keyword.value || undefined)
    if (res.code === 200) {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handleReset = () => {
  keyword.value = ''
  currentPage.value = 1
  fetchData()
}

const msgFmtText = (fmt: number) => {
  switch (fmt) {
    case 0: return 'ASCII'
    case 8: return 'UCS2'
    case 15: return 'GB2312'
    default: return `未知(${fmt})`
  }
}

const statusText = (status: number) => {
  switch (status) {
    case 1: return '已提交'
    case 2: return '发送失败'
    case 3: return '发送成功'
    default: return `未知(${status})`
  }
}

const statusTagType = (status: number) => {
  switch (status) {
    case 1: return 'warning'
    case 2: return 'danger'
    case 3: return 'success'
    default: return 'info'
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.sms-down-page {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
