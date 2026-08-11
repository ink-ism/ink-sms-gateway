<template>
  <div class="stats-page">
    <!-- 页面头部 -->
    <PageHeader title="数据统计" :badge="`近 ${days} 天`">
      <template #actions>
        <el-radio-group v-model="days" size="default" @change="loadAll">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
        </el-radio-group>
      </template>
    </PageHeader>

    <!-- 趋势图 -->
    <div class="chart-card panel">
      <div class="chart-header">
        <h3>发送趋势</h3>
      </div>
      <div class="chart-body">
        <v-chart class="chart" :option="trendOption" autoresize />
      </div>
    </div>

    <div class="tables-grid">
      <!-- 通道维度 -->
      <div class="table-card panel">
        <div class="table-header">
          <h3>通道维度</h3>
        </div>
        <el-table :data="channelStats" v-loading="channelLoading" size="small">
          <el-table-column prop="channelCode" label="通道" min-width="120">
            <template #default="{ row }"><span class="code-chip">{{ row.channelCode }}</span></template>
          </el-table-column>
          <el-table-column prop="total" label="发送量" width="90" align="right">
            <template #default="{ row }"><span class="mono">{{ row.total }}</span></template>
          </el-table-column>
          <el-table-column prop="success" label="成功" width="80" align="right">
            <template #default="{ row }"><span class="mono">{{ row.success }}</span></template>
          </el-table-column>
          <el-table-column prop="fail" label="失败" width="80" align="right">
            <template #default="{ row }"><span class="mono fail-cell">{{ row.fail }}</span></template>
          </el-table-column>
          <el-table-column label="成功率" width="130">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.successRate)" :stroke-width="6" :show-text="false" />
              <span class="mono rate-text">{{ row.successRate }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 客户维度 -->
      <div class="table-card panel">
        <div class="table-header">
          <h3>客户维度</h3>
        </div>
        <el-table :data="spStats" v-loading="spLoading" size="small">
          <el-table-column prop="spId" label="客户" min-width="120">
            <template #default="{ row }"><span class="code-chip">{{ row.spId }}</span></template>
          </el-table-column>
          <el-table-column prop="total" label="发送量" width="90" align="right">
            <template #default="{ row }"><span class="mono">{{ row.total }}</span></template>
          </el-table-column>
          <el-table-column prop="success" label="成功" width="80" align="right">
            <template #default="{ row }"><span class="mono">{{ row.success }}</span></template>
          </el-table-column>
          <el-table-column label="消费(元)" width="110" align="right">
            <template #default="{ row }"><span class="mono fee-cell">{{ formatAmount(row.fee) }}</span></template>
          </el-table-column>
          <el-table-column label="成功率" width="130">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.successRate)" :stroke-width="6" :show-text="false" />
              <span class="mono rate-text">{{ row.successRate }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import PageHeader from '../components/common/PageHeader.vue'
import { getStatsTrend, getChannelStats, getSpStats } from '../api/stats'
import { chartColors, darkTooltip, areaGradient } from '../utils/echarts-theme'
import type { TrendPoint, ChannelStat, SpStat } from '../types'

use([CanvasRenderer, LineChart, TooltipComponent, LegendComponent, GridComponent])

const days = ref(7)
const trendPoints = ref<TrendPoint[]>([])
const channelStats = ref<ChannelStat[]>([])
const spStats = ref<SpStat[]>([])
const channelLoading = ref(false)
const spLoading = ref(false)

const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'line', lineStyle: { color: chartColors.axisLine } },
    ...darkTooltip
  },
  legend: {
    top: 0,
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: chartColors.text }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: 36, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trendPoints.value.map(p => p.date.slice(5)),
    axisLine: { lineStyle: { color: chartColors.axisLine } },
    axisTick: { show: false },
    axisLabel: { color: chartColors.text }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: { lineStyle: { color: chartColors.splitLine } },
    axisLabel: { color: chartColors.text }
  },
  series: [
    {
      name: '发送总量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: chartColors.accent },
      areaStyle: { color: areaGradient(chartColors.accent) },
      itemStyle: { color: chartColors.accent },
      data: trendPoints.value.map(p => Number(p.total))
    },
    {
      name: '成功数量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: chartColors.success },
      areaStyle: { color: areaGradient(chartColors.success, 0.22) },
      itemStyle: { color: chartColors.success },
      data: trendPoints.value.map(p => Number(p.success))
    }
  ]
}))

const formatAmount = (value: number | null | undefined) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(4).replace(/0+$/, '').replace(/\.$/, '') : '0'
}

const loadTrend = async () => {
  try {
    const res = await getStatsTrend(days.value)
    trendPoints.value = res.data || []
  } catch (error) {
    console.error('加载趋势失败:', error)
  }
}

const loadChannelStats = async () => {
  channelLoading.value = true
  try {
    const res = await getChannelStats(days.value)
    channelStats.value = res.data || []
  } finally { channelLoading.value = false }
}

const loadSpStats = async () => {
  spLoading.value = true
  try {
    const res = await getSpStats(days.value)
    spStats.value = res.data || []
  } finally { spLoading.value = false }
}

const loadAll = () => {
  loadTrend()
  loadChannelStats()
  loadSpStats()
}

onMounted(loadAll)
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chart-card {
  padding: 22px 24px;
}

.chart-header {
  margin-bottom: 16px;
}

.chart-header h3,
.table-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.chart-body {
  height: 280px;
}

.chart {
  width: 100%;
  height: 100%;
}

.tables-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.table-card {
  overflow: hidden;
}

.table-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}

.fail-cell {
  color: var(--danger, #f56c6c);
}

.fee-cell {
  color: var(--accent);
}

.rate-text {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1200px) {
  .tables-grid {
    grid-template-columns: 1fr;
  }
}
</style>
