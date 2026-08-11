import request from '../utils/axios'
import type { ApiResponse, StatsOverview, TrendPoint, ChannelStat, SpStat } from '../types'

/** 看板概览 */
export function getStatsOverview() {
  return request.get<any, ApiResponse<StatsOverview>>('/admin/stats/overview')
}

/** 按天趋势 */
export function getStatsTrend(days: number) {
  return request.get<any, ApiResponse<TrendPoint[]>>('/admin/stats/trend', { params: { days } })
}

/** 按通道统计 */
export function getChannelStats(days: number) {
  return request.get<any, ApiResponse<ChannelStat[]>>('/admin/stats/channel', { params: { days } })
}

/** 按客户统计 */
export function getSpStats(days: number) {
  return request.get<any, ApiResponse<SpStat[]>>('/admin/stats/sp', { params: { days } })
}
