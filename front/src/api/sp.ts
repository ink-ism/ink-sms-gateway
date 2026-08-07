import request from '../utils/axios'
import type { ApiResponse, Sp, SpListData } from '../types'

/** 获取客户列表 */
export function getSpList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<SpListData>>('/admin/sp/list', {
    params: { page, size, keyword }
  })
}

/** 获取客户详情 */
export function getSpDetail(id: number) {
  return request.get<any, ApiResponse<Sp>>(`/admin/sp/${id}`)
}

/** 创建客户 */
export function createSp(data: Partial<Sp>) {
  return request.post<any, ApiResponse<string>>('/admin/sp', data)
}

/** 更新客户 */
export function updateSp(id: number, data: Partial<Sp>) {
  return request.put<any, ApiResponse<string>>(`/admin/sp/${id}`, data)
}

/** 删除客户 */
export function deleteSp(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/sp/${id}`)
}

/** 启用客户 */
export function enableSp(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/sp/${id}/enable`)
}

/** 禁用客户 */
export function disableSp(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/sp/${id}/disable`)
}

/** 获取客户绑定通道 */
export function getSpChannels(id: number) {
  return request.get<any, ApiResponse<string[]>>(`/admin/sp/${id}/channels`)
}

/** 全量替换客户绑定通道 */
export function bindSpChannels(id: number, channelCodes: string[]) {
  return request.put<any, ApiResponse<string>>(`/admin/sp/${id}/channels`, { channelCodes })
}
