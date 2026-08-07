<template>
  <div class="sp-page">
    <!-- 页面头部 -->
    <PageHeader title="客户管理" :badge="`共 ${total} 个客户`">
      <template #actions>
        <el-input
          v-model="keyword"
          placeholder="搜索客户标识/名称/描述"
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
        <el-button type="primary" plain @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增客户
        </el-button>
      </template>
    </PageHeader>

    <!-- 客户表格 -->
    <div class="table-card panel">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="spId" label="客户标识" width="140">
          <template #default="{ row }"><span class="code-chip">{{ row.spId }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="客户名称" width="160" show-overflow-tooltip />
        <el-table-column prop="spSecret" label="共享密钥" width="160" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono secret-cell">{{ row.spSecret }}</span></template>
        </el-table-column>
        <el-table-column label="绑定通道" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="code in row.channelCodes" :key="code" size="small" effect="plain" class="channel-tag">{{ code }}</el-tag>
            <span v-if="!row.channelCodes || row.channelCodes.length === 0" class="channel-all">全部通道</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip>
          <template #default="{ row }"><span class="content-cell">{{ row.description }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" round>{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }"><span class="mono dim-cell">{{ row.createTime }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBindDialog(row as Sp)">绑定通道</el-button>
            <el-button link type="primary" @click="openEditDialog(row as Sp)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row as Sp)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button link type="danger" @click="handleDelete(row as Sp)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="客户标识" required>
          <el-input v-model="form.spId" placeholder="CMPP 接入标识（创建后不可修改）" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="共享密钥" required>
          <el-input v-model="form.spSecret" placeholder="CMPP Connect 认证密钥" />
        </el-form-item>
        <el-form-item label="客户名称" required>
          <el-input v-model="form.name" placeholder="客户名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="客户描述（可选）" />
        </el-form-item>
        <el-form-item label="绑定通道">
          <el-select v-model="form.channelCodes" multiple placeholder="不选则不限通道" style="width: 100%">
            <el-option v-for="ch in channelOptions" :key="ch.code" :label="`${ch.name}（${ch.code}）`" :value="ch.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 绑定通道弹窗 -->
    <el-dialog v-model="bindDialogVisible" title="绑定通道" width="480px" destroy-on-close>
      <p class="bind-tip">客户 [{{ bindingSp?.spId }}] 仅可在绑定通道内发送短信，不绑定则不限通道。</p>
      <el-select v-model="bindCodes" multiple placeholder="选择通道" style="width: 100%">
        <el-option v-for="ch in channelOptions" :key="ch.code" :label="`${ch.name}（${ch.code}）`" :value="ch.code" />
      </el-select>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleBindSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import { getSpList, createSp, updateSp, deleteSp, enableSp, disableSp, bindSpChannels } from '../api/sp'
import { getChannelList } from '../api/channel'
import type { Sp, Channel } from '../types'

const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<Sp[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const bindDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const bindingSp = ref<Sp | null>(null)
const bindCodes = ref<string[]>([])
const channelOptions = ref<Channel[]>([])

const form = reactive({
  spId: '',
  spSecret: '',
  name: '',
  description: '',
  channelCodes: [] as string[]
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpList(currentPage.value, pageSize.value, keyword.value || undefined)
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
const handleReset = () => { keyword.value = ''; currentPage.value = 1; fetchData() }

const resetForm = () => {
  form.spId = ''; form.spSecret = ''; form.name = ''; form.description = ''; form.channelCodes = []
}

const openCreateDialog = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: Sp) => {
  editingId.value = row.id
  form.spId = row.spId
  form.spSecret = row.spSecret
  form.name = row.name
  form.description = row.description
  form.channelCodes = [...(row.channelCodes || [])]
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.spId || !form.spSecret || !form.name) {
    ElMessage.warning('客户标识、共享密钥、客户名称不能为空')
    return
  }
  saving.value = true
  try {
    const res = editingId.value
      ? await updateSp(editingId.value, { ...form, status: undefined } as Partial<Sp>)
      : await createSp(form as Partial<Sp>)
    if (res.code === 200) {
      ElMessage.success(res.data || '保存成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally { saving.value = false }
}

const openBindDialog = (row: Sp) => {
  bindingSp.value = row
  bindCodes.value = [...(row.channelCodes || [])]
  bindDialogVisible.value = true
}

const handleBindSave = async () => {
  if (!bindingSp.value) return
  saving.value = true
  try {
    const res = await bindSpChannels(bindingSp.value.id, bindCodes.value)
    if (res.code === 200) {
      ElMessage.success(res.data || '通道绑定更新成功')
      bindDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '通道绑定更新失败')
    }
  } finally { saving.value = false }
}

const handleToggleStatus = async (row: Sp) => {
  const res = row.status === 1 ? await disableSp(row.id) : await enableSp(row.id)
  if (res.code === 200) { ElMessage.success(res.data); fetchData() } else { ElMessage.error(res.message) }
}

const handleDelete = (row: Sp) => {
  ElMessageBox.confirm(`确定删除客户 [${row.spId}] 吗？其通道绑定将一并清理。`, '删除确认', { type: 'warning' })
    .then(async () => {
      const res = await deleteSp(row.id)
      if (res.code === 200) { ElMessage.success(res.data); fetchData() } else { ElMessage.error(res.message) }
    })
    .catch(() => {})
}

onMounted(() => { fetchData(); fetchChannels() })
</script>

<style scoped>
.sp-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input {
  width: 280px;
}

/* 表格卡片 */
.table-card {
  overflow: hidden;
}

.secret-cell {
  font-size: 12px;
  color: var(--text-secondary);
}

.content-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.dim-cell {
  color: var(--text-secondary);
  font-size: 12px;
}

.channel-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}

.channel-all {
  color: var(--text-muted);
  font-size: 12px;
}

.bind-tip {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
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
