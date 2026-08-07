<template>
  <span class="status-dot-wrap">
    <span class="pulse-dot" :class="`dot-${status}`"></span>
    <span v-if="label" class="status-dot-label" :class="`label-${status}`">{{ label }}</span>
    <slot />
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 状态：online 在线 / offline 离线 / loading 检测中 / disabled 禁用 */
    status: 'online' | 'offline' | 'loading' | 'disabled'
    /** 可选文字标签，不传则仅显示圆点 */
    label?: string
  }>(),
  { label: '' }
)
</script>

<style scoped>
.status-dot-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.status-dot-label {
  font-family: var(--font-mono);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.label-online { color: var(--success); }
.label-offline { color: var(--danger); }
.label-loading { color: var(--accent); }
.label-disabled { color: var(--text-muted); }
</style>
