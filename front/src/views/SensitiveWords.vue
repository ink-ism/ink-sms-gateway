<template>
  <div class="sensitive-page">
    <PageHeader title="敏感词管理" :badge="`共 ${total} 条`">
      <template #actions>
        <el-input v-model="keyword" placeholder="搜索敏感词" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" plain @click="openCreateDialog">新增敏感词</el-button>
      </template>
    </PageHeader>

    <div class="panel list-panel">
      <el-alert type="info" :closable="false" class="tip-alert">
        敏感词命中后短信将被拦截（CMPP 返回 result=8）；保存后自动通知网关服务刷新词表。
      </el-alert>
      <el-table :data="words" v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="80" align="right">
          <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="word" label="敏感词" min-width="200">
          <template #default="{ row }"><span class="code-chip">{{ row.word }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" round>{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }"><span class="mono dim">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row as SensitiveWord)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row as SensitiveWord)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row as SensitiveWord)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next" background @change="loadList"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑敏感词' : '新增敏感词'" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="敏感词" required>
          <el-input v-model="wordInput" placeholder="请输入敏感词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import {
  getSensitiveList, createSensitive, updateSensitive,
  enableSensitive, disableSensitive, deleteSensitive
} from '../api/sensitive'
import type { SensitiveWord } from '../types'

const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const words = ref<SensitiveWord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getSensitiveList(page.value, size.value, keyword.value || undefined, statusFilter.value)
    words.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally { loading.value = false }
}

const handleSearch = () => {
  page.value = 1
  loadList()
}

/* ---------- 新增/编辑 ---------- */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const wordInput = ref('')
const saving = ref(false)

const openCreateDialog = () => {
  editingId.value = null
  wordInput.value = ''
  dialogVisible.value = true
}

const openEditDialog = (row: SensitiveWord) => {
  editingId.value = row.id
  wordInput.value = row.word
  dialogVisible.value = true
}

const handleSave = async () => {
  const word = wordInput.value.trim()
  if (!word) return ElMessage.warning('请输入敏感词')
  saving.value = true
  try {
    if (editingId.value) await updateSensitive(editingId.value, { word })
    else await createSensitive({ word })
    ElMessage.success(editingId.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadList()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

/* ---------- 启用/禁用/删除 ---------- */
const handleToggleStatus = async (row: SensitiveWord) => {
  try {
    if (row.status === 1) await disableSensitive(row.id)
    else await enableSensitive(row.id)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    loadList()
  } catch { /* handled by interceptor */ }
}

const handleDelete = async (row: SensitiveWord) => {
  try {
    await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？删除后不可恢复`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteSensitive(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch { /* handled by interceptor */ }
}

onMounted(loadList)
</script>

<style scoped>
.sensitive-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-panel {
  padding: 16px 20px;
}

.tip-alert {
  margin-bottom: 14px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.dim {
  color: var(--text-secondary);
}
</style>
