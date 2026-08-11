<template>
  <div class="sigtpl-page">
    <PageHeader title="签名模板" :badge="activeTab === 'signature' ? `签名 ${sigTotal} 条` : `模板 ${tplTotal} 条`">
      <template #actions>
        <el-input v-model="keyword" placeholder="关键词搜索" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" plain @click="openCreateDialog">{{ activeTab === 'signature' ? '新增签名' : '新增模板' }}</el-button>
      </template>
    </PageHeader>

    <div class="panel list-panel">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 签名 -->
        <el-tab-pane label="签名管理" name="signature">
          <el-table :data="signatures" v-loading="sigLoading" size="small">
            <el-table-column prop="id" label="ID" width="70" align="right">
              <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
            </el-table-column>
            <el-table-column prop="content" label="签名内容" width="150">
              <template #default="{ row }"><span class="code-chip">【{{ row.content }}】</span></template>
            </el-table-column>
            <el-table-column label="归属" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.spId" size="small" effect="plain">{{ row.spId }}</el-tag>
                <el-tag v-else size="small" type="info" effect="plain">平台全局</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
              <template #default="{ row }"><span class="dim">{{ row.remark || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170">
              <template #default="{ row }"><span class="mono dim">{{ formatDateTime(row.createTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="success" @click="handleApprove('signature', row as Signature)">通过</el-button>
                <el-button v-if="row.status === 0" link type="warning" @click="handleReject('signature', row as Signature)">驳回</el-button>
                <el-button link type="primary" @click="openEditDialog('signature', row as Signature)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete('signature', row as Signature)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="sigPage" v-model:page-size="sigSize"
              :total="sigTotal" :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next" background @change="loadSignatures"
            />
          </div>
        </el-tab-pane>

        <!-- 模板 -->
        <el-tab-pane label="模板管理" name="template">
          <el-table :data="templates" v-loading="tplLoading" size="small">
            <el-table-column prop="id" label="ID" width="70" align="right">
              <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
            </el-table-column>
            <el-table-column prop="name" label="模板名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="content" label="模板内容" min-width="220" show-overflow-tooltip>
              <template #default="{ row }"><span class="dim">{{ row.content }}</span></template>
            </el-table-column>
            <el-table-column label="关联签名" width="140">
              <template #default="{ row }">
                <span v-if="row.signatureContent" class="code-chip">【{{ row.signatureContent }}】</span>
                <span v-else class="dim">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170">
              <template #default="{ row }"><span class="mono dim">{{ formatDateTime(row.createTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="success" @click="handleApprove('template', row as Template)">通过</el-button>
                <el-button v-if="row.status === 0" link type="warning" @click="handleReject('template', row as Template)">驳回</el-button>
                <el-button link type="primary" @click="openEditDialog('template', row as Template)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete('template', row as Template)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="tplPage" v-model:page-size="tplSize"
              :total="tplTotal" :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next" background @change="loadTemplates"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <template v-if="dialogKind === 'signature'">
          <el-form-item label="签名内容" required>
            <el-input v-model="form.content" placeholder="不含【】，如: 墨迹科技" />
          </el-form-item>
          <el-form-item label="归属客户">
            <el-input v-model="form.spId" placeholder="留空则为平台全局签名" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="模板名称" required>
            <el-input v-model="form.name" placeholder="如: 验证码模板" />
          </el-form-item>
          <el-form-item label="模板内容" required>
            <el-input v-model="form.content" type="textarea" :rows="4" placeholder="模板正文内容" />
          </el-form-item>
          <el-form-item label="关联签名">
            <el-select v-model="form.signatureId" clearable filterable placeholder="可选，关联已通过的签名" style="width: 100%">
              <el-option v-for="sig in approvedSignatures" :key="sig.id" :label="`【${sig.content}】`" :value="sig.id" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import {
  getSignatureList, createSignature, updateSignature, approveSignature, rejectSignature, deleteSignature,
  getTemplateList, createTemplate, updateTemplate, approveTemplate, rejectTemplate, deleteTemplate
} from '../api/signature'
import type { Signature, Template } from '../types'

type Kind = 'signature' | 'template'

const activeTab = ref<Kind>('signature')
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

/* ---------- 签名列表 ---------- */
const signatures = ref<Signature[]>([])
const sigTotal = ref(0)
const sigPage = ref(1)
const sigSize = ref(10)
const sigLoading = ref(false)

const loadSignatures = async () => {
  sigLoading.value = true
  try {
    const res = await getSignatureList(sigPage.value, sigSize.value, keyword.value || undefined, statusFilter.value)
    signatures.value = res.data?.list || []
    sigTotal.value = Number(res.data?.total) || 0
  } finally { sigLoading.value = false }
}

/* ---------- 模板列表 ---------- */
const templates = ref<Template[]>([])
const tplTotal = ref(0)
const tplPage = ref(1)
const tplSize = ref(10)
const tplLoading = ref(false)

