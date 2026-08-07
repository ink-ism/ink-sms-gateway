import request from '../utils/axios'
import type { ApiResponse, BlacklistListData } from '../types'

/** 分页查询黑名单 */
export function getBlacklistList(page: number, size: number, channelCode?: string, phone?: string) {
  return request.get<any, ApiResponse<BlacklistListData>>('/admin/blacklist/list', {
    params: { page, size, channelCode, phone }
  })
}

/** 移除黑名单 */
export function removeBlacklist(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/blacklist/${id}`)
}
