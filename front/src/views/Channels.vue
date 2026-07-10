<template>
  <AppLayout>
    <div class="channels-page">
      <div class="page-header">
        <h2>通道管理</h2>
        <div class="header-actions">
          <el-input v-model="searchKeyword" placeholder="搜索通道名称/编码/地址" clearable style="width: 260px" @keyup.enter="loadChannels">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :loading="refreshing" @click="handleRefreshChannels">
            <el-icon><Refresh /></el-icon> 刷新通道
          </el-button>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 新增通道
          </el-button>
        </div>
      </div>

      <el-table :data="channelList" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="code" label="通道编码" width="120" />
        <el-table-column prop="name" label="通道名称" width="140" />
        <el-table-column label="连接地址" width="180">
          <template #default="{ row }">{{ row.host }}:{{ row.port }}</template>
        </el-table-column>
        <el-table-column prop="spId" label="SP代码" width="100" />
        <el-table-column label="CMPP版本" width="100">
          <template #default="{ row }">0x{{ row.version?.toString(16).toUpperCase().padStart(2, '0') }}</template>
        </el-table-column>
        <el-table-column prop="heartbeatInterval" label="心跳(s)" width="80" align="center" />
        <el-table-column prop="maxConcurrent" label="最大并发" width="80" align="center" />
        <el-table-column label="启用状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连接状态" width="140" align="center">
          <template #default="{ row }">
            <span v-if="statusMap[row.id]?.loading" class="status-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </span>
            <template v-else-if="statusMap[row.id]">
              <el-tag v-if="statusMap[row.id].connected" type="success" size="small" effect="plain">
                <el-icon style="vertical-align: middle"><CircleCheck /></el-icon> 已连接
                <span class="latency-text">{{ statusMap[row.id].latency }}ms</span>
              </el-tag>
              <el-tooltip v-else :content="statusMap[row.id].reason" placement="top">
                <el-tag type="danger" size="small" effect="plain">
                  <el-icon style="vertical-align: middle"><CircleClose /></el-icon> 未连接
                </el-tag>
              </el-tooltip>
            </template>
            <span v-else class="status-unknown">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定删除该通道？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="handlePageChange" @current-change="handlePageChange" />
      </div>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑通道' : '新增通道'" width="720px" destroy-on-close>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" label-position="right" class="channel-form">

          <!-- 基础信息 -->
          <div class="form-section">
            <div class="section-title"><el-icon><InfoFilled /></el-icon> 基础信息</div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="通道编码" prop="code">
                  <el-input v-model="formData.code" placeholder="如: cmpp-emay" :disabled="isEdit" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="通道名称" prop="name">
                  <el-input v-model="formData.name" placeholder="如: 亿美软通" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="16">
                <el-form-item label="服务器地址" prop="host">
                  <el-input v-model="formData.host" placeholder="如: 127.0.0.1" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="端口" prop="port">
                  <el-input-number v-model="formData.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 认证配置 -->
          <div class="form-section">
            <div class="section-title"><el-icon><Lock /></el-icon> 认证配置</div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="SP企业代码" prop="spId">
                  <el-input v-model="formData.spId" placeholder="如: test" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="共享密钥" prop="sharedSecret">
                  <el-input v-model="formData.sharedSecret" placeholder="请输入密钥" show-password />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 协议与性能参数 -->
          <div class="form-section">
            <div class="section-title"><el-icon><Setting /></el-icon> 协议与性能参数</div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="CMPP版本" prop="version">
                  <el-select v-model="formData.version" style="width: 100%">
                    <el-option label="CMPP 2.0 (0x20)" :value="32" />
                    <el-option label="CMPP 2.1 (0x21)" :value="33" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大并发数" prop="maxConcurrent">
                  <el-input-number v-model="formData.maxConcurrent" :min="1" :max="100" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="心跳间隔(s)" prop="heartbeatInterval">
                  <el-input-number v-model="formData.heartbeatInterval" :min="10" :max="300" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="重连间隔(s)" prop="reconnectInterval">
                  <el-input-number v-model="formData.reconnectInterval" :min="5" :max="120" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大重连(s)" prop="maxReconnectInterval">
                  <el-input-number v-model="formData.maxReconnectInterval" :min="10" :max="300" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="连接超时(ms)" prop="connectTimeout">
                  <el-input-number v-model="formData.connectTimeout" :min="1000" :max="30000" :step="500" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态">
                  <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 备注 -->
          <div class="form-section">
            <el-form-item label="描述">
              <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="可选备注描述" />
            </el-form-item>
          </div>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Refresh, InfoFilled, Lock, Setting, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Channel } from '../types'
