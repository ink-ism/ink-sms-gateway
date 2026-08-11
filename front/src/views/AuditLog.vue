<template>
  <div class="audit-page">
    <PageHeader title="审计日志" :badge="`共 ${total} 条`">
      <template #actions>
        <el-select v-model="moduleFilter" placeholder="模块" clearable style="width: 130px" @change="handleSearch">
          <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <el-select v-model="actionFilter" placeholder="操作" clearable style="width: 120px" @change="handleSearch">
          <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
        </el-select>
        <el-input v-model="keyword" placeholder="关键词（操作人/目标）" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </template>
    </PageHeader>

    <div class="panel list-panel">
      <el-table :data="logs" v-loading="loading" size="small">
        <el-table-column prop="createTime" label="操作时间" width="170">
          <template #default="{ row }"><span class="mono dim">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="120">
          <template #default="{ row }">
            <span v-if="row.username" class="mono">{{ row.username }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ moduleLabel(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTagType(row.action)" effect="plain">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标对象" width="140" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono dim">{{ row.target || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip>
          <template #default="{ row }"><span class="dim detail-text">{{ row.detail || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }"><span class="mono dim">{{ row.ip || '-' }}</span></template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next" background @change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import { getAuditLogs } from '../api/audit'
import type { AuditLog } from '../types'

const moduleOptions = [
  { value: 'CHANNEL', label: '通道' },
  { value: 'SP', label: '客户' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'BLACKLIST', label: '黑名单' },
  { value: 'SIGNATURE', label: '签名' },
  { value: 'TEMPLATE', label: '模板' },
  { value: 'SENSITIVE', label: '敏感词' }
]

const actionOptions = [
  { value: 'CREATE', label: '新增' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' },
  { value: 'ENABLE', label: '启用' },
  { value: 'DISABLE', label: '禁用' },
  { value: 'RECHARGE', label: '充值' },
  { value: 'APPROVE', label: '审核通过' },
  { value: 'REJECT', label: '审核驳回' }
]

const moduleMap = Object.fromEntries(moduleOptions.map(m => [m.value, m.label]))
const actionMap = Object.fromEntries(actionOptions.map(a => [a.value, a.label]))

const moduleLabel = (module: string) => moduleMap[module] || module
const actionLabel = (action: string) => actionMap[action] || action

const actionTagType = (action: string) => {
  switch (action) {
    case 'CREATE': return 'success'
    case 'DELETE': case 'DISABLE': case 'REJECT': return 'danger'
    case 'ENABLE': case 'APPROVE': case 'RECHARGE': return 'warning'
    default: return 'info'
  }
}

const moduleFilter = ref<string | undefined>(undefined)
const actionFilter = ref<string | undefined>(undefined)
const keyword = ref('')
const logs = ref<AuditLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs({
      page: page.value, size: size.value,
      module: moduleFilter.value, action: actionFilter.value,
      keyword: keyword.value || undefined
    })
    logs.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally { loading.value = false }
}

const handleSearch = () => {
  page.value = 1
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-panel {
  padding: 16px 20px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.dim {
  color: var(--text-secondary);
}

.detail-text {
  font-size: 12px;
  word-break: break-all;
}
</style>
