<template>
  <div class="channels-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-left">
          <h2>通道管理</h2>
          <el-tag class="count-badge" effect="dark" round>共 {{ total }} 个通道</el-tag>
        </div>
        <div class="header-right">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索通道名称/编码/地址" 
            clearable 
            class="search-input"
            @keyup.enter="loadChannels"
            @clear="loadChannels"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button :loading="refreshing" @click="handleRefreshChannels">
            <el-icon><Refresh /></el-icon>
            刷新通道
          </el-button>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新增通道
          </el-button>
        </div>
      </div>

      <!-- 通道表格 -->
      <div class="table-card">
        <el-table 
          :data="channelList" 
          v-loading="loading" 
          class="custom-table"
          :header-cell-style="headerCellStyle"
        >
          <el-table-column prop="code" label="通道编码" width="120">
            <template #default="{ row }">
              <span class="code-cell">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="通道名称" width="130" />
          <el-table-column label="连接地址" width="170">
            <template #default="{ row }">
              <span class="address-cell">{{ row.host }}:{{ row.port }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="spId" label="SP代码" width="100" />
          <el-table-column label="CMPP版本" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info" effect="plain">
                v{{ row.version === 32 ? '2.0' : '2.1' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxConcurrent" label="并发数" width="80" align="center" />
          <el-table-column label="启用状态" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                @change="toggleStatus(row)"
                :loading="row._toggling"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="连接状态" width="150" align="center">
            <template #default="{ row }">
              <div class="status-cell">
                <span v-if="statusMap[row.code]?.loading" class="status-loading">
                  <span class="loading-dot"></span>
                  检测中
                </span>
                <template v-else-if="statusMap[row.code]">
                  <div v-if="statusMap[row.code].connected > 0" class="status-connected">
                    <span class="status-dot pulse-dot"></span>
                    <span>{{ statusMap[row.code].connected }}/{{ statusMap[row.code].poolSize }}</span>
                  </div>
                  <div v-else class="status-disconnected">
                    <span class="status-dot"></span>
                    <span>未连接</span>
                  </div>
                </template>
                <span v-else class="status-unknown">—</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-popconfirm title="确定删除该通道？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" text type="danger">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination 
            v-model:current-page="currentPage" 
            v-model:page-size="pageSize" 
            :total="total" 
            :page-sizes="[10, 20, 50]" 
            layout="total, sizes, prev, pager, next, jumper" 
            @size-change="handlePageChange" 
            @current-change="handlePageChange"
            background
            small
          />
        </div>
      </div>

      <!-- 新增/编辑对话框 -->
      <el-dialog 
        v-model="dialogVisible" 
        :title="isEdit ? '编辑通道' : '新增通道'" 
        width="720px" 
        destroy-on-close
        class="channel-dialog"
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" label-position="right" class="channel-form">
          <!-- 基础信息 -->
          <div class="form-section">
            <div class="section-title">
              <el-icon><InfoFilled /></el-icon>
              基础信息
            </div>
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
            <div class="section-title">
              <el-icon><Lock /></el-icon>
              认证配置
            </div>
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
            <div class="section-title">
              <el-icon><Setting /></el-icon>
              协议与性能参数
            </div>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Refresh, InfoFilled, Lock, Setting, Loading, CircleCheck, CircleClose, Edit, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Channel } from '../types'
import { getChannelList, createChannel, updateChannel, deleteChannel, enableChannel, disableChannel, getChannelPoolStatus, refreshChannels } from '../api/channel'
const loading = ref(false)
const submitting = ref(false)
const refreshing = ref(false)
const channelList = ref<(Channel & { _toggling?: boolean })[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const headerCellStyle = {
  background: '#f8fafc',
  color: '#374151',
  fontWeight: '600',
  fontSize: '13px'
}

/** 连接池状态: { [channelCode]: { connected, poolSize, loading } } */
const statusMap = reactive<Record<string, { connected: number; poolSize: number; loading: boolean }>>({})
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
  checkAllConnectionStatus()
}

/** 获取所有通道的运行时连接池状态 */
const checkAllConnectionStatus = async () => {
  const channels = channelList.value
  if (channels.length === 0) return
  channels.forEach(c => { statusMap[c.code] = { connected: 0, poolSize: 0, loading: true } })
  try {
    const res = await getChannelPoolStatus()
    Object.entries(res.data).forEach(([code, status]) => {
      statusMap[code] = { connected: status.connected, poolSize: status.poolSize, loading: false }
    })
    // 对未出现在返回结果中的通道标记为未连接
    channels.forEach(c => {
      if (!res.data[c.code]) {
        statusMap[c.code] = { connected: 0, poolSize: 0, loading: false }
      }
    })
  } catch {
    channels.forEach(c => { statusMap[c.code] = { connected: 0, poolSize: 0, loading: false } })
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

const toggleStatus = async (row: Channel & { _toggling?: boolean }) => {
  row._toggling = true
  try {
    if (row.status === 1) {
      await disableChannel(row.id)
      ElMessage.success('通道已禁用')
      row.status = 0
    } else {
      await enableChannel(row.id)
      ElMessage.success('通道已启用')
      row.status = 1
    }
    checkAllConnectionStatus()
  } catch { /* handled by interceptor */ }
  row._toggling = false
}

onMounted(() => {
  loadChannels()
  statusTimer = setInterval(checkAllConnectionStatus, 30000)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.channels-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  padding: 20px 24px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.count-badge {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  border: none;
  font-weight: 500;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 240px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--border-color);
  transition: all var(--transition-normal) ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary-light);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-color);
}

/* 表格卡片 */
.table-card {
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* 自定义表格 */
.custom-table {
  --el-table-border-color: #f1f5f9;
}

.custom-table :deep(.el-table__header) {
  border-bottom: 1px solid #e2e8f0;
}

.custom-table :deep(.el-table__row) {
  transition: all var(--transition-normal) ease;
}

.custom-table :deep(.el-table__row:hover > td) {
  background: #f8fafc !important;
}

.code-cell {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  color: #475569;
}

.address-cell {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  color: #64748b;
}

/* 状态单元格 */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  animation: pulse 1s ease-in-out infinite;
}

.status-connected {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #10b981;
  font-size: 13px;
  font-weight: 500;
}

.status-disconnected {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
  font-size: 13px;
  cursor: pointer;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.pulse-dot {
  background: #10b981;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.9);
  }
}

.status-unknown {
  color: #d1d5db;
}

/* 分页 */
.pagination-wrapper {
  padding: 20px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f1f5f9;
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-button-bg-color: #f8fafc;
}

.pagination-wrapper :deep(.el-pager li) {
  border-radius: 6px;
  font-weight: 500;
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  color: white;
}

/* 对话框 */
.channel-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
}

.channel-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

/* 表单分组样式 */
.channel-form :deep(.el-form-item) { 
  margin-bottom: 14px; 
}

.form-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 24px 12px;
  margin-bottom: 16px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .el-icon {
  color: var(--primary-color);
  font-size: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .header-right {
    flex-wrap: wrap;
  }
  
  .search-input {
    width: 100%;
  }
}
</style>