import { getChannelList, createChannel, updateChannel, deleteChannel, enableChannel, disableChannel, getConnectionStatusBatch, refreshChannels } from '../api/channel'
import AppLayout from '../components/AppLayout.vue'

const loading = ref(false)
const submitting = ref(false)
const refreshing = ref(false)
const channelList = ref<Channel[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

/** 连接状态: { [id]: { connected, reason, latency, loading } } */
const statusMap = reactive<Record<number, { connected: boolean; reason: string; latency: number; loading: boolean }>>({})
let statusTimer: ReturnType<typeof setInterval> | null = null

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const defaultForm = {
  code: '', name: '', host: '', port: 7890, spId: '', sharedSecret: '',
  version: 32, heartbeatInterval: 60, reconnectInterval: 10,
  maxReconnectInterval: 60, connectTimeout: 5000, maxConcurrent: 10,
  status: 1, description: ''
}
const formData = reactive({ ...defaultForm })

const formRules: FormRules = {
  code: [{ required: true, message: '请输入通道编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入通道名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入服务器地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  spId: [{ required: true, message: '请输入SP企业代码', trigger: 'blur' }],
  sharedSecret: [{ required: true, message: '请输入共享密钥', trigger: 'blur' }]
}

const loadChannels = async () => {
  loading.value = true
  try {
    const res = await getChannelList(currentPage.value, pageSize.value, searchKeyword.value || undefined)
    channelList.value = res.data.list
    total.value = res.data.total
  } catch { /* handled by interceptor */ }
  loading.value = false
  // 加载完后自动检测连接状态
  checkAllConnectionStatus()
}

/** 批量检测当前页所有通道的连接状态 */
const checkAllConnectionStatus = async () => {
  const ids = channelList.value.map(c => c.id)
  if (ids.length === 0) return
  ids.forEach(id => { statusMap[id] = { connected: false, reason: '', latency: 0, loading: true } })
  try {
    const res = await getConnectionStatusBatch(ids)
    Object.entries(res.data).forEach(([idStr, status]) => {
      const id = Number(idStr)
      statusMap[id] = { connected: status.connected, reason: status.reason, latency: status.latency, loading: false }
    })
  } catch {
    ids.forEach(id => { statusMap[id] = { connected: false, reason: '检测失败', latency: 0, loading: false } })
  }
}

const handlePageChange = () => {
  loadChannels()
}

const openCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, defaultForm)
  dialogVisible.value = true
}

const openEditDialog = (row: Channel) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(formData, {
    code: row.code, name: row.name, host: row.host, port: row.port,
    spId: row.spId, sharedSecret: row.sharedSecret, version: row.version,
    heartbeatInterval: row.heartbeatInterval, reconnectInterval: row.reconnectInterval,
    maxReconnectInterval: row.maxReconnectInterval, connectTimeout: row.connectTimeout,
    maxConcurrent: row.maxConcurrent, status: row.status, description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateChannel(editId.value, { ...formData })
      ElMessage.success('通道更新成功')
    } else {
      await createChannel({ ...formData })
      ElMessage.success('通道创建成功')
    }
    dialogVisible.value = false
    loadChannels()
  } catch { /* handled by interceptor */ }
  submitting.value = false
}

const handleDelete = async (id: number) => {
  try {
    await deleteChannel(id)
    ElMessage.success('通道已删除')
    loadChannels()
  } catch { /* handled by interceptor */ }
}

const handleRefreshChannels = async () => {
  refreshing.value = true
  try {
    await refreshChannels()
    ElMessage.success('通道配置已刷新')
    await loadChannels()
  } catch {
    ElMessage.error('刷新通道失败')
  } finally {
    refreshing.value = false
  }
}

const toggleStatus = async (row: Channel) => {
  try {
    if (row.status === 1) {
      await disableChannel(row.id)
      ElMessage.success('通道已禁用')
    } else {
      await enableChannel(row.id)
      ElMessage.success('通道已启用')
    }
    loadChannels()
  } catch { /* handled by interceptor */ }
}

onMounted(() => {
  loadChannels()
  // 每 30 秒自动刷新连接状态
  statusTimer = setInterval(checkAllConnectionStatus, 30000)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.channels-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; color: #333; }
.header-actions { display: flex; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 表单分组样式 */
.channel-form :deep(.el-form-item) { margin-bottom: 14px; }
.form-section {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px 20px 8px;
  margin-bottom: 16px;
}
.form-section:last-child {
  margin-bottom: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 6px;
}
.section-title .el-icon {
  color: #409eff;
  font-size: 15px;
}

/* 连接状态样式 */
.status-loading { color: #909399; font-size: 16px; }
.status-unknown { color: #c0c4cc; }
.latency-text {
  font-size: 11px;
  color: #67c23a;
  margin-left: 2px;
}
</style>
