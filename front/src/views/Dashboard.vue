<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="(stat, index) in stats" :key="index" :style="{ background: stat.gradient }">
        <div class="stat-icon">
          <el-icon :size="28"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">
            <span class="animated-number">{{ stat.animatedValue }}</span>
            <span v-if="stat.suffix" class="stat-suffix">{{ stat.suffix }}</span>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 通道状态饼图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>通道状态分布</h3>
          <el-tag size="small" type="info">实时</el-tag>
        </div>
        <div class="chart-body">
          <v-chart class="chart" :option="channelChartOption" autoresize />
        </div>
      </div>

      <!-- 短信趋势折线图 -->
      <div class="chart-card">
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

    <!-- 最近活动 -->
    <div class="activity-card">
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
import { ref, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { User, Monitor, Connection, CircleCheck } from '@element-plus/icons-vue'
import { getUserCount } from '../api/user'
import { getChannelList, getChannelPoolStatus } from '../api/channel'

use([CanvasRenderer, PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// 统计数据
const statsData = ref({
  userCount: 0,
  channelCount: 0,
  connectedCount: 0,
  systemStatus: '运行中'
})

const animatedNumbers = ref([0, 0, 0])

// 动画数字
const animateNumber = (target: number, index: number, duration = 1500) => {
  const start = animatedNumbers.value[index]
  const increment = (target - start) / (duration / 16)
  let current = start
  
  const timer = setInterval(() => {
    current += increment
    if ((increment > 0 && current >= target) || (increment < 0 && current <= target)) {
      animatedNumbers.value[index] = target
      clearInterval(timer)
    } else {
      animatedNumbers.value[index] = Math.round(current)
    }
  }, 16)
}

const stats = computed(() => [
  {
    icon: User,
    label: '用户总数',
    value: statsData.value.userCount,
    animatedValue: animatedNumbers.value[0],
    gradient: 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)'
  },
  {
    icon: Connection,
    label: '通道总数',
    value: statsData.value.channelCount,
    animatedValue: animatedNumbers.value[1],
    gradient: 'linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%)'
  },
  {
    icon: CircleCheck,
    label: '在线通道',
    value: statsData.value.connectedCount,
    animatedValue: animatedNumbers.value[2],
    gradient: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)'
  },
  {
    icon: Monitor,
    label: '系统状态',
    value: statsData.value.systemStatus,
    animatedValue: statsData.value.systemStatus,
    suffix: '',
    gradient: 'linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%)'
  }
])

// 通道状态饼图数据
const channelStatusData = ref({ connected: 0, disconnected: 0, disabled: 0 })

const channelChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    textStyle: { color: '#6b7280' }
  },
  series: [
    {
      name: '通道状态',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: [
        { 
          value: channelStatusData.value.connected, 
          name: '已连接',
          itemStyle: { color: '#10b981' }
        },
        { 
          value: channelStatusData.value.disconnected, 
          name: '未连接',
          itemStyle: { color: '#ef4444' }
        },
        { 
          value: channelStatusData.value.disabled, 
          name: '已禁用',
          itemStyle: { color: '#9ca3af' }
        }
      ]
    }
  ]
}))

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
      axisPointer: {
        type: 'cross',
        label: { backgroundColor: '#6a7985' }
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#6b7280' }
    },
    series: [
      {
        name: '发送总量',
        type: 'line',
        smooth: true,
        lineStyle: { width: 3, color: '#3b82f6' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
            ]
          }
        },
        itemStyle: { color: '#3b82f6' },
        data: data.sent
      },
      {
        name: '成功数量',
        type: 'line',
        smooth: true,
        lineStyle: { width: 3, color: '#10b981' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
            ]
          }
        },
        itemStyle: { color: '#10b981' },
        data: data.success
      }
    ]
  }
})

// 活动列表
const activities = ref([
  { time: '刚刚', content: '系统运行正常，所有服务可用', type: 'success', icon: CircleCheck },
  { time: '5分钟前', content: '通道状态检测完成', type: 'primary', icon: Monitor },
  { time: '1小时前', content: '用户登录系统', type: 'primary', icon: User },
  { time: '今天 09:00', content: '系统启动完成', type: 'success', icon: CircleCheck }
])

// 加载数据
const loadData = async () => {
  try {
    // 用户数
    const userRes = await getUserCount()
    statsData.value.userCount = userRes.data || 0
    animateNumber(statsData.value.userCount, 0)
    
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
    
    animateNumber(statsData.value.channelCount, 1)
    animateNumber(statsData.value.connectedCount, 2)
    
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

watch(trendRange, () => {
  // 切换范围时可以重新加载数据
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  border-radius: var(--border-radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  color: white;
  transition: all var(--transition-normal) ease;
  animation: cardSlideUp 0.5s ease-out both;
}

.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.1s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.2s; }

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.stat-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-suffix {
  font-size: 14px;
  font-weight: 400;
  margin-left: 4px;
  opacity: 0.8;
}

.stat-label {
  font-size: 13px;
  opacity: 0.8;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal) ease;
  animation: cardSlideUp 0.5s ease-out both;
  animation-delay: 0.25s;
}

.chart-card:hover {
  box-shadow: var(--shadow-lg);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 16px;
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

/* 活动卡片 */
.activity-card {
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  animation: cardSlideUp 0.5s ease-out both;
  animation-delay: 0.3s;
}

.activity-card .card-header {
  margin-bottom: 20px;
}

.activity-card .card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.activity-content {
  padding-left: 8px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.activity-item .el-icon {
  color: var(--primary-color);
}

/* 动画 */
@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
