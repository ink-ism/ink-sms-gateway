import request from '../utils/axios'
import type { ApiResponse, Channel, ChannelListData } from '../types'

/** 获取通道列表 */
export function getChannelList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<ChannelListData>>('/admin/channel/list', {
    params: { page, size, keyword }
  })
}

/** 获取通道详情 */
export function getChannelDetail(id: number) {
  return request.get<any, ApiResponse<Channel>>(`/admin/channel/${id}`)
}

/** 创建通道 */
export function createChannel(data: Partial<Channel>) {
  return request.post<any, ApiResponse<string>>('/admin/channel', data)
}

/** 更新通道 */
export function updateChannel(id: number, data: Partial<Channel>) {
  return request.put<any, ApiResponse<string>>(`/admin/channel/${id}`, data)
}

/** 删除通道 */
export function deleteChannel(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/channel/${id}`)
}

/** 启用通道 */
export function enableChannel(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/channel/${id}/enable`)
}

/** 禁用通道 */
export function disableChannel(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/channel/${id}/disable`)
}

/** 检测单个通道连接状态 */
export function getConnectionStatus(id: number) {
  return request.get<any, ApiResponse<{ id: number; connected: boolean; reason: string; latency: number }>>(`/admin/channel/${id}/connection-status`)
}

/** 批量检测通道连接状态 */
export function getConnectionStatusBatch(ids: number[]) {
  return request.post<any, ApiResponse<Record<string, { id: number; connected: boolean; reason: string; latency: number }>>>('/admin/channel/connection-status-batch', ids)
}

/** 刷新通道配置（重新加载CMPP连接） */
export function refreshChannels() {
  return request.post<any, ApiResponse<string>>('/sms/channels/refresh')
}
