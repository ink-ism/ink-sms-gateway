<template>
  <div class="sms-down-page">
    <!-- 页面头部 -->
    <PageHeader title="下行短信" :badge="`共 ${total} 条`">
      <template #actions>
        <el-input
          v-model="keyword"
          placeholder="搜索手机号/源号码/内容/通道"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="statusFilter"
          placeholder="状态筛选"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="已提交" value="1" />
          <el-option label="发送成功" value="3" />
          <el-option label="发送失败" value="2" />
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

    <!-- 下行短信表格 -->
    <div class="table-card panel">
      <el-table :data="tableData" v-loading="loading" max-height="600" style="width: 100%">
        <el-table-column prop="msgId" label="消息ID" width="200" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono msg-id-cell">{{ row.msgId }}</span></template>
        </el-table-column>
        <el-table-column prop="destTerminalId" label="目标手机号" width="140" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono phone-cell">{{ row.destTerminalId }}</span></template>
        </el-table-column>
        <el-table-column prop="carrier" label="运营商" width="90" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.carrier && row.carrier !== '未知'" size="small" :type="carrierTagType(row.carrier)" effect="plain">{{ row.carrier }}</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="srcId" label="源号码" width="120" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.srcId }}</span></template>
        </el-table-column>
        <el-table-column prop="signature" label="签名" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.signature" size="small" effect="plain">{{ row.signature }}</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="msgContent" label="短信内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><span class="content-cell">{{ row.msgContent }}</span></template>
        </el-table-column>
        <el-table-column prop="channelCode" label="通道编码" width="120" show-overflow-tooltip>
          <template #default="{ row }"><span class="code-chip">{{ row.channelCode }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)" round>{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="响应码" width="110" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.statusReport" size="small" :type="reportTagType(row.statusReport)" effect="plain" class="mono">{{ row.statusReport }}</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="170" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono dim-cell">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="updateTime" label="响应时间" width="170" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono dim-cell">{{ row.updateTime ? formatDateTime(row.updateTime) : '-' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="下行短信详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <div class="detail-section">
            <div class="detail-section-title">
              <el-icon><ChatLineSquare /></el-icon>
              短信信息
            </div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="消息ID">{{ detailData.msgId }}</el-descriptions-item>
              <el-descriptions-item label="目标手机号">{{ detailData.destTerminalId }}</el-descriptions-item>
              <el-descriptions-item label="运营商">
                <el-tag v-if="detailData.carrier && detailData.carrier !== '未知'" size="small" :type="carrierTagType(detailData.carrier)" effect="plain">{{ detailData.carrier }}</el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="源号码">{{ detailData.srcId || '—' }}</el-descriptions-item>
              <el-descriptions-item label="签名">
                <el-tag v-if="detailData.signature" size="small" effect="plain">{{ detailData.signature }}</el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="发送客户">{{ detailData.spId || '—' }}</el-descriptions-item>
              <el-descriptions-item label="短信内容" :span="2">
                <div class="detail-content">{{ detailData.msgContent || '—' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="通道编码">{{ detailData.channelCode || '—' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag size="small" :type="statusTagType(detailData.status)" round>{{ statusText(detailData.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="响应码">
                <el-tag v-if="detailData.statusReport" size="small" :type="reportTagType(detailData.statusReport)" effect="plain" class="mono">{{ detailData.statusReport }}</el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="计费金额">{{ detailData.fee != null ? `¥${detailData.fee}` : '—' }}</el-descriptions-item>
              <el-descriptions-item label="发送时间">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="响应时间">{{ detailData.updateTime ? formatDateTime(detailData.updateTime) : '—' }}</el-descriptions-item>
              <el-descriptions-item label="回执时间">{{ detailData.statusReportTime ? formatDateTime(detailData.statusReportTime) : '—' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 通道信息 -->
          <div class="detail-section" v-if="detailData.channelName">
            <div class="detail-section-title">
              <el-icon><Connection /></el-icon>
              使用通道信息
            </div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="通道名称">{{ detailData.channelName }}</el-descriptions-item>
              <el-descriptions-item label="通道编码">{{ detailData.channelCode }}</el-descriptions-item>
              <el-descriptions-item label="服务器地址">{{ detailData.channelHost }}:{{ detailData.channelPort }}</el-descriptions-item>
              <el-descriptions-item label="通道状态">
                <el-tag size="small" :type="detailData.channelStatus === 1 ? 'success' : 'danger'">
                  {{ detailData.channelStatus === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View, ChatLineSquare, Connection } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import { getSmsDownList, getSmsDownDetail } from '../api/sms'
import type { SmsDown, SmsDownDetail } from '../types'

const keyword = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<SmsDown[]>([])
const loading = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<SmsDownDetail | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSmsDownList(currentPage.value, pageSize.value, keyword.value || undefined)
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

const handleSearch = () => { currentPage.value = 1; fetchData() }
const handleReset = () => { keyword.value = ''; statusFilter.value = ''; currentPage.value = 1; fetchData() }

// 查看详情
const handleDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getSmsDownDetail(row.id)
    if (res.code === 200) { detailData.value = res.data }
    else { ElMessage.error(res.message); detailVisible.value = false }
  } finally { detailLoading.value = false }
}

const carrierTagType = (carrier: string) => {
  if (carrier === '移动') return 'success'
  if (carrier === '联通') return 'warning'
  if (carrier === '电信') return 'primary'
  return 'info'
}
const statusText = (status: number) => {
  switch (status) { case 1: return '已提交'; case 2: return '发送失败'; case 3: return '发送成功'; default: return '未知' }
}
const statusTagType = (status: number) => {
  switch (status) { case 1: return 'warning'; case 2: return 'danger'; case 3: return 'success'; default: return 'info' }
}
const reportTagType = (report: string) => {
  const stat = report.toUpperCase()
  if (stat === 'DELIVRD') return 'success'
  if (stat === 'ACCEPTED') return 'warning'
  return 'danger'
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.sms-down-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input {
  width: 280px;
}

.status-select {
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

.cell-empty {
  color: var(--text-muted);
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* 详情弹窗 */
.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}

.detail-content {
  word-break: break-all;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-input,
  .status-select {
    width: 100%;
  }
}
</style>
