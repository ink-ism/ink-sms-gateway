<template>
  <div class="page-header panel">
    <div class="header-left">
      <h2 class="header-title">{{ title }}</h2>
      <span v-if="badge" class="header-badge mono">{{ badge }}</span>
      <slot name="title-extra" />
    </div>
    <div v-if="$slots.actions" class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 页面标题 */
    title: string
    /** 标题旁计数徽标，如「共 12 条」 */
    badge?: string
  }>(),
  { badge: '' }
)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  position: relative;
  padding-left: 12px;
}

/* 标题前霓虹竖条 */
.header-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: var(--accent);
  box-shadow: var(--glow-accent);
}

.header-badge {
  font-size: 12px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid rgba(34, 211, 238, 0.2);
  padding: 3px 10px;
  border-radius: 20px;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