const loadTemplates = async () => {
  tplLoading.value = true
  try {
    const res = await getTemplateList(tplPage.value, tplSize.value, keyword.value || undefined, statusFilter.value)
    templates.value = res.data?.list || []
    tplTotal.value = Number(res.data?.total) || 0
  } finally { tplLoading.value = false }
}

/** 已通过签名：供模板表单关联选择 */
const approvedSignatures = ref<Signature[]>([])
const loadApprovedSignatures = async () => {
  try {
    const res = await getSignatureList(1, 200, undefined, 1)
    approvedSignatures.value = res.data?.list || []
  } catch { /* 忽略 */ }
}

const handleSearch = () => {
  sigPage.value = 1
  tplPage.value = 1
  if (activeTab.value === 'signature') loadSignatures()
  else loadTemplates()
}

const handleTabChange = () => {
  if (activeTab.value === 'signature') loadSignatures()
  else loadTemplates()
}

/* ---------- 状态展示 ---------- */
const statusLabel = (status: number) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' } as Record<number, string>)[status] || '未知'
const statusTagType = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'danger' } as Record<number, 'warning' | 'success' | 'danger'>)[status] || 'info'

/* ---------- 审核 ---------- */
const handleApprove = async (kind: Kind, row: Signature | Template) => {
  try {
    await ElMessageBox.confirm(`确认通过「${kind === 'signature' ? (row as Signature).content : (row as Template).name}」的审核？`, '审核确认', { type: 'success' })
  } catch { return }
  try {
    if (kind === 'signature') await approveSignature(row.id)
    else await approveTemplate(row.id)
    ElMessage.success('审核通过')
    refreshCurrent()
  } catch { /* handled by interceptor */ }
}

const handleReject = async (kind: Kind, row: Signature | Template) => {
  let remark = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '审核驳回', {
      confirmButtonText: '驳回', cancelButtonText: '取消',
      inputPlaceholder: '驳回原因（可选）', inputType: 'textarea'
    })
    remark = value || ''
  } catch { return }
  try {
    if (kind === 'signature') await rejectSignature(row.id, remark)
    else await rejectTemplate(row.id, remark)
    ElMessage.success('已驳回')
    refreshCurrent()
  } catch { /* handled by interceptor */ }
}

const handleDelete = async (kind: Kind, row: Signature | Template) => {
  try {
    await ElMessageBox.confirm(`确认删除「${kind === 'signature' ? (row as Signature).content : (row as Template).name}」？删除后不可恢复`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    if (kind === 'signature') await deleteSignature(row.id)
    else await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    refreshCurrent()
  } catch { /* handled by interceptor */ }
}

/* ---------- 新增/编辑 ---------- */
const dialogVisible = ref(false)
const dialogKind = ref<Kind>('signature')
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ content: '', spId: '', name: '', signatureId: null as number | null })

const dialogTitle = computed(() => {
  const noun = dialogKind.value === 'signature' ? '签名' : '模板'
  return editingId.value ? `编辑${noun}` : `新增${noun}`
})

const openCreateDialog = () => {
  dialogKind.value = activeTab.value
  editingId.value = null
  Object.assign(form, { content: '', spId: '', name: '', signatureId: null })
  if (dialogKind.value === 'template') loadApprovedSignatures()
  dialogVisible.value = true
}

const openEditDialog = (kind: Kind, row: Signature | Template) => {
  dialogKind.value = kind
  editingId.value = row.id
  if (kind === 'signature') {
    const sig = row as Signature
    Object.assign(form, { content: sig.content, spId: sig.spId || '', name: '', signatureId: null })
  } else {
    const tpl = row as Template
    Object.assign(form, { content: tpl.content, spId: '', name: tpl.name, signatureId: tpl.signatureId })
    loadApprovedSignatures()
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (dialogKind.value === 'signature') {
    if (!form.content.trim()) return ElMessage.warning('请输入签名内容')
  } else {
    if (!form.name.trim()) return ElMessage.warning('请输入模板名称')
    if (!form.content.trim()) return ElMessage.warning('请输入模板内容')
  }
  saving.value = true
  try {
    if (dialogKind.value === 'signature') {
      const data = { content: form.content.trim(), spId: form.spId.trim() || null }
      if (editingId.value) await updateSignature(editingId.value, data)
      else await createSignature(data)
    } else {
      const data = { name: form.name.trim(), content: form.content.trim(), signatureId: form.signatureId }
      if (editingId.value) await updateTemplate(editingId.value, data)
      else await createTemplate(data)
    }
    ElMessage.success(editingId.value ? '更新成功' : '创建成功，待审核')
    dialogVisible.value = false
    refreshCurrent()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

const refreshCurrent = () => {
  if (activeTab.value === 'signature') loadSignatures()
  else loadTemplates()
}

onMounted(() => {
  loadSignatures()
  loadTemplates()
})
</script>

<style scoped>
.sigtpl-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-panel {
  padding: 8px 20px 16px;
}

.list-panel :deep(.el-tabs__header) {
  margin-bottom: 12px;
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
