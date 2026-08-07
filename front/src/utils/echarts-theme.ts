/**
 * ECharts 暗色运维风主题基础配置
 * 统一文字色/轴线色/tooltip 样式与霓虹系列色板，避免页面内硬编码颜色
 */

/** 主题色板（与 Design Token 对齐） */
export const chartColors = {
  text: '#8b9bb4',
  textStrong: '#e6edf7',
  axisLine: '#1b2740',
  splitLine: '#131c30',
  accent: '#22d3ee',
  success: '#00ffa3',
  warning: '#fbbf24',
  danger: '#ff4d6d',
  violet: '#a78bfa',
  muted: '#55647e'
}

/** 系列霓虹色板 */
export const seriesPalette = [
  chartColors.accent,
  chartColors.success,
  chartColors.violet,
  chartColors.warning,
  chartColors.danger
]

/** 深色 tooltip 样式 */
export const darkTooltip = {
  backgroundColor: 'rgba(13, 20, 36, 0.95)',
  borderColor: '#1b2740',
  borderWidth: 1,
  textStyle: { color: '#e6edf7', fontSize: 12 },
  extraCssText: 'box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5); border-radius: 8px;'
}

/** hex 转 rgba */
const hexToRgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** 面积图垂直渐变（从上到下由亮到透明） */
export const areaGradient = (hex: string, topAlpha = 0.32) => ({
  type: 'linear' as const,
  x: 0,
  y: 0,
  x2: 0,
  y2: 1,
  colorStops: [
    { offset: 0, color: hexToRgba(hex, topAlpha) },
    { offset: 1, color: hexToRgba(hex, 0.02) }
  ]
})
