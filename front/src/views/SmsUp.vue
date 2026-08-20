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
      <el-table :data="tableData" v-loading="loading" max-height="600" style="width: 100%">
        <el-table-column prop="msgId" label="消息ID" width="150" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono msg-id-cell">{{ row.msgId }}</span></template>
        </el-table-column>
        <el-table-column prop="srcTerminalId" label="源手机号" width="140" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono phone-cell">{{ row.srcTerminalId }}</span></template>
        </el-table-column>
        <el-table-column prop="carrier" label="运营商" width="90" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.carrier && row.carrier !== '未知'" size="small" :type="carrierTagType(row.carrier)" effect="plain">{{ row.carrier }}</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="destId" label="目的号码" width="120" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.destId }}</span></template>
        </el-table-column>
        <el-table-column prop="msgContent" label="短信内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><span class="content-cell">{{ row.msgContent }}</span></template>
        </el-table-column>
        <el-table-column prop="channelCode" label="通道编码" width="120" show-overflow-tooltip>
          <template #default="{ row }"><span class="code-chip">{{ row.channelCode }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.isReport === 1 ? 'danger' : 'success'" round>
              {{ row.isReport === 1 ? '状态报告' : '上行短信' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="接收时间" width="170" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono dim-cell">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
            <el-button link type="warning" size="small" @click="handleBlacklist(row)">
              <el-icon><CircleClose /></el-icon>加黑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
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
    <el-dialog v-model="detailVisible" title="上行短信详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <!-- 上行短信信息 -->
          <div class="detail-section">
            <div class="detail-section-title">
              <el-icon><ChatLineSquare /></el-icon>
              上行短信信息
            </div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="消息ID">{{ detailData.msgId }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                <el-tag size="small" :type="detailData.isReport === 1 ? 'danger' : 'success'" round>
                  {{ detailData.isReport === 1 ? '状态报告' : '上行短信' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="源手机号">{{ detailData.srcTerminalId }}</el-descriptions-item>
              <el-descriptions-item label="运营商">
                <el-tag v-if="detailData.carrier && detailData.carrier !== '未知'" size="small" :type="carrierTagType(detailData.carrier)" effect="plain">{{ detailData.carrier }}</el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="目的号码">{{ detailData.destId || '—' }}</el-descriptions-item>
              <el-descriptions-item label="路由客户">{{ detailData.spId || '—' }}</el-descriptions-item>
              <el-descriptions-item label="短信内容" :span="2">
                <div class="detail-content">{{ detailData.msgContent || '—' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="消息格式">{{ msgFmtText(detailData.msgFmt) }}</el-descriptions-item>
              <el-descriptions-item label="业务标识">{{ detailData.serviceId || '—' }}</el-descriptions-item>
              <el-descriptions-item label="状态报告" v-if="detailData.isReport === 1">
                {{ detailData.reportStat || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="接收时间">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 关联下行短信 -->
          <div class="detail-section">
            <div class="detail-section-title">
              <el-icon><ChatDotSquare /></el-icon>
              关联下行短信
            </div>
            <el-descriptions :column="2" border size="small" v-if="detailData.downMsgContent">
              <el-descriptions-item label="下行内容" :span="2">
                <div class="detail-content">{{ detailData.downMsgContent }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="发送时间" :span="2">
                {{ formatDateTime(detailData.downCreateTime) }}
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="未找到关联下行记录" :image-size="60" />
          </div>

          <!-- 通道信息 -->
          <div class="detail-section">
            <div class="detail-section-title">
              <el-icon><Connection /></el-icon>
              使用通道信息
            </div>
            <el-descriptions :column="2" border size="small" v-if="detailData.channelName">
              <el-descriptions-item label="通道名称">{{ detailData.channelName }}</el-descriptions-item>
              <el-descriptions-item label="通道编码">{{ detailData.channelCode }}</el-descriptions-item>
              <el-descriptions-item label="服务器地址">{{ detailData.channelHost }}:{{ detailData.channelPort }}</el-descriptions-item>
              <el-descriptions-item label="通道状态">
                <el-tag size="small" :type="detailData.channelStatus === 1 ? 'success' : 'danger'">
                  {{ detailData.channelStatus === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="未找到通道信息" :image-size="60" />
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Delete, CircleClose, ChatLineSquare, ChatDotSquare, Connection } from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import { formatDateTime } from '../utils/format'
import { getSmsUpList, getSmsUpDetail, deleteSmsUp, blacklistSmsUp } from '../api/sms'
import type { SmsUp, SmsUpDetail } from '../types'

const keyword = ref('')
const typeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref<SmsUp[]>([])
const loading = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<SmsUpDetail | null>(null)

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

const carrierTagType = (carrier: string) => {
  if (carrier === '移动') return 'success'
  if (carrier === '联通') return 'warning'
  if (carrier === '电信') return 'primary'
  return 'info'
}

// 查看详情
const handleDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getSmsUpDetail(row.id)
    if (res.code === 200) { detailData.value = res.data }
    else { ElMessage.error(res.message); detailVisible.value = false }
  } finally { detailLoading.value = false }
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定删除上行短信记录 [${row.srcTerminalId}] 吗？删除后不可恢复。`,
    '删除确认',
    { type: 'warning' }
  )
    .then(async () => {
      const res = await deleteSmsUp(row.id)
      if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
      else { ElMessage.error(res.message) }
    })
    .catch(() => {})
}

// 加黑
const handleBlacklist = (row: any) => {
  ElMessageBox.confirm(
    `确定将手机号 [${row.srcTerminalId}] 加入通道 [${row.channelCode}] 黑名单吗？加黑后该通道将不再向此号码发送短信（有效期180天）。`,
    '加黑确认',
    { type: 'warning' }
  )
    .then(async () => {
      const res = await blacklistSmsUp(row.id)
      if (res.code === 200) { ElMessage.success('已加入黑名单') }
      else { ElMessage.error(res.message) }
    })
    .catch(() => {})
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
  .type-select {
    width: 100%;
  }
}
</style>
