<template>
  <AppLayout>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统概览</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-statistic title="用户总数" :value="stats.userCount">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="今日登录" :value="stats.todayLogins">
            <template #prefix>
              <el-icon><Monitor /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="系统状态" value="运行中">
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
          </el-statistic>
        </el-col>
      </el-row>
    </el-card>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { User, Monitor, CircleCheck } from '@element-plus/icons-vue'
import { getUserCount } from '../api/user'
import AppLayout from '../components/AppLayout.vue'

const stats = ref({ userCount: 0, todayLogins: 0 })

const loadStats = async () => {
  try {
    const res = await getUserCount()
    stats.value.userCount = res.data || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

loadStats()
</script>

<style scoped>
.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
