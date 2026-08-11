/** 后端 LocalDateTime 序列化为 ISO 格式（含 T），展示时替换为空格 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
