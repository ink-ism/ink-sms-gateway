import request from '../utils/axios'
import type { ApiResponse, SensitiveWord, PageData } from '../types'

/** 获取敏感词列表 */
export function getSensitiveList(page: number, size: number, keyword?: string, status?: number) {
  return request.get<any, ApiResponse<PageData<SensitiveWord>>>('/admin/sensitives/list', {
    params: { page, size, keyword, status }
  })
}

/** 创建敏感词 */
export function createSensitive(data: Partial<SensitiveWord>) {
  return request.post<any, ApiResponse<string>>('/admin/sensitives', data)
}

/** 更新敏感词 */
export function updateSensitive(id: number, data: Partial<SensitiveWord>) {
  return request.put<any, ApiResponse<string>>(`/admin/sensitives/${id}`, data)
}

/** 启用敏感词 */
export function enableSensitive(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/sensitives/${id}/enable`)
}

/** 禁用敏感词 */
export function disableSensitive(id: number) {
  return request.put<any, ApiResponse<string>>(`/admin/sensitives/${id}/disable`)
}

/** 删除敏感词 */
export function deleteSensitive(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/sensitives/${id}`)
}
