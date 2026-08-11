import request from '../utils/axios'
import type { ApiResponse, SmsDown, SmsUp, SmsUpDetail, SmsListData } from '../types'

/** 获取下行短信列表 */
export function getSmsDownList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<SmsListData<SmsDown>>>('/admin/sms/down/list', {
    params: { page, size, keyword }
  })
}

/** 获取上行短信列表 */
export function getSmsUpList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<SmsListData<SmsUp>>>('/admin/sms/up/list', {
    params: { page, size, keyword }
  })
}

/** 获取上行短信详情 */
export function getSmsUpDetail(id: number) {
  return request.get<any, ApiResponse<SmsUpDetail>>(`/admin/sms/up/${id}/detail`)
}

/** 删除上行短信记录 */
export function deleteSmsUp(id: number) {
  return request.delete<any, ApiResponse<string>>(`/admin/sms/up/${id}`)
}

/** 将上行手机号加入黑名单 */
export function blacklistSmsUp(id: number) {
  return request.post<any, ApiResponse<string>>(`/admin/sms/up/${id}/blacklist`)
}
