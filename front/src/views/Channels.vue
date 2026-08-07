<template>
  <div class="channels-page">
    <!-- 页面头部 -->
    <PageHeader title="通道管理" :badge="`共 ${total} 个通道`">
      <template #actions>
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
      </template>
    </PageHeader>

    <!-- 通道表格 -->
    <div class="table-card panel">
      <el-table :data="channelList" v-loading="loading">
        <el-table-column prop="code" label="通道编码" width="120">
          <template #default="{ row }">
            <span class="code-chip">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="通道名称" width="120" show-overflow-tooltip />
        <el-table-column label="连接地址" width="160">
          <template #default="{ row }">
            <span class="mono address-cell">{{ row.host }}:{{ row.port }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="spId" label="SP代码" width="90" show-overflow-tooltip />
        <el-table-column label="CMPP版本" width="95">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              v{{ row.version === 32 ? '2.0' : '2.1' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxConcurrent" label="并发数" width="70" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.maxConcurrent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="toggleStatus(row as ChannelRow)"
              :loading="row._toggling"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="连接状态" width="130" align="center">
          <template #default="{ row }">
            <StatusDot
              v-if="statusMap[row.code]?.loading"
              status="loading"
              label="检测中"
            />
            <StatusDot
              v-else-if="statusMap[row.code] && statusMap[row.code].connected > 0"
              status="online"
              :label="`${statusMap[row.code].connected}/${statusMap[row.code].poolSize}`"
            />
            <StatusDot
              v-else-if="statusMap[row.code]"
              status="offline"
              label="未连接"
            />
            <span v-else class="status-unknown">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right" align="center" class-name="action-column">
          <template #default="{ row }">
            <el-button size="small" text type="success" @click="openTestSendDialog(row as Channel)">
              <el-icon><Promotion /></el-icon>
              测试
            </el-button>
            <el-button size="small" text type="primary" @click="openEditDialog(row as Channel)">
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

    <!-- 测试发送对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="通道测试发送"
      width="500px"
      destroy-on-close
    >
      <div class="test-send-form">
        <div class="test-channel-info">
          <span class="code-chip">{{ testForm.channelCode }}</span>
          <span class="test-channel-name">{{ testForm.channelName }}</span>
        </div>
        <el-form label-width="80px" style="margin-top: 16px">
          <el-form-item label="手机号">
            <el-input v-model="testForm.phone" placeholder="请输入测试手机号" clearable />
          </el-form-item>
          <el-form-item label="短信内容">
            <el-input v-model="testForm.content" type="textarea" :rows="3" placeholder="请输入测试短信内容" />
          </el-form-item>
          <el-form-item label="源号码">
            <el-input v-model="testForm.srcId" placeholder="选填，默认 10690000" clearable />
          </el-form-item>
        </el-form>
        <div v-if="testResult" class="test-result" :class="testResult.success ? 'result-success' : 'result-error'">
          <el-icon><component :is="testResult.success ? CircleCheck : CircleClose" /></el-icon>
          <span>{{ testResult.message }}</span>
          <span v-if="testResult.serverMsgId" class="mono server-msg-id">MsgId: {{ testResult.serverMsgId }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="testDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="testSending" :disabled="!testForm.phone || !testForm.content" @click="handleTestSend">
          发送测试
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑对话框（独立组件） -->
    <ChannelFormDialog v-model="dialogVisible" :channel="editingChannel" @saved="loadChannels" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Refresh, CircleCheck, CircleClose, Edit, Delete, Promotion } from '@element-plus/icons-vue'
import type { Channel } from '../types'
import { getChannelList, deleteChannel, enableChannel, disableChannel, getChannelPoolStatus, refreshChannels, testSendChannel } from '../api/channel'
import PageHeader from '../components/common/PageHeader.vue'
import StatusDot from '../components/common/StatusDot.vue'
import ChannelFormDialog from '../components/common/ChannelFormDialog.vue'

/** 表格行类型：通道 + 启停中临时标记 */
type ChannelRow = Channel & { _toggling?: boolean }

const loading = ref(false)
const refreshing = ref(false)

// 测试发送相关
const testDialogVisible = ref(false)
const testSending = ref(false)
const testResult = ref<{ success: boolean; message: string; serverMsgId?: string } | null>(null)
const testForm = reactive({ channelCode: '', channelName: '', phone: '', content: '', srcId: '' })

const channelList = ref<ChannelRow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

/** 连接池状态: { [channelCode]: { connected, poolSize, loading } } */
const statusMap = reactive<Record<string, { connected: number; poolSize: number; loading: boolean }>>({})
let statusTimer: ReturnType<typeof setInterval> | null = null

// 新增/编辑对话框
const dialogVisible = ref(false)
const editingChannel = ref<Channel | null>(null)

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
  editingChannel.value = null
  dialogVisible.value = true
}

const openEditDialog = (row: Channel) => {
  editingChannel.value = row
  dialogVisible.value = true
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

const openTestSendDialog = (row: Channel) => {
  testForm.channelCode = row.code
  testForm.channelName = row.name
  testForm.phone = ''
  testForm.content = ''
  testForm.srcId = ''
  testResult.value = null
  testDialogVisible.value = true
}

const handleTestSend = async () => {
  if (!testForm.phone || !testForm.content) return
  testSending.value = true
  testResult.value = null
  try {
    const res = await testSendChannel({
      channelCode: testForm.channelCode,
      phone: testForm.phone,
      content: testForm.content,
      srcId: testForm.srcId || undefined
    })
    testResult.value = { success: true, message: res.data.message, serverMsgId: res.data.serverMsgId }
    ElMessage.success('测试短信发送成功')
  } catch (e: any) {
    testResult.value = { success: false, message: e?.message || '发送失败' }
  } finally {
    testSending.value = false
  }
}

const toggleStatus = async (row: ChannelRow) => {
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
  gap: 16px;
}

.search-input {
  width: 240px;
}

/* 表格卡片 */
.table-card {
  overflow: hidden;
}

/* 操作列：紧凑单行，避免按钮换行 */
.table-card :deep(.action-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.table-card :deep(.action-column .el-button) {
  padding: 4px 6px;
  flex-shrink: 0;
}

.table-card :deep(.action-column .el-button + .el-button),
.table-card :deep(.action-column .el-popconfirm + .el-button),
.table-card :deep(.action-column .el-button + .el-popconfirm .el-button) {
  margin-left: 2px;
}

.address-cell {
  font-size: 12px;
  color: var(--text-secondary);
}

.status-unknown {
  color: var(--text-muted);
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* 测试发送对话框 */
.test-channel-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.test-channel-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.test-result {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  flex-wrap: wrap;
}

.result-success {
  color: var(--success);
  background: var(--success-soft);
  border: 1px solid rgba(0, 255, 163, 0.2);
}

.result-error {
  color: var(--danger);
  background: var(--danger-soft);
  border: 1px solid rgba(255, 77, 109, 0.2);
}

.server-msg-id {
  font-size: 11px;
  opacity: 0.85;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-input {
    width: 100%;
  }
}
</style>
