<template>
  <div class="stat-card" :class="`tone-${tone}`">
    <div class="stat-icon">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value mono">
        {{ displayValue }}<span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
      </div>
      <div class="stat-label">{{ label }}</div>
    </div>
    <div class="stat-glow"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { Component } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | string
    icon: Component
    tone?: 'accent' | 'success' | 'violet' | 'warning'
    suffix?: string
  }>(),
  { tone: 'accent', suffix: '' }
)

/** 数字滚动动画（仅数值型 value 生效） */
const animated = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const animateTo = (target: number, duration = 1200) => {
  if (timer) clearInterval(timer)
  const start = animated.value
  const step = (target - start) / (duration / 16)
  timer = setInterval(() => {
    const next = animated.value + step
    if ((step > 0 && next >= target) || (step < 0 && next <= target) || step === 0) {
      animated.value = target
      if (timer) clearInterval(timer)
    } else {
      animated.value = Math.round(next)
    }
  }, 16)
}

const displayValue = computed(() =>
  typeof props.value === 'number' ? animated.value : props.value
)

watch(
  () => props.value,
  (val) => {
    if (typeof val === 'number') animateTo(val)
  }
)

onMounted(() => {
  if (typeof props.value === 'number') animateTo(props.value)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  background: var(--bg-panel);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-normal) ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lift);
}

/* 左侧霓虹色条 */
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  bottom: 16px;
  width: 3px;
  border-radius: 0 3px 3px 0;
}

.tone-accent::before { background: var(--accent); box-shadow: var(--glow-accent); }
.tone-success::before { background: var(--success); box-shadow: var(--glow-success); }
.tone-violet::before { background: var(--violet); box-shadow: 0 0 12px rgba(167, 139, 250, 0.4); }
.tone-warning::before { background: var(--warning); box-shadow: 0 0 12px rgba(251, 191, 36, 0.4); }

/* 图标容器 */
.stat-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.tone-accent .stat-icon { color: var(--accent); background: var(--accent-soft); }
.tone-success .stat-icon { color: var(--success); background: var(--success-soft); }
.tone-violet .stat-icon { color: var(--violet); background: var(--violet-soft); }
.tone-warning .stat-icon { color: var(--warning); background: var(--warning-soft); }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}

.stat-suffix {
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
  color: var(--text-muted);
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* 右下角氛围光斑 */
.stat-glow {
  position: absolute;
  right: -30px;
  bottom: -30px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0.5;
}

.tone-accent .stat-glow { background: radial-gradient(circle, rgba(34, 211, 238, 0.14) 0%, transparent 70%); }
.tone-success .stat-glow { background: radial-gradient(circle, rgba(0, 255, 163, 0.12) 0%, transparent 70%); }
.tone-violet .stat-glow { background: radial-gradient(circle, rgba(167, 139, 250, 0.14) 0%, transparent 70%); }
.tone-warning .stat-glow { background: radial-gradient(circle, rgba(251, 191, 36, 0.12) 0%, transparent 70%); }
</style>
