<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <StatCard class="card-enter stagger-delay" label="用户总数" :value="statsData.userCount" :icon="User" tone="accent" />
      <StatCard class="card-enter stagger-delay" label="通道总数" :value="statsData.channelCount" :icon="Connection" tone="violet" />
      <StatCard class="card-enter stagger-delay" label="在线通道" :value="statsData.connectedCount" :icon="CircleCheck" tone="success" />
      <StatCard class="card-enter stagger-delay" label="系统状态" :value="statsData.systemStatus" :icon="Monitor" tone="warning" />
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 通道状态环形图 -->
      <div class="chart-card panel card-enter">
        <div class="chart-header">
          <h3>通道状态分布</h3>
          <el-tag size="small" type="success" effect="plain">实时</el-tag>
        </div>
        <div class="chart-body">
          <v-chart class="chart" :option="channelChartOption" autoresize />
        </div>
      </div>

      <!-- 短信趋势折线图 -->
      <div class="chart-card panel card-enter">
        <div class="chart-header">
          <h3>短信发送趋势</h3>
          <el-radio-group v-model="trendRange" size="small">
            <el-radio-button label="7d">近7天</el-radio-button>
            <el-radio-button label="30d">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-body">
          <v-chart class="chart" :option="trendChartOption" autoresize />
        </div>
      </div>
    </div>

    <!-- 系统状态时间线 -->
    <div class="activity-card panel card-enter">
      <div class="card-header">
        <h3>系统状态</h3>
      </div>
      <div class="activity-content">
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in activities"
            :key="index"
            :type="item.type"
            :timestamp="item.time"
            placement="top"
          >
            <div class="activity-item">
              <el-icon :size="16"><component :is="item.icon" /></el-icon>
              <span>{{ item.content }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { User, Monitor, Connection, CircleCheck } from '@element-plus/icons-vue'
import { getUserCount } from '../api/user'
import { getChannelList, getChannelPoolStatus } from '../api/channel'
import { chartColors, darkTooltip, areaGradient } from '../utils/echarts-theme'
import StatCard from '../components/common/StatCard.vue'

use([CanvasRenderer, PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// 统计数据
const statsData = ref({
  userCount: 0,
  channelCount: 0,
  connectedCount: 0,
  systemStatus: '运行中'
})

// 通道状态环形图数据
const channelStatusData = ref({ connected: 0, disconnected: 0, disabled: 0 })

const channelChartOption = computed(() => {
  const total = channelStatusData.value.connected + channelStatusData.value.disconnected + channelStatusData.value.disabled
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      ...darkTooltip
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: chartColors.text }
    },
    title: {
      text: `${total}`,
      subtext: '通道总数',
      left: '36%',
      top: 'center',
      textAlign: 'center',
      textStyle: { color: chartColors.textStrong, fontSize: 26, fontWeight: 700, fontFamily: 'JetBrains Mono, Consolas, monospace' },
      subtextStyle: { color: chartColors.text, fontSize: 12 }
    },
    series: [
      {
        name: '通道状态',
        type: 'pie',
        radius: ['55%', '75%'],
        center: ['36%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#0d1424',
          borderWidth: 3
        },
        label: { show: false },
        emphasis: { scaleSize: 4 },
        data: [
          { value: channelStatusData.value.connected, name: '已连接', itemStyle: { color: chartColors.success } },
          { value: channelStatusData.value.disconnected, name: '未连接', itemStyle: { color: chartColors.danger } },
          { value: channelStatusData.value.disabled, name: '已禁用', itemStyle: { color: chartColors.muted } }
        ]
      }
    ]
  }
})

// 短信趋势数据
const trendRange = ref('7d')
const trendData = ref({
  '7d': {
    dates: ['07-07', '07-08', '07-09', '07-10', '07-11', '07-12', '07-13'],
    sent: [1250, 1380, 1520, 1420, 1680, 1750, 1890],
    success: [1200, 1320, 1480, 1380, 1620, 1700, 1840]
  },
  '30d': {
    dates: Array.from({ length: 30 }, (_, i) => {
      const d = new Date()
      d.setDate(d.getDate() - 29 + i)
      return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }),
    sent: Array.from({ length: 30 }, () => Math.floor(Math.random() * 800 + 1200)),
    success: Array.from({ length: 30 }, () => Math.floor(Math.random() * 750 + 1100))
  }
})

const trendChartOption = computed(() => {
  const data = trendData.value[trendRange.value as keyof typeof trendData.value]
  return {
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
      data: data.dates,
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
        data: data.sent
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
        data: data.success
      }
    ]
  }
})

// 活动列表
const activities = ref([
  { time: '刚刚', content: '系统运行正常，所有服务可用', type: 'success' as const, icon: CircleCheck },
  { time: '5分钟前', content: '通道状态检测完成', type: 'primary' as const, icon: Monitor },
  { time: '1小时前', content: '用户登录系统', type: 'primary' as const, icon: User },
  { time: '今天 09:00', content: '系统启动完成', type: 'success' as const, icon: CircleCheck }
])

// 加载数据
const loadData = async () => {
  try {
    // 用户数
    const userRes = await getUserCount()
    statsData.value.userCount = userRes.data || 0

    // 通道数据
    const channelRes = await getChannelList(1, 100)
    const channels = channelRes.data.list
    statsData.value.channelCount = channelRes.data.total

    // 通道状态
    const enabledChannels = channels.filter(c => c.status === 1)
    const disabledChannels = channels.filter(c => c.status === 0)

    if (enabledChannels.length > 0) {
      try {
        const statusRes = await getChannelPoolStatus()
        const connectedCount = enabledChannels.filter(c => {
          const pool = statusRes.data[c.code]
          return pool && pool.connected > 0
        }).length
        statsData.value.connectedCount = connectedCount
        channelStatusData.value = {
          connected: connectedCount,
          disconnected: enabledChannels.length - connectedCount,
          disabled: disabledChannels.length
        }
      } catch {
        channelStatusData.value = {
          connected: 0,
          disconnected: enabledChannels.length,
          disabled: disabledChannels.length
        }
      }
    } else {
      channelStatusData.value = {
        connected: 0,
        disconnected: 0,
        disabled: disabledChannels.length
      }
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  padding: 22px 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
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

/* 系统状态卡片 */
.activity-card {
  padding: 22px 24px;
}

.activity-card .card-header {
  margin-bottom: 18px;
}

.activity-card .card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.activity-content {
  padding-left: 4px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-secondary);
}

.activity-item .el-icon {
  color: var(--accent);
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
