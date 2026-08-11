import request from '../utils/axios'
import type { ApiResponse, Signature, Template, PageData } from '../types'

// ==================== 签名 ====================

/** 获取签名列表 */
export function getSignatureList(page: number, size: number, keyword?: string, status?: number) {
  return request.get<any, ApiResponse<PageData<Signature>>>('/admin/signatures/list', {
    params: { page, size, keyword, status }
  })
}

/** 创建签名 */
export function createSignature(data: Partial<Signature>) {
  return request.post<any, ApiResponse<string>>('/admin/signatures', data)
}

/** 更新签名 */
export function updateSignature(id: number, data: Partial<Signature>) {
  return request.put<any, ApiResponse<string>>(`/admin/signatures/${id}`, data)
}

/** 审核通过 */
export function approveSignature(id: number, remark?: string) {
  return request.put<any, ApiResponse<string>>(`/admin/signatures/${id}/approve`, { remark })
}

/** 审核驳回 */
export function rejectSignature(id: number, remark?: string) {
  return request.put<any, ApiResponse<string>>(`/admin/signatures/${id}/reject`, { remark })
}

/** 删除签名 */
export function deleteSignature(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/signatures/${id}`)
}

// ==================== 模板 ====================

/** 获取模板列表 */
export function getTemplateList(page: number, size: number, keyword?: string, status?: number) {
  return request.get<any, ApiResponse<PageData<Template>>>('/admin/templates/list', {
    params: { page, size, keyword, status }
  })
}

/** 创建模板 */
export function createTemplate(data: Partial<Template>) {
  return request.post<any, ApiResponse<string>>('/admin/templates', data)
}

/** 更新模板 */
export function updateTemplate(id: number, data: Partial<Template>) {
  return request.put<any, ApiResponse<string>>(`/admin/templates/${id}`, data)
}

/** 审核通过 */
export function approveTemplate(id: number, remark?: string) {
  return request.put<any, ApiResponse<string>>(`/admin/templates/${id}/approve`, { remark })
}

/** 审核驳回 */
export function rejectTemplate(id: number, remark?: string) {
  return request.put<any, ApiResponse<string>>(`/admin/templates/${id}/reject`, { remark })
}

/** 删除模板 */
export function deleteTemplate(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/templates/${id}`)
}
