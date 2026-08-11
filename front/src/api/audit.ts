import request from '../utils/axios'
import type { ApiResponse, AuditLog, PageData } from '../types'

/** 分页查询审计日志 */
export function getAuditLogs(params: {
  page: number
  size: number
  module?: string
  action?: string
  keyword?: string
}) {
  return request.get<any, ApiResponse<PageData<AuditLog>>>('/admin/audit-logs/list', { params })
}
